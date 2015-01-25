/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary;

public class NaaccrDictionaryItem {

    private String id;

    private Integer number;

    private String name;

    private Integer startColumn;

    private Integer length;

    private String section;

    private String recordTypes;

    private String sourceOfStandard;

    private String parentXmlElement;

    private String regexValidation;

    private String dataType;

    private String parentItemId;

    public String getId() {
        return id;
    }

    public void setId(String val) {
        id = val;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer val) {
        number = val;
    }

    public String getName() {
        return name;
    }

    public void setName(String val) {
        name = val;
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

    public String getSection() {
        return section;
    }

    public void setSection(String val) {
        section = val;
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

    public String getRegexValidation() {
        return regexValidation;
    }

    public void setRegexValidation(String val) {
        regexValidation = val;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String val) {
        dataType = val;
    }

    public String getParentItemId() {
        return parentItemId;
    }

    public void setParentItemId(String val) {
        parentItemId = val;
    }
}
