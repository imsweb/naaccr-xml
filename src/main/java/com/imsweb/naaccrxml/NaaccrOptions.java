/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates the options that a reader/writer can use to customize its operations.
 */
public class NaaccrOptions {

    /**
     * The different ways of handling an unknown item.
     */
    public static final String ITEM_HANDLING_ERROR = "error";
    public static final String ITEM_HANDLING_IGNORE = "ignore";
    public static final String ITEM_HANDLING_PROCESS = "process";

    /**
     * When reading data, if set to false, no validation of the values will take place (this applies only to data types, the length is always validated). Defaults to true.
     */
    private Boolean _validateReadValues;

    /**
     * When reading/writing XML data, how to handle unknown items (items with an ID that is not defined in the dictionary). See the handling constants. Defaults to ITEM_HANDLING_ERROR.
     */
    private String _unknownItemHandling;

    /**
     * When reading/writing data, the item IDs to process, anything other items will be ignored. Defaults to null which means process all items.
     */
    private List<String> _itemsToInclude;

    /**
     * When reading/writing data, the item IDs to ignore. Defaults to null, which means to not ignore any items.
     */
    private List<String> _itemsToExclude;

    /**
     * When reading data (from flat format), which item IDs to use to group the tumors into patients. If null/empty, no grouping takes place. Defaults to using the Patient ID Number.
     */
    private List<String> _tumorGroupingItems;

    /**
     * When reading data (from flat format), whether or not errors need to be reported for patient-level or root-level values mismatch. Default to false.
     */
    private Boolean _reportLevelMismatch;

    /**
     * When writing data (to XML format), if set to true, both the NAACCR ID and NAACCR Number will be written (otherwise only the ID is written). Defaults to false.
     */
    private Boolean _writeItemNumber;

    /**
     * When writing data (to XML or flat file format), whether or not the padding rules should be applied. Defaults to false.
     */
    private Boolean _applyPaddingRules;

    /**
     * When writing data, whether or not errors need to be reported for values too long (values will always be truncated, this only affect the error reporting mechanism). Defaults to false.
     */
    private Boolean _reportValuesTooLong;

    /**
     * When reading XML data, whether or not strict namespace rules need to be enforced. Defaults to true.
     */
    private Boolean _useStrictNamespaces;

    /**
     * When writing XML data, whether the control (non-printable) characters should be ignored or reported as error (since they are not valid in XML 1.0). Defaults to true.
     */
    private Boolean _ignoreControlCharacters;

    /**
     * When reading or writing XML data, whether the extensions should be ignore or not (defaults to false)
     */
    private Boolean _ignoreExtensions;

    /**
     * Convenience method to make the code look nicer, but it really just calls the default constructor!
     * @return an instance of the options with all default values.
     */
    public static NaaccrOptions getDefault() {
        return new NaaccrOptions();
    }

    /**
     * Default constructor.
     */
    public NaaccrOptions() {
        _validateReadValues = true;
        _unknownItemHandling = ITEM_HANDLING_ERROR;
        _tumorGroupingItems = new ArrayList<>();
        _tumorGroupingItems.add(NaaccrXmlUtils.DEFAULT_TUMOR_GROUPING_ITEM);
        _reportLevelMismatch = false;
        _writeItemNumber = false;
        _applyPaddingRules = false;
        _reportValuesTooLong = false;
        _useStrictNamespaces = true;
        _ignoreControlCharacters = true;
        _ignoreExtensions = false;
    }

    public Boolean getValidateReadValues() {
        return _validateReadValues;
    }

    public void setValidateReadValues(Boolean validateReadValues) {
        _validateReadValues = validateReadValues;
    }

    public String getUnknownItemHandling() {
        return _unknownItemHandling;
    }

    public void setUnknownItemHandling(String unknownItemHandling) {
        _unknownItemHandling = unknownItemHandling;
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

    public List<String> getTumorGroupingItems() {
        return _tumorGroupingItems;
    }

    public void setTumorGroupingItems(List<String> tumorGroupingItems) {
        _tumorGroupingItems = tumorGroupingItems;
    }

    public Boolean getReportLevelMismatch() {
        return _reportLevelMismatch;
    }

    public void setReportLevelMismatch(Boolean reportLevelMismatch) {
        _reportLevelMismatch = reportLevelMismatch;
    }

    public Boolean getWriteItemNumber() {
        return _writeItemNumber;
    }

    public void setWriteItemNumber(Boolean writeItemNumber) {
        _writeItemNumber = writeItemNumber;
    }

    public Boolean getApplyPaddingRules() {
        return _applyPaddingRules;
    }

    public void setApplyPaddingRules(Boolean applyPaddingRules) {
        _applyPaddingRules = applyPaddingRules;
    }

    public Boolean getReportValuesTooLong() {
        return _reportValuesTooLong;
    }

    public void setReportValuesTooLong(Boolean reportValuesTooLong) {
        _reportValuesTooLong = reportValuesTooLong;
    }

    public Boolean getUseStrictNamespaces() {
        return _useStrictNamespaces;
    }

    public void setUseStrictNamespaces(Boolean useStrictNamespaces) {
        _useStrictNamespaces = useStrictNamespaces;
    }

    public Boolean getIgnoreControlCharacters() {
        return _ignoreControlCharacters;
    }

    public void setIgnoreControlCharacters(Boolean ignoreControlCharacters) {
        _ignoreControlCharacters = ignoreControlCharacters;
    }

    public Boolean getIgnoreExtensions() {
        return _ignoreExtensions;
    }

    public void setIgnoreExtensions(Boolean ignoreExtensions) {
        _ignoreExtensions = ignoreExtensions;
    }

    /**
     * Convenience method that computes if a given item needs to be ignored, based on the include/exclude lists.
     * @param naaccrId NAACCR ID
     * @return true if the corresponding item needs to be processed.
     */
    public boolean processItem(String naaccrId) {
        if (_itemsToInclude != null)
            return _itemsToInclude.contains(naaccrId);
        else if (_itemsToExclude != null)
            return !_itemsToExclude.contains(naaccrId);
        return true;
    }
}
