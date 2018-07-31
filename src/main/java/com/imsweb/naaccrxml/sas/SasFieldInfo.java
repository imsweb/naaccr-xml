/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

/**
 * Small DTO to wrap basic field information.
 * <br/><br/>
 * This information is available in the regular item definition but those come from dictionaries which need to be parsed
 * from XML files; that parsing doesn't work in Java 7 which is still required by SAS. So for now I am using CSV file to
 * load that information into these DTOs.
 */
public class SasFieldInfo {

    private String _naaccrId;
    private String _truncatedNaaccrId;
    private String _parentTag;
    private Integer _length;
    private Integer _num;
    private String _name;
    private Integer _start;

    public SasFieldInfo(String naaccrId, String truncatedNaaccrId, String parentTag, Integer length, Integer num, String name, Integer start) {
        _naaccrId = naaccrId;
        _truncatedNaaccrId = truncatedNaaccrId;
        _parentTag = parentTag;
        _length = length;
        _num = num;
        _name = name;
        _start = start;
    }

    public String getTruncatedNaaccrId() {
        return _truncatedNaaccrId;
    }

    public String getNaaccrId() {
        return _naaccrId;
    }

    public String getParentTag() {
        return _parentTag;
    }

    public Integer getLength() {
        return _length;
    }

    public Integer getNum() {
        return _num;
    }

    public String getName() {
        return _name;
    }

    public Integer getStart() {
        return _start;
    }
}
