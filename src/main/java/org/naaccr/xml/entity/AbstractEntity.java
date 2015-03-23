/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.naaccr.xml.NaaccrValidationError;

/**
 * Encapsulates the logic about the complex entities.
 * <ol>
 *     <li>Entity has a collection of items on it</li>
 *     <li>Entity has a collection of validation errors on it</li>
 *     <li>Entity has a line number on it (might not always be available)</li>
 * </ol>
 * This class also defines some utility methods to read/write those collections...
 */
public class AbstractEntity {

    // the items corresponding to this entity
    protected List<Item> _items;

    // the validation errors for this entity (for a patient, this collection would NOT contain the tumor errors...)
    protected List<NaaccrValidationError> _errors;

    // the line number for this entity; available only when reading from a file
    protected Integer _lineNumber;

    // caches to improve lookup performances
    protected Map<String, Item> _cachedById;

    public List<Item> getItems() {
        if (_items == null)
            _items = new ArrayList<>();
        return _items;
    }
    
    public Item getItem(String id) {
        if (_cachedById == null) {
            Map<String, Item> cache = new HashMap<>();
            for (Item item : getItems())
                if (item.getNaaccrId() != null)
                    cache.put(item.getNaaccrId(), item);
            _cachedById = cache;
        }
        return _cachedById.get(id);
    }
    
    public String getItemValue(String id) {
        Item item = getItem(id);
        if (item != null)
            return item.getValue();
        return null;
    }

    public List<NaaccrValidationError> getValidationErrors() {
        if (_errors == null)
            _errors = new ArrayList<>();
        return _errors;
    }

    public Integer getLineNumber() {
        return _lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        _lineNumber = lineNumber;
    }
}
