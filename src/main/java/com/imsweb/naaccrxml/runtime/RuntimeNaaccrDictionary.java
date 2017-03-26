/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrIOException;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionary {

    private NaaccrFormat _format;

    private List<RuntimeNaaccrDictionaryItem> _items;

    // caches used to improve lookup performances
    private Map<String, RuntimeNaaccrDictionaryItem> _cachedById;

    public RuntimeNaaccrDictionary(String recordType, NaaccrDictionary baseDictionary, NaaccrDictionary userDictionary) throws NaaccrIOException {
        if (recordType == null)
            throw new NaaccrIOException("Record type is required to create a runtime dictionary");
        if (baseDictionary == null)
            throw new NaaccrIOException("Base dictionary is required to create a runtime dictionary");

        // as of spec 1.1 the NAACCR version is optional on user-dictionaries, but that means we can't really validate them without 
        // knowing the corresponding base dictionary, so we have to re-validate here...
        if (userDictionary != null) {
            if (userDictionary.getNaaccrVersion() == null) {
                String error = NaaccrXmlDictionaryUtils.validateUserDictionary(userDictionary, baseDictionary.getNaaccrVersion());
                if (error != null)
                    throw new NaaccrIOException("Invalid user-defined dictionary: " + error);
            }
            else if (!baseDictionary.getNaaccrVersion().equals(userDictionary.getNaaccrVersion()))
                throw new NaaccrIOException("User-defined dictionary doesn't define the same version as the base dictionary");
        }

        // use the default user dictionary if one is not provided...
        if (userDictionary == null)
            userDictionary = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(baseDictionary.getNaaccrVersion());

        // extra validation: no user-defined item should have the same NAACCR ID or NAACCR Number as a base one
        for (NaaccrDictionaryItem item : userDictionary.getItems()) {
            if (baseDictionary.getItemByNaaccrId(item.getNaaccrId()) != null)
                throw new NaaccrIOException("User-defined dictionary cannot use same NAACCR ID as a base item: " + item.getNaaccrId());
            if (baseDictionary.getItemByNaaccrNum(item.getNaaccrNum()) != null)
                throw new NaaccrIOException("User-defined dictionary cannot use same NAACCR Number as a base item: " + item.getNaaccrNum());
        }

        _format = NaaccrFormat.getInstance(baseDictionary.getNaaccrVersion(), recordType);
        _items = new ArrayList<>();
        for (NaaccrDictionaryItem item : baseDictionary.getItems())
            if (item.getRecordTypes() == null || Arrays.asList(item.getRecordTypes().split(",")).contains(recordType))
                _items.add(new RuntimeNaaccrDictionaryItem(item));
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
}
