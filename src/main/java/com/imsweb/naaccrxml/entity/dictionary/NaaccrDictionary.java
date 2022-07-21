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

    // the default XML namespace
    private String _defaultXmlNamespace;

    // the items contained in this dictionary
    private List<NaaccrDictionaryItem> _items;

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

    public String getDefaultXmlNamespace() {
        return _defaultXmlNamespace;
    }

    public void setDefaultXmlNamespace(String defaultXmlNamespace) {
        _defaultXmlNamespace = defaultXmlNamespace;
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
}
