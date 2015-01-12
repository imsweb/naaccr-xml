/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity;

import java.util.Map;

public class NaaccrDictionary {

    // items, keyed by their NAACCR Item Number
    private Map<Integer, NaaccrDictionaryItem> _items;

    public Map<Integer, NaaccrDictionaryItem> getItems() {
        return _items;
    }

    public void setItems(Map<Integer, NaaccrDictionaryItem> items) {
        _items = items;
    }
    
    public NaaccrDictionaryItem getItemByNumber(Integer number) {
        return _items.get(number);
    }
}
