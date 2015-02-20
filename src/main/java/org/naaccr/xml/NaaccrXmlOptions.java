/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.util.List;

public class NaaccrXmlOptions {
    
    public static final String ITEM_HANDLING_ERROR = "error";
    public static final String ITEM_HANDLING_IGNORE = "ignore";
    public static final String ITEM_HANDLING_PROCESS = "process";

    private Boolean _validateAgainstDictionary;
    
    private String _retiredItemHandling;
    
    private String _unknownItemHandling;

    private Boolean _validateValues;

    private Boolean _enforceRecordType;

    private List<String> _itemsToExclude;

    private List<String> _itemsToInclude;
    
    public NaaccrXmlOptions() {
        _validateAgainstDictionary = true;
        _retiredItemHandling = ITEM_HANDLING_ERROR;
        _unknownItemHandling = ITEM_HANDLING_ERROR;
        _validateValues = true;
    }

    public Boolean getValidateAgainstDictionary() {
        return _validateAgainstDictionary;
    }

    public void setValidateAgainstDictionary(Boolean validateAgainstDictionary) {
        _validateAgainstDictionary = validateAgainstDictionary;
    }

    public String getRetiredItemHandling() {
        return _retiredItemHandling;
    }

    public void setRetiredItemHandling(String retiredItemHandling) {
        _retiredItemHandling = retiredItemHandling;
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
    
}
