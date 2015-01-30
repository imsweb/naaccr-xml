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

    private String section;

    private String recordTypes;

    private String sourceOfStandard;

    private String parentXmlElement;

    private String regexValidation;

    private String dataType;

    private String groupNaaccrId;

    private Boolean isGroup;

    private String retiredVersion;

    private String implementedVersion;
    
    public NaaccrDictionaryItem() {
        isGroup = false;
    }

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

    public String getGroupNaaccrId() {
        return groupNaaccrId;
    }

    public void setGroupNaaccrId(String val) {
        groupNaaccrId = val;
    }

    public Boolean getIsGroup() {
        return isGroup;
    }

    public void setIsGroup(Boolean val) {
        this.isGroup = val;
    }

    public String getRetiredVersion() {
        return retiredVersion;
    }

    public void setRetiredVersion(String val) {
        this.retiredVersion = val;
    }

    public String getImplementedVersion() {
        return implementedVersion;
    }

    public void setImplementedVersion(String val) {
        this.implementedVersion = val;
    }
}
