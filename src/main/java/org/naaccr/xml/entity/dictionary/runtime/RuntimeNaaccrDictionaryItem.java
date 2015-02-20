/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity.dictionary.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionaryItem {

    private String naaccrId;

    private Integer naaccrNum;
    
    private Integer startColumn;

    private Integer length;

    private Set<String> recordTypes;

    private String parentXmlElement;

    private Pattern regexValidation;

    private String dataType;

    private String groupNaaccrId;
    
    private List<RuntimeNaaccrDictionaryItem> subItems;
    
    public RuntimeNaaccrDictionaryItem(NaaccrDictionaryItem item) {
        naaccrId = item.getNaaccrId();
        naaccrNum = item.getNaaccrNum();
        startColumn = item.getStartColumn();
        length = item.getLength();
        recordTypes = new HashSet<>();
        if (item.getRecordTypes() != null)
            recordTypes.addAll(Arrays.asList(item.getRecordTypes().split(",")));
        parentXmlElement = item.getParentXmlElement();
        if (item.getRegexValidation() != null)
            regexValidation = Pattern.compile(item.getRegexValidation());
        dataType = item.getDataType();
        groupNaaccrId = item.getGroupNaaccrId();
        subItems = new ArrayList<>();
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

    public Set<String> getRecordTypes() {
        return recordTypes;
    }

    public String getParentXmlElement() {
        return parentXmlElement;
    }

    public void setParentXmlElement(String val) {
        parentXmlElement = val;
    }

    public Pattern getRegexValidation() {
        return regexValidation;
    }

    public void setRegexValidation(Pattern val) {
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

    public List<RuntimeNaaccrDictionaryItem> getSubItems() {
        return subItems;
    }

}
