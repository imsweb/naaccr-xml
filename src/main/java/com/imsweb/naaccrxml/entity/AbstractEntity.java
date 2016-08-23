/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.imsweb.naaccrxml.NaaccrValidationError;

/**
 * Encapsulates the logic about the complex entities.
 * <ol>
 *     <li>Entity has a collection of items on it</li>
 *     <li>Entity has a collection of validation errors on it (most errors are attached to individual items so it will be rare that this collection contains any)</li>
 *     <li>Entity has a line number on it (might not always be populated)</li>
 * </ol>
 * This class also defines some utility methods to read/write those variables...
 */
public class AbstractEntity {

    // the items corresponding to this entity
    protected List<Item> _items;

    // the validation errors for this entity (most errors are attached to individual items; to gather all errors for an entity, use the getAllValidationErrors() method)
    protected List<NaaccrValidationError> _errors;

    // the line number for this entity; available only when reading from a file
    protected Integer _startLineNumber;

    // caches to improve lookup performances
    protected Map<String, Item> _cachedById;

    /**
     * Default constructor.
     */
    public AbstractEntity() {
        _items = new ArrayList<>();
        _errors = new ArrayList<>();
        _cachedById = new HashMap<>();
    }

    /**
     * Returns all the items defined on this entity.
     * <br/><br/>
     * This method will only return the items that actually have a value. To iterates over all the items, regardless of their
     * value, consider using the dictionary instead.
     * @return the list of items that are contained in this entity
     */
    public List<Item> getItems() {
        return Collections.unmodifiableList(_items);
    }

    /**
     * Adds an item to this entity.
     * <br/><br/>
     * This method makes no validation on the item or its value; for example, it doesn't check that an item with the same ID has already been added.
     * The caller can check before adding the item, or wait until the full patient is validated, which will fail for duplicate items.
     * @param item item to add, cannot be null
     */
    public void addItem(Item item) {
        _items.add(item);
        _cachedById.put(item.getNaaccrId(), item);
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
     * Returns the validation error for the current entity (for a patient entity, it would NOT return any error from the tumors, nor the errors on the patient's items).
     * @return collection of validation error, maybe empty but never null
     */
    public List<NaaccrValidationError> getValidationErrors() {
        return Collections.unmodifiableList(_errors);
    }

    /**
     * Adds a validation error on the current entity.
     * @param error error to add, cannot be null
     */
    public void addValidationError(NaaccrValidationError error) {
        _errors.add(error);
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
