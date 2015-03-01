/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary;

public class NaaccrDictionaryItem {

    private String naaccrId;

    private Integer naaccrNum;

    private String naaccrName;

    private Integer startColumn;

    private Integer length;

    private String recordTypes;

    private String sourceOfStandard;

    private String parentXmlElement;

    private String dataType;

    private String regexValidation;

    public String getNaaccrId() {
        return naaccrId;
    }

    public void setNaaccrId(String val) {
        naaccrId = val;
    }

    public Integer getNaaccrNum() {
        return naaccrNum;
    }

    public void setNaaccrNum(Integer val) {
        naaccrNum = val;
    }

    public String getNaaccrName() {
        return naaccrName;
    }

    public void setNaaccrName(String val) {
        naaccrName = val;
    }

    public Integer getStartColumn() {
        return startColumn;
    }

    public void setStartColumn(Integer val) {
        startColumn = val;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer val) {
        length = val;
    }

    public String getRecordTypes() {
        return recordTypes;
    }

    public void setRecordTypes(String val) {
        recordTypes = val;
    }

    public String getSourceOfStandard() {
        return sourceOfStandard;
    }

    public void setSourceOfStandard(String val) {
        sourceOfStandard = val;
    }

    public String getParentXmlElement() {
        return parentXmlElement;
    }

    public void setParentXmlElement(String val) {
        parentXmlElement = val;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String val) {
        dataType = val;
    }
    
    public String getRegexValidation() {
        return regexValidation;
    }

    public void setRegexValidation(String val) {
        regexValidation = val;
    }
}
