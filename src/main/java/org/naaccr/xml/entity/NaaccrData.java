/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity;

import java.util.ArrayList;
import java.util.List;

public class NaaccrData {

    private String naaccrVersion;
    
    private String recordType;

    private List<Patient> patients;

    public String getNaaccrVersion() {
        return naaccrVersion;
    }

    public void setNaaccrVersion(String val) {
        this.naaccrVersion = val;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String val) {
        this.recordType = val;
    }


    public List<Patient> getPatients() {
        if (patients == null)
            patients = new ArrayList<>();
        return patients;
    }
}
