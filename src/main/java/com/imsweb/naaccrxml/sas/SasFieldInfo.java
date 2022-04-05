/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

import java.util.Arrays;
import java.util.List;

/**
 * Small DTO to wrap basic field information.
 * <br/><br/>
 * This information is available in the regular item definition but those come from dictionaries which need to be parsed
 * from XML files; that parsing doesn't work in Java 7 which is still required by SAS. So for now I am using CSV file to
 * load that information into these DTOs.
 */
public class SasFieldInfo {

    private final String _naaccrId;
    private final String _truncatedNaaccrId;
    private final String _parentTag;
    private final Integer _length;
    private final Integer _num;
    private final String _name;
    private final Integer _start;
    private final List<String> _contains;

    public SasFieldInfo(String naaccrId, String truncatedNaaccrId, String parentTag, Integer length, Integer num, String name, Integer start) {
        _naaccrId = naaccrId;
        _truncatedNaaccrId = truncatedNaaccrId;
        _parentTag = parentTag;
        _length = length;
        _num = num;
        _name = name;
        _start = start;
        _contains = null;
    }

    public SasFieldInfo(String naaccrId, Integer num, String name, String parentTag, Integer length, String contains) {
        _naaccrId = naaccrId;
        _truncatedNaaccrId = null;
        _parentTag = parentTag;
        _length = length;
        _num = num;
        _name = name;
        _start = null;
        _contains = Arrays.asList(contains.split(","));
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

    public List<String> getContains() {
        return _contains;
    }
}
