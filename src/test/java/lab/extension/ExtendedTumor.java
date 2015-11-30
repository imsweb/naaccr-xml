/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package lab.extension;

import com.imsweb.naaccrxml.entity.Tumor;

public class ExtendedTumor extends Tumor {

    private EditsReport _editsReport;

    public EditsReport getEditsReport() {
        return _editsReport;
    }

    public void setEditsReport(EditsReport val) {
        _editsReport = val;
    }
    
}
