/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity.dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NaaccrDictionary {

    // the dictionary identifier (might look like an internet address but most of the time it's not)
    private String _dictionaryUri;

    // the NAACCR version for this dictionary
    private String _naaccrVersion;

    // specification version implemented by this dictionary
    private String _specificationVersion;

    // the last time this dictionary was updated/modified
    private Date _dateLastModified;

    // a description for this dictionary
    private String _description;

    // the items contained in this dictionary
    private List<NaaccrDictionaryItem> _items;

    // the grouped items contained in this dictionary
    private List<NaaccrDictionaryGroupedItem> _groupedItems;

    // caches to improve lookup performances
    private final Map<String, NaaccrDictionaryItem> _cachedById;
    private final Map<Integer, NaaccrDictionaryItem> _cachedByNumber;

    // cache access needs to be synchronized!
    private static final Object _CACHED_LOCK = new Object();

    public NaaccrDictionary() {
        _items = new ArrayList<>();
        _cachedById = new HashMap<>();
        _cachedByNumber = new HashMap<>();
        // I am not using an empty collection for grouped items because we don't want them to appear as an empty tag in the XML, so we need null...
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

    public Date getDateLastModified() {
        return _dateLastModified;
    }

    public void setDateLastModified(Date dateLastModified) {
        _dateLastModified = dateLastModified;
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
            synchronized (_CACHED_LOCK) {
                for (NaaccrDictionaryItem item : items) {
                    _cachedById.put(item.getNaaccrId(), item);
                    _cachedByNumber.put(item.getNaaccrNum(), item);
                }
            }
        }
    }

    public void addItem(NaaccrDictionaryItem item) {
        _items.add(item);
        synchronized (_CACHED_LOCK) {
            if (item.getNaaccrId() != null)
                _cachedById.put(item.getNaaccrId(), item);
            if (item.getNaaccrNum() != null)
                _cachedByNumber.put(item.getNaaccrNum(), item);
        }
    }

    public NaaccrDictionaryItem getItemByNaaccrId(String id) {
        synchronized (_CACHED_LOCK) {
            if (_cachedById.isEmpty())
                for (NaaccrDictionaryItem item : _items)
                    _cachedById.put(item.getNaaccrId(), item);
            return _cachedById.get(id);
        }
    }

    public NaaccrDictionaryItem getItemByNaaccrNum(Integer number) {
        synchronized (_CACHED_LOCK) {
            if (_cachedByNumber.isEmpty())
                for (NaaccrDictionaryItem item : _items)
                    _cachedByNumber.put(item.getNaaccrNum(), item);
            return _cachedByNumber.get(number);
        }
    }

    public List<NaaccrDictionaryGroupedItem> getGroupedItems() {
        if (_groupedItems == null)
            return Collections.emptyList();
        return Collections.unmodifiableList(_groupedItems);
    }

    public void setGroupedItems(List<NaaccrDictionaryGroupedItem> items) {
        _groupedItems = items;
    }

    public void addGroupedItem(NaaccrDictionaryGroupedItem item) {
        if (_groupedItems == null)
            _groupedItems = new ArrayList<>();
        _groupedItems.add(item);
    }

    public NaaccrDictionaryGroupedItem getGroupedItemByNaaccrId(String id) {
        if (_groupedItems == null)
            return null;
        for (NaaccrDictionaryGroupedItem groupedItem : _groupedItems)
            if (groupedItem.getNaaccrId().equals(id))
                return groupedItem;
        return null;
    }

    public NaaccrDictionaryGroupedItem getGroupedItemByNaaccrNum(Integer number) {
        if (_groupedItems == null)
            return null;
        for (NaaccrDictionaryGroupedItem groupedItem : _groupedItems)
            if (groupedItem.getNaaccrNum().equals(number))
                return groupedItem;
        return null;
    }
}
