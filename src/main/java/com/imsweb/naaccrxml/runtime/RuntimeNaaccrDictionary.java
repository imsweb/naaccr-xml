/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

        // as of spec 1.1 the NAACCR version is optional on user-dictionaries, but that means we can't really validate them without 
        // knowing the corresponding base dictionary, so we have to re-validate here...
        if (userDictionary != null && userDictionary.getNaaccrVersion() == null) {
            String error = NaaccrXmlDictionaryUtils.validateUserDictionary(userDictionary, baseDictionary.getNaaccrVersion());
            if (error != null)
                throw new NaaccrIOException("Invalid user-defined dictionary: " + error);
        }

        // use the default user dictionary if one is not provided...
        if (userDictionary == null)
            userDictionary = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(baseDictionary.getNaaccrVersion());

        _format = NaaccrFormat.getInstance(baseDictionary.getNaaccrVersion(), recordType);
        _items = new ArrayList<>();
        for (NaaccrDictionaryItem item : baseDictionary.getItems())
            if (item.getRecordTypes() == null || Arrays.asList(item.getRecordTypes().split(",")).contains(recordType))
                _items.add(new RuntimeNaaccrDictionaryItem(item));
        for (NaaccrDictionaryItem item : userDictionary.getItems())
            if (item.getRecordTypes() == null || Arrays.asList(item.getRecordTypes().split(",")).contains(recordType))
                _items.add(new RuntimeNaaccrDictionaryItem(item));

        // sort the fields by starting columns (no start columns go to the end)
        Collections.sort(_items, new Comparator<RuntimeNaaccrDictionaryItem>() {
            @Override
            public int compare(RuntimeNaaccrDictionaryItem o1, RuntimeNaaccrDictionaryItem o2) {
                if (o1.getStartColumn() == null)
                    return 1;
                if (o2.getStartColumn() == null)
                    return -1;
                return o1.getStartColumn().compareTo(o2.getStartColumn());
            }
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
