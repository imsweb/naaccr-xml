/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

public class Item {
    
    private String _naaccrId;
    
    private Integer _naaccrNum;
    
    private String _value;

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

    public String getValue() {
        return _value;
    }

    public void setValue(String value) {
        _value = value;
    }
    
}
