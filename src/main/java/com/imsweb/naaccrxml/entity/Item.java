/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

import com.imsweb.naaccrxml.NaaccrValidationError;

/**
 * Corresponds to the "Item" element in the XML.
 */
public class Item {

    // the NAACCR ID that uniquely identifies this item (required)
    protected String _naaccrId;

    // the NAACCR Number, not required according to the specs, but this library sets it when reading data files, even if they are not provided...
    protected Integer _naaccrNum;

    // the value for this item
    protected String _value;

    // the line number for this item; available only when reading from a file
    protected Integer _startLineNumber;

    // the validation error for this item (null if it has no error)
    protected NaaccrValidationError _validationError;

    /**
     * Default constructor.
     */
    public Item() {
    }

    /**
     * Partial constructor.
     * @param naaccrId required NAACCR ID
     * @param value optional value
     */
    public Item(String naaccrId, String value) {
        _naaccrId = naaccrId;
        _value = value;
    }

    /**
     * Full constructor.
     * @param naaccrId required NAACCR ID
     * @param naaccrNum optional NAACCR Number
     * @param value optional value
     * @param startLineNumber optional line number
     */
    public Item(String naaccrId, Integer naaccrNum, String value, Integer startLineNumber) {
        _naaccrId = naaccrId;
        _naaccrNum = naaccrNum;
        _value = value;
        _startLineNumber = startLineNumber;
    }

    public String getNaaccrId() {
        return _naaccrId;
    }

    public void setNaaccrId(String naaccrId) {
        _naaccrId = naaccrId;
    }

    public Integer getNaaccrNum() {
        return _naaccrNum;
    }

    public void setNaaccrNum(Integer naaccrNum) {
        _naaccrNum = naaccrNum;
    }

    public String getValue() {
        return _value;
    }

    public void setValue(String value) {
        _value = value;
    }

    /**
     * Returns the line number of this item from the file it was read from.
     * <br/><br/>
     * Since it is not a requirement to create patients from a file, this might return null.
     * @return the line number of this item, maybe null
     */
    public Integer getStartLineNumber() {
        return _startLineNumber;
    }

    /**
     * Sets the line number of this item.
     * @param startLineNumber line number to set
     */
    public void setStartLineNumber(Integer startLineNumber) {
        _startLineNumber = startLineNumber;
    }

    public NaaccrValidationError getValidationError() {
        return _validationError;
    }

    public void setValidationError(NaaccrValidationError validationError) {
        _validationError = validationError;
    }
}
