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

import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionary {


    private List<RuntimeNaaccrDictionaryItem> items;

    public RuntimeNaaccrDictionary(String format, NaaccrDictionary standardDictionary, NaaccrDictionary userDictionary) {
        
        // get the record type from the format
        String recordType;
        if (format.endsWith("-abstract"))
            recordType = "A";
        else if (format.endsWith("-modified"))
            recordType = "M";
        else if (format.endsWith("-confidential"))
            recordType = "C";
        else if (format.endsWith("-incidence"))
            recordType = "I";
        else
            throw new RuntimeException("Invalid file format: " + format);

        Map<String, RuntimeNaaccrDictionaryItem> runtimeItems = new HashMap<>();
        for (NaaccrDictionaryItem item : standardDictionary.getItems())
            if (item.getRecordTypes().contains(recordType) && item.getParentItemId() == null) 
                runtimeItems.put(item.getId(), new RuntimeNaaccrDictionaryItem(item));
        for (NaaccrDictionaryItem item : standardDictionary.getItems())
            if (item.getRecordTypes().contains(recordType) && item.getParentItemId() != null)
                runtimeItems.get(item.getParentItemId()).getSubItems().add(new RuntimeNaaccrDictionaryItem(item));
        
        // TODO a user-defined field should never have the same id or number as a standard item...
        if (userDictionary != null) {
            for (NaaccrDictionaryItem item : userDictionary.getItems())
                if (item.getRecordTypes().contains(recordType) && item.getParentItemId() == null)
                    runtimeItems.put(item.getId(), new RuntimeNaaccrDictionaryItem(item));
            for (NaaccrDictionaryItem item : userDictionary.getItems())
                if (item.getRecordTypes().contains(recordType) && item.getParentItemId() != null)
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

    public List<RuntimeNaaccrDictionaryItem> getItems() {
        if (items == null)
            items = new ArrayList<>();
        return items;
    }
    
}
