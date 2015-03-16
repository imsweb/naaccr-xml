/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.extension;

import org.naaccr.xml.entity.Patient;

public class ExtendedPatient extends Patient {
    
    private EditsReport editsReport;

    public EditsReport getEditsReport() {
        return editsReport;
    }

    public void setEditsReport(EditsReport val) {
        editsReport = val;
    }
}
