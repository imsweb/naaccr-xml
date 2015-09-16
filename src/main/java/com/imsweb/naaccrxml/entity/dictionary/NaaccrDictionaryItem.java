/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity.dictionary;

/**
 * This class encapsulates a single dictionary item, as provided in the dictionary XML files.
 */
public class NaaccrDictionaryItem {

    private String _naaccrId;

    private Integer _naaccrNum;

    private String _naaccrName;

    private Integer _startColumn;

    private Integer _length;

    private String _recordTypes;

    private String _sourceOfStandard;

    private String _parentXmlElement;

    private String _dataType;

    private String _regexValidation;

    private String _padding;

    private String _trim;

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

    public String getNaaccrName() {
        return _naaccrName;
    }

    public void setNaaccrName(String naaccrName) {
        _naaccrName = naaccrName;
    }

    public Integer getStartColumn() {
        return _startColumn;
    }

    public void setStartColumn(Integer naaccrName) {
        _startColumn = naaccrName;
    }

    public Integer getLength() {
        return _length;
    }

    public void setLength(Integer length) {
        _length = length;
    }

    public String getRecordTypes() {
        return _recordTypes;
    }

    public void setRecordTypes(String recordTypes) {
        _recordTypes = recordTypes;
    }

    public String getSourceOfStandard() {
        return _sourceOfStandard;
    }

    public void setSourceOfStandard(String sourceOfStandard) {
        _sourceOfStandard = sourceOfStandard;
    }

    public String getParentXmlElement() {
        return _parentXmlElement;
    }

    public void setParentXmlElement(String parentXmlElement) {
        _parentXmlElement = parentXmlElement;
    }

    public String getDataType() {
        return _dataType;
    }

    public void setDataType(String dataType) {
        _dataType = dataType;
    }

    public String getRegexValidation() {
        return _regexValidation;
    }

    public void setRegexValidation(String regexValidation) {
        _regexValidation = regexValidation;
    }

    public String getPadding() {
        return _padding;
    }

    public void setPadding(String padding) {
        _padding = padding;
    }

    public String getTrim() {
        return _trim;
    }

    public void setTrim(String trim) {
        _trim = trim;
    }
}
