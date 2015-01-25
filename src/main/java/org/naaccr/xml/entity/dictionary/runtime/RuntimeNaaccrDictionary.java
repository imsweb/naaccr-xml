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
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionary {

    private NaaccrFormat _format;
    
    private List<RuntimeNaaccrDictionaryItem> items;

    public RuntimeNaaccrDictionary(String format, NaaccrDictionary standardDictionary, NaaccrDictionary userDictionary) {
        
        _format = NaaccrFormat.getInstance(format);

        Map<String, RuntimeNaaccrDictionaryItem> runtimeItems = new HashMap<>();

        for (NaaccrDictionaryItem item : standardDictionary.getItems())
            if (item.getRecordTypes().contains(_format.getRecordType()) && item.getParentItemId() == null) 
                runtimeItems.put(item.getId(), new RuntimeNaaccrDictionaryItem(item));
        for (NaaccrDictionaryItem item : standardDictionary.getItems())
            if (item.getRecordTypes().contains(_format.getRecordType()) && item.getParentItemId() != null)
                runtimeItems.get(item.getParentItemId()).getSubItems().add(new RuntimeNaaccrDictionaryItem(item));
        
        // TODO a user-defined field should never have the same id or number as a standard item; there is all kind of validation we should be doing here...
        // TODO although, do we want to allow the length and things like that to be overridden?
        
        if (userDictionary != null) {
            for (NaaccrDictionaryItem item : userDictionary.getItems())
                if (item.getRecordTypes().contains(_format.getRecordType()) && item.getParentItemId() == null)
                    runtimeItems.put(item.getId(), new RuntimeNaaccrDictionaryItem(item));
            for (NaaccrDictionaryItem item : userDictionary.getItems())
                if (item.getRecordTypes().contains(_format.getRecordType()) && item.getParentItemId() != null)
                    runtimeItems.get(item.getParentItemId()).getSubItems().add(new RuntimeNaaccrDictionaryItem(item));
        }
        
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
        
        // don't forget to sort the subfields
        for (RuntimeNaaccrDictionaryItem item : items)
            Collections.sort(item.getSubItems(), comparator);
    }

    public NaaccrFormat getFormat() {
        return _format;
    }

    public List<RuntimeNaaccrDictionaryItem> getItems() {
        if (items == null)
            items = new ArrayList<>();
        return items;
    }
}
