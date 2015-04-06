/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import org.naaccr.xml.entity.Patient;

/**
 * This class can be used to observe the progress of reading/writing patients from a given source.
 */
public interface NaaccrObserver {

    /**
     * A patient has been read.
     * @param patient the read patient
     */
    void patientRead(Patient patient);

    /**
     * A patient has been written.
     * @param patient the written patient
     */
    void patientWritten(Patient patient);
}
