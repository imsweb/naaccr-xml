/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrIOException;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionary {

    // used to uniquely identify a runtime dictionary (based on the URI of the base and user dictionaries)
    private final String _id;

    // the format for this runtime dictionary
    private final NaaccrFormat _format;

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

        // compute a clean list of user dictionaries
        List<NaaccrDictionary> dictionaries = new ArrayList<>();
        if (userDictionaries != null)
            for (NaaccrDictionary userDictionary : userDictionaries)
                if (userDictionary != null)
                    dictionaries.add(userDictionary);

        // assign the ID based on the URI (done on the raw input so the behavior is the same when called outside of this constructor)
        _id = computeId(recordType, baseDictionary, dictionaries);

        List<String> errors = NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, dictionaries);
        if (!errors.isEmpty())
            throw new NaaccrIOException(StringUtils.capitalize(errors.get(0)));

        // use the default user dictionary if one is not provided...
        if (dictionaries.isEmpty()) {
            NaaccrDictionary defaultUserDictionary = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(baseDictionary.getNaaccrVersion());
            if (defaultUserDictionary != null)
                dictionaries.add(defaultUserDictionary);
        }

        _format = NaaccrFormat.getInstance(baseDictionary.getNaaccrVersion(), recordType);
        _items = new ArrayList<>();
        for (NaaccrDictionaryItem item : baseDictionary.getItems())
            if (item.getRecordTypes() == null || StringUtils.contains(item.getRecordTypes(), recordType))
                _items.add(new RuntimeNaaccrDictionaryItem(item));
        Set<String> processedIds = new HashSet<>();
        for (NaaccrDictionary userDictionary : dictionaries) {
            for (NaaccrDictionaryItem item : userDictionary.getItems()) {
                if ((item.getRecordTypes() == null || StringUtils.contains(item.getRecordTypes(), recordType) && !processedIds.contains(item.getNaaccrId()))) {
                    _items.add(new RuntimeNaaccrDictionaryItem(item));
                    processedIds.add(item.getNaaccrId());
                }
            }
        }

        // sort the fields by starting columns (no start columns go to the end) for older version, by ID for new ones
        if (Integer.parseInt(_format.getNaaccrVersion()) <= 180) {
            _items.sort((o1, o2) -> {
                if (o1.getStartColumn() == null)
                    return 1;
                if (o2.getStartColumn() == null)
                    return -1;
                return o1.getStartColumn().compareTo(o2.getStartColumn());
            });
        }
        else
            _items.sort(Comparator.comparing(RuntimeNaaccrDictionaryItem::getNaaccrId));
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
