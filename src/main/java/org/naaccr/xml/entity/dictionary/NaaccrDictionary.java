/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NaaccrDictionary {

    private String _dictionaryUri;

    private String _naaccrVersion;

    private String _description;

    private List<NaaccrDictionaryItem> _items;

    // caches to improve lookup performances
    private Map<String, NaaccrDictionaryItem> _cachedById;
    private Map<Integer, NaaccrDictionaryItem> _cachedByNumber;

    public String getDictionaryUri() {
        return _dictionaryUri;
    }

    public void setDictionaryUri(String dictionaryUri) {
        _dictionaryUri = dictionaryUri;
    }

    public String getNaaccrVersion() {
        return _naaccrVersion;
    }

    public void setNaaccrVersion(String naaccrVersion) {
        _naaccrVersion = naaccrVersion;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public List<NaaccrDictionaryItem> getItems() {
        if (_items == null)
            _items = new ArrayList<>();
        return _items;
    }

    public NaaccrDictionaryItem getItemByNaaccrId(String id) {
        if (_cachedById == null) {
            Map<String, NaaccrDictionaryItem> cache = new HashMap<>();
            for (NaaccrDictionaryItem item : _items)
                if (item.getNaaccrId() != null)
                    cache.put(item.getNaaccrId(), item);
            _cachedById = cache;
        }
        return _cachedById.get(id);
    }

    public NaaccrDictionaryItem getItemByNaaccrNum(Integer number) {
        if (_cachedByNumber == null) {
            Map<Integer, NaaccrDictionaryItem> cache = new HashMap<>();
            for (NaaccrDictionaryItem item : _items)
                if (item.getNaaccrNum() != null)
                    cache.put(item.getNaaccrNum(), item);
            _cachedByNumber = cache;
        }
        return _cachedByNumber.get(number);
    }
}
