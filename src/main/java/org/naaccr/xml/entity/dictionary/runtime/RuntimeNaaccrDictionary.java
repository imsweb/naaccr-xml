/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.naaccr.xml.NaaccrFormat;
import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionary {

    private String _baseDictionaryUri;

    private String _userDictionaryUri;

    private NaaccrFormat _format;

    private List<RuntimeNaaccrDictionaryItem> _items;

    // caches used to improve lookup performances
    private Map<String, RuntimeNaaccrDictionaryItem> _cachedById;
    private Map<Integer, RuntimeNaaccrDictionaryItem> _cachedByNumber;

    public RuntimeNaaccrDictionary(String format, NaaccrDictionary userDictionary) {

        _format = NaaccrFormat.getInstance(format);

        NaaccrDictionary baseDictionary = NaaccrXmlUtils.getBaseDictionary(_format.getNaaccrVersion());
        if (userDictionary == null)
            userDictionary = NaaccrXmlUtils.getDefaultUserDictionary(_format.getNaaccrVersion());

        _baseDictionaryUri = baseDictionary.getDictionaryUri();
        _userDictionaryUri = userDictionary.getDictionaryUri();

        _items = new ArrayList<>();
        for (NaaccrDictionaryItem item : baseDictionary.getItems())
            _items.add(new RuntimeNaaccrDictionaryItem(item));
        for (NaaccrDictionaryItem item : userDictionary.getItems())
            _items.add(new RuntimeNaaccrDictionaryItem(item));

        // sort the fields by starting columns
        Collections.sort(_items, new Comparator<RuntimeNaaccrDictionaryItem>() {
            @Override
            public int compare(RuntimeNaaccrDictionaryItem o1, RuntimeNaaccrDictionaryItem o2) {
                return o1.getStartColumn().compareTo(o2.getStartColumn());
            }
        });
    }

    public String getBaseDictionaryUri() {
        return _baseDictionaryUri;
    }

    public String getUserDictionaryUri() {
        return _userDictionaryUri;
    }

    public NaaccrFormat getFormat() {
        return _format;
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

    public RuntimeNaaccrDictionaryItem getItemByNaaccrNum(Integer number) {
        if (_cachedByNumber == null) {
            Map<Integer, RuntimeNaaccrDictionaryItem> cache = new HashMap<>();
            for (RuntimeNaaccrDictionaryItem item : _items)
                if (item.getNaaccrNum() != null)
                    cache.put(item.getNaaccrNum(), item);
            _cachedByNumber = cache;
        }
        return _cachedByNumber.get(number);
    }

    public RuntimeNaaccrDictionaryItem getItem(Item item) {
        if (item.getId() != null)
            return getItemByNaaccrId(item.getId());
        if (item.getNum() != null)
            return getItemByNaaccrNum(item.getNum());
        return null;
    }
}
