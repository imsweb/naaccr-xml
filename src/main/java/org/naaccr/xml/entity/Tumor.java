/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tumor {
    
    private List<Item> items;

    // caches to improve lookup performances
    private Map<String, Item> _cachedById;
    private Map<Integer, Item> _cachedByNumber;

    public List<Item> getItems() {
        if (items == null)
            items = new ArrayList<>();
        return items;
    }

    public Item getItemById(String id) {
        if (_cachedById == null) {
            Map<String, Item> cache = new HashMap<>();
            for (Item item : items)
                if (item.getId() != null)
                    cache.put(item.getId(), item);
            _cachedById = cache;
        }
        return _cachedById.get(id);
    }
    
    public Item getItemByNumber(Integer number) {
        if (_cachedByNumber == null) {
            Map<Integer, Item> cache = new HashMap<>();
            for (Item item : items)
                if (item.getNum() != null)
                    cache.put(item.getNum(), item);
            _cachedByNumber = cache;
        }
        return _cachedByNumber.get(number);
    }

    public Item getItem(Object key) {
        if (key instanceof String)
            return getItemById((String)key);
        if (key instanceof Integer)
            return getItemByNumber((Integer)key);
        return null;
    }
}
