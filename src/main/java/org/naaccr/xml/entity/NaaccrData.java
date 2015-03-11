/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NaaccrData {

    private String _baseDictionaryUri;

    private String _userDictionaryUri;
    
    private String _recordType;

    private Date _timeGenerated;

    private List<Patient> _patients;

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

    public List<Patient> getPatients() {
        if (_patients == null)
            _patients = new ArrayList<>();
        return _patients;
    }
}
