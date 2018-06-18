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

    private String _parentTag;

    private Integer _length;

    public SasFieldInfo(String naaccrId, String parentTag, String length) {
        _naaccrId = naaccrId;
        _parentTag = parentTag;
        _length = Integer.valueOf(length);
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
}
