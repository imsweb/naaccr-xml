/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.naaccr.xml.NaaccrValidationError;
import org.naaccr.xml.entity.Item;

/**
 * Encapsulates the logic about the complex entities (except the root one, NaaccrDataExchange):
 * <ol>
 *     <li>Entity has a collection of items on it</li>
 *     <li>Entity has a collection of validation errors on it</li>
 * </ol>
 * This class also defines some utility methods to read/write those collections...
 */
public class AbstractEntity {

    protected List<Item> _items;
    
    protected List<NaaccrValidationError> _errors;

    // caches to improve lookup performances
    protected Map<String, Item> _cachedById;
    protected Map<Integer, Item> _cachedByNumber;

    public List<Item> getItems() {
        if (_items == null)
            _items = new ArrayList<>();
        return _items;
    }
    
    public Item getItemById(String id) {
        if (_cachedById == null) {
            Map<String, Item> cache = new HashMap<>();
            for (Item item : getItems())
                if (item.getId() != null)
                    cache.put(item.getId(), item);
            _cachedById = cache;
        }
        return _cachedById.get(id);
    }

    public Item getItemByNumber(Integer number) {
        if (_cachedByNumber == null) {
            Map<Integer, Item> cache = new HashMap<>();
            for (Item item : getItems())
                if (item.getNum() != null)
                    cache.put(item.getNum(), item);
            _cachedByNumber = cache;
        }
        return _cachedByNumber.get(number);
    }

    public Item getItem(String id, Integer number) {
        Item item = getItemById(id);
        if (item == null)
            item = getItemByNumber(number);
        return item;
    }
    
    public String getItemValue(String id, Integer number) {
        Item item = getItem(id, number);
        if (item != null)
            return item.getValue();
        return null;
    }

    public List<NaaccrValidationError> getValidationErrors() {
        if (_errors == null)
            _errors = new ArrayList<>();
        return _errors;
    }
}
