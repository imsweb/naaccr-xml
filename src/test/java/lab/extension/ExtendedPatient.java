/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package lab.extension;

import com.imsweb.naaccrxml.entity.Patient;

public class ExtendedPatient extends Patient {
    
    private EditsReport _editsReport;

    public EditsReport getEditsReport() {
        return _editsReport;
    }

    public void setEditsReport(EditsReport val) {
        _editsReport = val;
    }
}
