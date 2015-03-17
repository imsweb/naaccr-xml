/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.util.ArrayList;
import java.util.List;

public class NaaccrXmlOptions {
    
    public static final String ITEM_HANDLING_ERROR = "error";
    public static final String ITEM_HANDLING_IGNORE = "ignore";
    public static final String ITEM_HANDLING_PROCESS = "process";

    private Boolean _validateAgainstDictionary;
    
    private String _unknownItemHandling;

    private Boolean _validateValues;

    private Boolean _enforceRecordType;

    private List<String> _itemsToExclude;

    private List<String> _itemsToInclude;

    private List<String> _groupingItems;

    private Boolean _writeItemNumber;
    
    public NaaccrXmlOptions() {
        _validateAgainstDictionary = true;
        _unknownItemHandling = ITEM_HANDLING_ERROR;
        _validateValues = true;
        _enforceRecordType = true;
        _groupingItems = new ArrayList<>();
        _groupingItems.add("patientIdNumber");
        _writeItemNumber = false;
    }

    public Boolean getValidateAgainstDictionary() {
        return _validateAgainstDictionary;
    }

    public void setValidateAgainstDictionary(Boolean validateAgainstDictionary) {
        _validateAgainstDictionary = validateAgainstDictionary;
    }

    public String getUnknownItemHandling() {
        return _unknownItemHandling;
    }

    public void setUnknownItemHandling(String unknownItemHandling) {
        _unknownItemHandling = unknownItemHandling;
    }

    public Boolean getValidateValues() {
        return _validateValues;
    }

    public void setValidateValues(Boolean validateValues) {
        _validateValues = validateValues;
    }

    public Boolean getEnforceRecordType() {
        return _enforceRecordType;
    }

    public void setEnforceRecordType(Boolean enforceRecordType) {
        _enforceRecordType = enforceRecordType;
    }

    public List<String> getItemsToExclude() {
        return _itemsToExclude;
    }

    public void setItemsToExclude(List<String> itemsToExclude) {
        _itemsToExclude = itemsToExclude;
    }

    public List<String> getItemsToInclude() {
        return _itemsToInclude;
    }

    public void setItemsToInclude(List<String> itemsToInclude) {
        _itemsToInclude = itemsToInclude;
    }

    public List<String> getGroupingItems() {
        return _groupingItems;
    }

    public void setGroupingItems(List<String> groupingItems) {
        _groupingItems = groupingItems;
    }

    public Boolean getWriteItemNumber() {
        return _writeItemNumber;
    }

    public void setWriteItemNumber(Boolean writeItemNumber) {
        _writeItemNumber = writeItemNumber;
    }
}
