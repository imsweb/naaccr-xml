/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package lab.extension;

import com.imsweb.naaccrxml.entity.Patient;

public class ExtendedPatient extends Patient {
    
    private EditsReport editsReport;

    public EditsReport getEditsReport() {
        return editsReport;
    }

    public void setEditsReport(EditsReport val) {
        editsReport = val;
    }
}
