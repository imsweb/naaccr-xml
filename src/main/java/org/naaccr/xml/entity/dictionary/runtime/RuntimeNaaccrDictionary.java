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

    private NaaccrFormat _format;

    private List<RuntimeNaaccrDictionaryItem> items;

    // caches to improve lookup performances
    private Map<String, RuntimeNaaccrDictionaryItem> _cachedById;
    private Map<Integer, RuntimeNaaccrDictionaryItem> _cachedByNumber;

    public RuntimeNaaccrDictionary(String format, NaaccrDictionary userDictionary) {

        _format = NaaccrFormat.getInstance(format);

        NaaccrDictionary baseDictionary = NaaccrXmlUtils.getBaseDictionary(_format.getNaaccrVersion());
        if (userDictionary == null)
            userDictionary = NaaccrXmlUtils.getDefaultUserDictionary(_format.getNaaccrVersion());

        Map<String, RuntimeNaaccrDictionaryItem> runtimeItems = new HashMap<>();

        for (NaaccrDictionaryItem item : baseDictionary.getItems())
            runtimeItems.put(item.getNaaccrId(), new RuntimeNaaccrDictionaryItem(item));
        for (NaaccrDictionaryItem item : userDictionary.getItems())
            runtimeItems.put(item.getNaaccrId(), new RuntimeNaaccrDictionaryItem(item));

        // now we are ready to assign the fields
        items = new ArrayList<>(runtimeItems.values());

        // we will use a shared comparator that will sort the fields by starting columns
        Comparator<RuntimeNaaccrDictionaryItem> comparator = new Comparator<RuntimeNaaccrDictionaryItem>() {
            @Override
            public int compare(RuntimeNaaccrDictionaryItem o1, RuntimeNaaccrDictionaryItem o2) {
                return o1.getStartColumn().compareTo(o2.getStartColumn());
            }
        };

        // sort the fields
        Collections.sort(items, comparator);
    }

    public NaaccrFormat getFormat() {
        return _format;
    }

    public List<RuntimeNaaccrDictionaryItem> getItems() {
        if (items == null)
            items = new ArrayList<>();
        return items;
    }

    public RuntimeNaaccrDictionaryItem getItemByNaaccrId(String id) {
        if (_cachedById == null) {
            Map<String, RuntimeNaaccrDictionaryItem> cache = new HashMap<>();
            for (RuntimeNaaccrDictionaryItem item : items)
                if (item.getNaaccrId() != null)
                    cache.put(item.getNaaccrId(), item);
            _cachedById = cache;
        }
        return _cachedById.get(id);
    }

    public RuntimeNaaccrDictionaryItem getItemByNaaccrNum(Integer number) {
        if (_cachedByNumber == null) {
            Map<Integer, RuntimeNaaccrDictionaryItem> cache = new HashMap<>();
            for (RuntimeNaaccrDictionaryItem item : items)
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
