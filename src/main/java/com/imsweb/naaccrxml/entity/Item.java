/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

public class Item {

    private String _naaccrId;

    private Integer _naaccrNum;

    private String _value;

    /**
     * Default constructor.
     */
    public Item() {
    }

    /**
     * Partial constructor.
     * @param naaccrId required NAACCR ID
     * @param value optional value
     */
    public Item(String naaccrId, String value) {
        _naaccrId = naaccrId;
        _value = value;
    }

    /**
     * Full constructor.
     * @param naaccrId required NAACCR ID
     * @param naaccrNum optional NAACCR Number
     * @param value optional value
     */
    public Item(String naaccrId, Integer naaccrNum, String value) {
        _naaccrId = naaccrId;
        _naaccrNum = naaccrNum;
        _value = value;
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

    public String getValue() {
        return _value;
    }

    public void setValue(String value) {
        _value = value;
    }

}
