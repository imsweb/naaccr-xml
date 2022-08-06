/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates the options that a reader/writer can use to customize its operations.
 */
public class NaaccrOptions {

    /**
     * The different ways of handling an unknown item.
     */
    public static final String ITEM_HANDLING_ERROR = "error"; // raise a validation error for the unknown item, do not copy the value
    public static final String ITEM_HANDLING_IGNORE = "ignore"; // ignore the unknown item
    public static final String ITEM_HANDLING_PROCESS = "process"; // copy the value of the unknown item into the patient data

    /**
     * The different ways of specifying new lines.
     */
    public static final String NEW_LINE_OS = "OS"; // let the Operating System decide
    public static final String NEW_LINE_LF = "LF"; // Line Feed only
    public static final String NEW_LINE_CRLF = "CRLF"; // Carriage Return followed by Line Feed

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
     * When writing data (to XML or flat file format), whether or not the zero-padding rules should be applied. Defaults to false.
     */
    private Boolean _applyZeroPaddingRules;

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
     * When writing flat or XML data, which new line flavor to use (this does not apply to new lines appearing in the data itself). Defaults to the OS flavor.
     */
    private String _newLine;

    /**
     * When reading or writing XML data, whether the extensions should be ignore or not (defaults to false)
     */
    private Boolean _ignoreExtensions;

    /**
     * When reading XML data, should the renamed standard IDs be automatically translated (this also includes items that changed level). Defaults to false.
     */
    private Boolean _translateRenamedStandardItemIds;

    /**
     * When reading XML data, the provided item IDs will be automatically translated (before any validation happens). Default is a null map, which means no translation.
     */
    private Map<String, String> _itemIdsToTranslate;


    /**
     * When reading XML data, the provided dictionary IDs will be automatically translated (before any validation happens). Default is a null map, which means no translation.
     */
    private Map<String, String> _dictionaryIdsToTranslate;

    /**
     * When reading XML data, whether a missing user-defined dictionary should trigger an exception from the reader. Defaults to true (meaning by default missing dictionaries don't trigger an exception in the reader).
     */
    private Boolean _allowMissingDictionary;

    /**
     * The specification version to use when writing XML data. Defaults to the "current" specification version defined in NaaccrXmlUtils.
     */
    private String _specificationVersionWritten;

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
        _applyZeroPaddingRules = false;
        _reportValuesTooLong = false;
        _useStrictNamespaces = true;
        _ignoreControlCharacters = true;
        _ignoreExtensions = false;
        _translateRenamedStandardItemIds = false;
        _itemIdsToTranslate = null;
        _dictionaryIdsToTranslate = null;
        _newLine = NEW_LINE_OS;
        _allowMissingDictionary = true;
        _specificationVersionWritten = NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION;
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

    public Boolean getApplyZeroPaddingRules() {
        return _applyZeroPaddingRules;
    }

    public void setApplyZeroPaddingRules(Boolean applyZeroPaddingRules) {
        _applyZeroPaddingRules = applyZeroPaddingRules;
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

    public String getNewLine() {
        return _newLine;
    }

    public void setNewLine(String newLine) {
        _newLine = newLine;
    }

    public Boolean getIgnoreExtensions() {
        return _ignoreExtensions;
    }

    public void setIgnoreExtensions(Boolean ignoreExtensions) {
        _ignoreExtensions = ignoreExtensions;
    }

    public Boolean getTranslateRenamedStandardItemIds() {
        return _translateRenamedStandardItemIds;
    }

    public void setTranslateRenamedStandardItemIds(Boolean translateRenamedStandardItemIds) {
        _translateRenamedStandardItemIds = translateRenamedStandardItemIds;
    }

    public Map<String, String> getItemIdsToTranslate() {
        return _itemIdsToTranslate;
    }

    public void setItemIdsToTranslate(Map<String, String> itemIdsToTranslate) {
        _itemIdsToTranslate = itemIdsToTranslate;
    }

    public Map<String, String> getDictionaryIdsToTranslate() {
        return _dictionaryIdsToTranslate;
    }

    public void setDictionaryIdsToTranslate(Map<String, String> dictionaryIdsToTranslate) {
        _dictionaryIdsToTranslate = dictionaryIdsToTranslate;
    }

    public Boolean getAllowMissingDictionary() {
        return _allowMissingDictionary;
    }

    public void setAllowMissingDictionary(Boolean allowMissingDictionary) {
        _allowMissingDictionary = allowMissingDictionary;
    }

    public String getSpecificationVersionWritten() {
        return _specificationVersionWritten;
    }

    public void setSpecificationVersionWritten(String specificationVersionWritten) {
        _specificationVersionWritten = specificationVersionWritten;
    }

    /**
     * Convenience method that computes if a given item needs to be ignored, based on the include/exclude lists.
     * @param naaccrId NAACCR ID
     * @return true if the corresponding item needs to be processed.
     */
    public static boolean processItem(NaaccrOptions options, String naaccrId) {
        if (options.getItemsToInclude() != null)
            return options.getItemsToInclude().contains(naaccrId);
        else if (options.getItemsToExclude() != null)
            return !options.getItemsToExclude().contains(naaccrId);
        return true;
    }
}
