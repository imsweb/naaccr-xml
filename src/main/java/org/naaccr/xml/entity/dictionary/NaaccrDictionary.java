/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NaaccrDictionary {

    private List<NaaccrDictionaryItem> items;
    
    // caches to improve lookup performances
    private Map<Integer, NaaccrDictionaryItem> _cachedByNumber;
    private Map<String, NaaccrDictionaryItem> _cachedByElementName;

    public List<NaaccrDictionaryItem> getItems() {
        if (items == null)
            items = new ArrayList<>();
        return items;
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

    public NaaccrDictionaryItem getItemByElementName(String name) {
        if (_cachedByElementName == null) {
            Map<String, NaaccrDictionaryItem> cache = new HashMap<>();
            for (NaaccrDictionaryItem item : items)
                if (item.getElementName() != null)
                    cache.put(item.getElementName(), item);
            _cachedByElementName = cache;
        }
        return _cachedByElementName.get(name);
    }
    
    public NaaccrDictionaryItem getItem(Object key) {
        if (key instanceof Integer)
            return getItemByNumber((Integer)key);
        if (key instanceof String)
            return getItemByElementName((String)key);
        return null;
    }
}
