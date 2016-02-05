/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.imsweb.naaccrxml.NaaccrValidationError;

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
    protected Integer _startLineNumber;

    // caches to improve lookup performances
    protected Map<String, Item> _cachedById;

    /**
     * Returns all the items defined on this entity.
     * <br/><br/>
     * This method will only return the items that actually have a value. To iterates over all the items, regardless of their
     * value, consider using the dictionary instead.
     * @return the list of items that are contained in this entity
     */
    public List<Item> getItems() {
        if (_items == null)
            _items = new ArrayList<>();
        return _items;
    }

    /**
     * Returns the item corresponding to the requested ID, maybe null.
     * <br/><br/>
     * The returned item will be null if its value is null, so this method is not very useful.  If you want to just get the value,
     * consider using the gteItemValue() method instead. If you want to get the item definitions, consider using the dictionary instead.
     * @param id requested item ID
     * @return the corresponding item, sometimes null
     */
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

    /**
     * Returns the value of the item with the requested ID.
     * @param id requested item ID
     * @return the value of the corresponding item, sometimes null
     */
    public String getItemValue(String id) {
        Item item = getItem(id);
        if (item != null)
            return item.getValue();
        return null;
    }

    /**
     * Returns the validation error for the current entity.
     * @return collection of validation error, maybe empty but never null
     */
    public List<NaaccrValidationError> getValidationErrors() {
        if (_errors == null)
            _errors = new ArrayList<>();
        return _errors;
    }

    /**
     * Returns the line number of the current entity from the file it was read from.
     * <br/><br/>
     * Since it is not a requirement to create patients from a file, this might return null.
     * @return the line number of the current entity, maybe null
     */
    public Integer getStartLineNumber() {
        return _startLineNumber;
    }

    /**
     * Sets the line number of the current entity.
     * @param startLineNumber line number to set
     */
    public void setStartLineNumber(Integer startLineNumber) {
        _startLineNumber = startLineNumber;
    }
}
