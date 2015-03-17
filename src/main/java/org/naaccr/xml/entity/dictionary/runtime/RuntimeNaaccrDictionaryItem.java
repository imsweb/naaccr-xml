/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary.runtime;

import java.util.regex.Pattern;

import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionaryItem {

    private String _naaccrId;

    private Integer _naaccrNum;

    private Integer _startColumn;

    private Integer _length;

    private String _parentXmlElement;

    private Pattern _regexValidation;

    private String _dataType;

    private String _trim;

    private String _padding;

    public RuntimeNaaccrDictionaryItem(NaaccrDictionaryItem item) {
        _naaccrId = item.getNaaccrId();
        _naaccrNum = item.getNaaccrNum();
        _startColumn = item.getStartColumn();
        _length = item.getLength();
        _parentXmlElement = item.getParentXmlElement();
        if (item.getRegexValidation() != null)
            _regexValidation = Pattern.compile(item.getRegexValidation());
        _dataType = item.getDataType();
        _trim = item.getTrim();
        _padding = item.getPadding();
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

    public String getParentXmlElement() {
        return _parentXmlElement;
    }

    public void setParentXmlElement(String parentXmlElement) {
        _parentXmlElement = parentXmlElement;
    }

    public Pattern getRegexValidation() {
        return _regexValidation;
    }

    public void setRegexValidation(Pattern regexValidation) {
        _regexValidation = regexValidation;
    }

    public String getDataType() {
        return _dataType;
    }

    public void setDataType(String dataType) {
        _dataType = dataType;
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
