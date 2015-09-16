/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;

public class NaaccrData extends AbstractEntity {

    private String _baseDictionaryUri;

    private String _userDictionaryUri;
    
    private String _recordType;

    private Date _timeGenerated;
    
    private Map<String, String> _extraRootParameters;

    private List<Patient> _patients;
    
    public NaaccrData() {
    }
    
    public NaaccrData(String format) {
        this();
        NaaccrFormat naaccrFormat = NaaccrFormat.getInstance(format);
        _baseDictionaryUri = NaaccrXmlDictionaryUtils.createUriFromVersion(naaccrFormat.getNaaccrVersion(), true);
        _recordType = naaccrFormat.getRecordType();
        _timeGenerated = new Date();
    }

    public String getBaseDictionaryUri() {
        return _baseDictionaryUri;
    }

    public void setBaseDictionaryUri(String baseDictionaryUri) {
        _baseDictionaryUri = baseDictionaryUri;
    }

    public String getUserDictionaryUri() {
        return _userDictionaryUri;
    }

    public void setUserDictionaryUri(String userDictionaryUri) {
        _userDictionaryUri = userDictionaryUri;
    }

    public String getRecordType() {
        return _recordType;
    }

    public void setRecordType(String recordType) {
        _recordType = recordType;
    }

    public Date getTimeGenerated() {
        return _timeGenerated;
    }

    public void setTimeGenerated(Date timeGenerated) {
        _timeGenerated = timeGenerated;
    }

    public Map<String, String> getExtraRootParameters() {
        if (_extraRootParameters == null)
            _extraRootParameters = new HashMap<>();
        return _extraRootParameters;
    }
    
    public List<Patient> getPatients() {
        if (_patients == null)
            _patients = new ArrayList<>();
        return _patients;
    }
}
