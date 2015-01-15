/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary;

import java.util.Set;

public class NaaccrDictionaryItem {

    private Integer number;
    
    private String name;
    
    private Integer startColumn;
    
    private Integer length;
    
    private String section;
    
    private Set<String> recordTypes;
    
    private String sourceOfStandard;
    
    private String elementName;
    
    private String parentElement;
    
    private String regexValidation;
    
    private String dataType;

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

    public Set<String> getRecordTypes() {
        return recordTypes;
    }

    public void setRecordTypes(Set<String> val) {
        recordTypes = val;
    }

    public String getSourceOfStandard() {
        return sourceOfStandard;
    }

    public void setSourceOfStandard(String val) {
        sourceOfStandard = val;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String val) {
        elementName = val;
    }

    public String getParentElement() {
        return parentElement;
    }

    public void setParentElement(String val) {
        parentElement = val;
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
}
