/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.regex.Pattern;

import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionaryItem {

    private String _naaccrId;

    private Integer _naaccrNum;

    private Integer _startColumn;

    private Integer _length;

    private Boolean _allowUnlimitedText;

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
        _allowUnlimitedText = item.getAllowUnlimitedText();
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

    public Integer getNaaccrNum() {
        return _naaccrNum;
    }

    public Integer getStartColumn() {
        return _startColumn;
    }

    public Integer getLength() {
        return _length;
    }

    public Boolean getAllowUnlimitedText() {
        return _allowUnlimitedText;
    }

    public String getParentXmlElement() {
        return _parentXmlElement;
    }

    public Pattern getRegexValidation() {
        return _regexValidation;
    }

    public String getDataType() {
        return _dataType;
    }

    public String getPadding() {
        return _padding;
    }

    public String getTrim() {
        return _trim;
    }
}
