/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary;

import java.util.List;

public class NaaccrDictionaryItem {

    private Integer _number;
    
    private String _name;
    
    private Integer _startColumn;
    
    private Integer _length;
    
    private String _section;
    
    private List<String> _recordType;
    
    private String _sourceOfStandard;
    
    private String _elementName;
    
    private String _parentElement;
    
    private String _regexValidation;
    
    private String _dataType;

    public Integer getNumber() {
        return _number;
    }

    public void setNumber(Integer number) {
        _number = number;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public Integer getStartColumn() {
        return _startColumn;
    }

    public void setStartColumn(Integer startColumn) {
        _startColumn = startColumn;
    }

    public Integer getLength() {
        return _length;
    }

    public void setLength(Integer length) {
        _length = length;
    }

    public String getSection() {
        return _section;
    }

    public void setSection(String section) {
        _section = section;
    }

    public List<String> getRecordType() {
        return _recordType;
    }

    public void setRecordType(List<String> recordType) {
        _recordType = recordType;
    }

    public String getSourceOfStandard() {
        return _sourceOfStandard;
    }

    public void setSourceOfStandard(String sourceOfStandard) {
        _sourceOfStandard = sourceOfStandard;
    }

    public String getElementName() {
        return _elementName;
    }

    public void setElementName(String elementName) {
        _elementName = elementName;
    }

    public String getParentElement() {
        return _parentElement;
    }

    public void setParentElement(String parentElement) {
        _parentElement = parentElement;
    }

    public String getRegexValidation() {
        return _regexValidation;
    }

    public void setRegexValidation(String regexValidation) {
        _regexValidation = regexValidation;
    }

    public String getDataType() {
        return _dataType;
    }

    public void setDataType(String dataType) {
        _dataType = dataType;
    }
}
