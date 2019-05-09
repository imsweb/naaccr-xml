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

    // the base dictionary URI
    private String _baseDictionaryUri;

    // the list of user-defined dictionary URIs
    private List<String> _userDictionaryUri;

    // the record type(s)
    private String _recordType;

    // the time generated
    private Date _timeGenerated;

    // the NAACCR XML specification version
    private String _specificationVersion;

    // any extra (non-standard) root attributes
    private Map<String, String> _extraRootParameters;

    // the list of patients
    private List<Patient> _patients;

    /**
     * Constructor.
     */
    public NaaccrData() {
        super();
        _userDictionaryUri = new ArrayList<>();
        _extraRootParameters = new HashMap<>();
        _patients = new ArrayList<>();
    }

    /**
     * Constructor.
     * @param format the format for this naaccr data.
     */
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

    public List<String> getUserDictionaryUri() {
        return _userDictionaryUri;
    }

    public void setUserDictionaryUri(List<String> userDictionaryUri) {
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

    /**
     * Returns a non-modifiable view of the patients.
     * @return the patients for this naaccr data
     */
    public List<Patient> getPatients() {
        return Collections.unmodifiableList(_patients);
    }

    /**
     * Adds the given patient to this naaccr data.
     */
    public void addPatient(Patient patient) {
        _patients.add(patient);
    }

    /**
     * Removes the patient for the specified index.
     * @param patientIdx patient index, must fall in the range of existing patients.
     */
    public void removePatient(int patientIdx) {
        _patients.remove(patientIdx);
    }

    /**
     * Sets the patients for this naaccr data.
     * @param patients patients to set, if null, an empty collection will be used.
     */
    public void setPatients(List<Patient> patients) {
        _patients.clear();
        if (patients != null)
            _patients.addAll(patients);
    }
}
