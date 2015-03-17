/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import org.naaccr.xml.entity.Patient;

public interface NaaccrStreamObserver {

    void patientRead(Patient patient);

    void patientWritten(Patient patient);
}
