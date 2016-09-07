/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;

/**
 * Corresponds to the "NaaccrData" element in the XML.
 */
public class NaaccrData extends AbstractEntity {

    private String _baseDictionaryUri;

    private String _userDictionaryUri;

    private String _recordType;

    private Date _timeGenerated;

    private String _specificationVersion;

    private Map<String, String> _extraRootParameters;

    private List<Patient> _patients;

    public NaaccrData() {
        super();
        _extraRootParameters = new HashMap<>();
        _patients = new ArrayList<>();
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

    public String getSpecificationVersion() {
        return _specificationVersion;
    }

    public void setSpecificationVersion(String specificationVersion) {
        _specificationVersion = specificationVersion;
    }

    public Map<String, String> getExtraRootParameters() {
        return Collections.unmodifiableMap(_extraRootParameters);
    }

    public void addExtraRootParameters(String key, String value) {
        _extraRootParameters.put(key, value);
    }

    public List<Patient> getPatients() {
        return Collections.unmodifiableList(_patients);
    }

    public void addPatient(Patient patient) {
        _patients.add(patient);
    }
}
