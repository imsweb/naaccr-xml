/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NaaccrDictionary {

    private String version;
    
    private List<NaaccrDictionaryItem> items;
    
    // caches to improve lookup performances
    private Map<String, NaaccrDictionaryItem> _cachedById;
    private Map<Integer, NaaccrDictionaryItem> _cachedByNumber;


    public String getVersion() {
        return version;
    }

    public void setVersion(String val) {
        version = val;
    }
    
    public List<NaaccrDictionaryItem> getItems() {
        if (items == null)
            items = new ArrayList<>();
        return items;
    }

    public NaaccrDictionaryItem getItemById(String id) {
        if (_cachedById == null) {
            Map<String, NaaccrDictionaryItem> cache = new HashMap<>();
            for (NaaccrDictionaryItem item : items)
                if (item.getId() != null)
                    cache.put(item.getId(), item);
            _cachedById = cache;
        }
        return _cachedById.get(id);
    }
    
    public NaaccrDictionaryItem getItemByNumber(Integer number) {
        if (_cachedByNumber == null) {
            Map<Integer, NaaccrDictionaryItem> cache = new HashMap<>();
            for (NaaccrDictionaryItem item : items)
                if (item.getNumber() != null)
                    cache.put(item.getNumber(), item);
            _cachedByNumber = cache;
        }
        return _cachedByNumber.get(number);
    }
    
    public NaaccrDictionaryItem getItem(Object key) {
        if (key instanceof String)
            return getItemById((String)key);
        if (key instanceof Integer)
            return getItemByNumber((Integer)key);
        return null;
    }
}
