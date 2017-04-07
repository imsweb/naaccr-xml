/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity.dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NaaccrDictionary {

    private String _dictionaryUri;

    private String _naaccrVersion;

    private String _specificationVersion;

    private String _description;

    private List<NaaccrDictionaryItem> _items;

    private List<NaaccrDictionaryGroupedItem> _groupedItems;

    // caches to improve lookup performances
    private Map<String, NaaccrDictionaryItem> _cachedById;
    private Map<Integer, NaaccrDictionaryItem> _cachedByNumber;
    private Map<String, NaaccrDictionaryGroupedItem> _groupedCachedById;
    private Map<Integer, NaaccrDictionaryGroupedItem> _groupedCachedByNumber;

    public NaaccrDictionary() {
        _items = new ArrayList<>();
        _cachedById = new HashMap<>();
        _cachedByNumber = new HashMap<>();
        _groupedCachedById = new HashMap<>();
        _groupedCachedByNumber = new HashMap<>();
    }

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

    public String getSpecificationVersion() {
        return _specificationVersion;
    }

    public void setSpecificationVersion(String specificationVersion) {
        _specificationVersion = specificationVersion;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public List<NaaccrDictionaryItem> getItems() {
        return Collections.unmodifiableList(_items);
    }

    public void setItems(List<NaaccrDictionaryItem> items) {
        if (items != null) {
            _items = items;
            for (NaaccrDictionaryItem item : items) {
                _cachedById.put(item.getNaaccrId(), item);
                _cachedByNumber.put(item.getNaaccrNum(), item);
            }
        }
    }

    public void addItem(NaaccrDictionaryItem item) {
        _items.add(item);
        if (item.getNaaccrId() != null)
            _cachedById.put(item.getNaaccrId(), item);
        if (item.getNaaccrNum() != null)
            _cachedByNumber.put(item.getNaaccrNum(), item);
    }

    public NaaccrDictionaryItem getItemByNaaccrId(String id) {
        // I don't know how/why, but it seems XStreams by-passes the default constructor, so I have to deal with a null cache here...
        if (_cachedById == null) {
            _cachedById = new HashMap<>();
            for (NaaccrDictionaryItem item : _items)
                _cachedById.put(item.getNaaccrId(), item);
        }
        return _cachedById.get(id);
    }

    public NaaccrDictionaryItem getItemByNaaccrNum(Integer number) {
        // I don't know how/why, but it seems XStreams by-passes the default constructor, so I have to deal with a null cache here...
        if (_cachedByNumber == null) {
            _cachedByNumber = new HashMap<>();
            for (NaaccrDictionaryItem item : _items)
                _cachedByNumber.put(item.getNaaccrNum(), item);
        }
        return _cachedByNumber.get(number);
    }

    public List<NaaccrDictionaryGroupedItem> getGroupedItems() {
        return Collections.unmodifiableList(_groupedItems);
    }

    public void setGroupedItems(List<NaaccrDictionaryGroupedItem> items) {
        if (items != null) {
            _groupedItems = items;
            for (NaaccrDictionaryGroupedItem item : items) {
                _groupedCachedById.put(item.getNaaccrId(), item);
                _groupedCachedByNumber.put(item.getNaaccrNum(), item);
            }
        }
    }

    public void addGroupedItem(NaaccrDictionaryGroupedItem item) {
        _groupedItems.add(item);
        if (item.getNaaccrId() != null)
            _groupedCachedById.put(item.getNaaccrId(), item);
        if (item.getNaaccrNum() != null)
            _groupedCachedByNumber.put(item.getNaaccrNum(), item);
    }

    public NaaccrDictionaryGroupedItem getGroupedItemByNaaccrId(String id) {
        // I don't know how/why, but it seems XStreams by-passes the default constructor, so I have to deal with a null cache here...
        if (_groupedCachedById == null) {
            _groupedCachedById = new HashMap<>();
            for (NaaccrDictionaryGroupedItem item : _groupedItems)
                _groupedCachedById.put(item.getNaaccrId(), item);
        }
        return _groupedCachedById.get(id);
    }

    public NaaccrDictionaryGroupedItem getGroupedItemByNaaccrNum(Integer number) {
        // I don't know how/why, but it seems XStreams by-passes the default constructor, so I have to deal with a null cache here...
        if (_groupedCachedByNumber == null) {
            _groupedCachedByNumber = new HashMap<>();
            for (NaaccrDictionaryGroupedItem item : _groupedItems)
                _groupedCachedByNumber.put(item.getNaaccrNum(), item);
        }
        return _groupedCachedByNumber.get(number);
    }
}
