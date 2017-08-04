/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrIOException;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionary {

    // used to uniquely identify a runtime dictionary (based on the URI of the base and user dictionaries)
    private String _id;

    // the format for this runtime dictionary
    private NaaccrFormat _format;

    // the items for this runtime dictionary
    private List<RuntimeNaaccrDictionaryItem> _items;

    // caches used to improve lookup performances
    private Map<String, RuntimeNaaccrDictionaryItem> _cachedById;

    /**
     * Constructor.
     * @param recordType record type (required)
     * @param baseDictionary base dictionary (required)
     * @param userDictionaries user dictionaries (optional)
     * @throws NaaccrIOException if the runtime dictionary cannot be successfully created
     */
    public RuntimeNaaccrDictionary(String recordType, NaaccrDictionary baseDictionary, Collection<NaaccrDictionary> userDictionaries) throws NaaccrIOException {
        if (recordType == null)
            throw new NaaccrIOException("Record type is required to create a runtime dictionary");
        if (baseDictionary == null)
            throw new NaaccrIOException("Base dictionary is required to create a runtime dictionary");

        // assign the ID based on the URI (done on the raw input so the behavior is the same when called outside of this constructor)
        _id = computeId(recordType, baseDictionary, userDictionaries);

        // compute a clean list of user dictionaries
        List<NaaccrDictionary> dictionaries = new ArrayList<>();
        if (userDictionaries != null)
            for (NaaccrDictionary userDictionary : userDictionaries)
                if (userDictionary != null)
                    dictionaries.add(userDictionary);

        // as of spec 1.1 the NAACCR version is optional on user-dictionaries, but that means we can't really validate them without
        // knowing the corresponding base dictionary, so we have to re-validate here...
        for (NaaccrDictionary userDictionary : dictionaries) {
            if (userDictionary.getNaaccrVersion() == null) {
                String error = NaaccrXmlDictionaryUtils.validateUserDictionary(userDictionary, baseDictionary.getNaaccrVersion());
                if (error != null)
                    throw new NaaccrIOException("Invalid user-defined dictionary '" + userDictionary.getDictionaryUri() + "': " + error);
            }
            else if (!baseDictionary.getNaaccrVersion().equals(userDictionary.getNaaccrVersion()))
                throw new NaaccrIOException("User-defined dictionary '" + userDictionary.getDictionaryUri() + "' doesn't define the same version as the base dictionary");
        }

        // use the default user dictionary if one is not provided...
        if (dictionaries.isEmpty())
            dictionaries.add(NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(baseDictionary.getNaaccrVersion()));

        // extra validation involving all the user-defined dictionaries
        Map<String, String> idsDejaVu = new HashMap<>();
        Map<Integer, String> numbersDejaVue = new HashMap<>();
        for (NaaccrDictionary userDictionary : dictionaries) {
            String dictId = userDictionary.getDictionaryUri();
            for (NaaccrDictionaryItem item : userDictionary.getItems()) {
                // NAACCR IDs defined in user dictionaries cannot be the same as the base NAACCR IDs
                if (baseDictionary.getItemByNaaccrId(item.getNaaccrId()) != null)
                    throw new NaaccrIOException("User-defined dictionary '" + dictId + "' cannot use same NAACCR ID as a base item: " + item.getNaaccrId());
                // NAACCR Numbers defined in user dictionaries cannot be the same as the base NAACCR Numbers
                if (baseDictionary.getItemByNaaccrNum(item.getNaaccrNum()) != null)
                    throw new NaaccrIOException("User-defined dictionary '" + dictId + "' cannot use same NAACCR Number as a base item: " + item.getNaaccrNum());
                // NAACCR IDs must be unique among all user dictionaries
                if (idsDejaVu.containsKey(item.getNaaccrId()))
                    throw new NaaccrIOException("User-defined dictionary '" + dictId + "' and '" + idsDejaVu.get(item.getNaaccrId()) + "' both  define NAACCR ID '" + item.getNaaccrId() + "'");
                else
                    idsDejaVu.put(item.getNaaccrId(), dictId);
                // NAACCR Numbers must be unique among all user dictionaries
                if (numbersDejaVue.containsKey(item.getNaaccrNum()))
                    throw new NaaccrIOException("User-defined dictionary '" + dictId + "' and '" + numbersDejaVue.get(item.getNaaccrNum()) + "' both  define NAACCR ID '" + item.getNaaccrNum() + "'");
                else
                    numbersDejaVue.put(item.getNaaccrNum(), dictId);
            }
        }

        _format = NaaccrFormat.getInstance(baseDictionary.getNaaccrVersion(), recordType);
        _items = new ArrayList<>();
        for (NaaccrDictionaryItem item : baseDictionary.getItems())
            if (item.getRecordTypes() == null || Arrays.asList(item.getRecordTypes().split(",")).contains(recordType))
                _items.add(new RuntimeNaaccrDictionaryItem(item));
        for (NaaccrDictionary userDictionary : dictionaries)
            for (NaaccrDictionaryItem item : userDictionary.getItems())
                if (item.getRecordTypes() == null || Arrays.asList(item.getRecordTypes().split(",")).contains(recordType))
                    _items.add(new RuntimeNaaccrDictionaryItem(item));

        // sort the fields by starting columns (no start columns go to the end)
        _items.sort((o1, o2) -> {
            if (o1.getStartColumn() == null)
                return 1;
            if (o2.getStartColumn() == null)
                return -1;
            return o1.getStartColumn().compareTo(o2.getStartColumn());
        });
    }

    public String getId() {
        return _id;
    }

    public String getNaaccrVersion() {
        return _format.getNaaccrVersion();
    }

    public String getRecordType() {
        return _format.getRecordType();
    }

    public Integer getLineLength() {
        return _format.getLineLength();
    }

    public List<RuntimeNaaccrDictionaryItem> getItems() {
        if (_items == null)
            _items = new ArrayList<>();
        return _items;
    }

    public RuntimeNaaccrDictionaryItem getItemByNaaccrId(String id) {
        if (_cachedById == null) {
            Map<String, RuntimeNaaccrDictionaryItem> cache = new HashMap<>();
            for (RuntimeNaaccrDictionaryItem item : _items)
                if (item.getNaaccrId() != null)
                    cache.put(item.getNaaccrId(), item);
            _cachedById = cache;
        }
        return _cachedById.get(id);
    }

    /**
     * Helper method to compute an ID for a runtime dictionary based on the URI of its base and user dictionaries.
     * @param baseDictionary base dictionary (required)
     * @param userDictionaries user-defined dictionaries (optional, can be null or empty)
     * @return computed runtime dictionary ID, never null
     */
    public static String computeId(String recordType, NaaccrDictionary baseDictionary, Collection<NaaccrDictionary> userDictionaries) {
        if (baseDictionary == null)
            return recordType;

        StringBuilder buf = new StringBuilder(recordType);
        buf.append(";").append(baseDictionary.getDictionaryUri());
        if (userDictionaries != null)
            for (NaaccrDictionary userDictionary : userDictionaries)
                if (userDictionary != null)
                    buf.append(";").append(userDictionary.getDictionaryUri());
        return buf.toString();
    }
}
