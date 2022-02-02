/*
 * Copyright (C) 2022 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import com.imsweb.naaccrxml.entity.Patient;

public interface NaaccrPatientProcessor {

    /**
     * Processes the given Patient (what that really means depend on the implementation).
     * <br/><br/>
     * This interface can be used to remove, add or update fields on each patient in a data file or in a stream.
     */
    void processPatient(Patient patient);
}
