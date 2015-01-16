/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import org.naaccr.xml.entity.Patient;

public class PatientFlatWriter implements AutoCloseable {

    private BufferedWriter _writer;

    public PatientFlatWriter(Writer writer) throws IOException {
        _writer = new BufferedWriter(writer);
    }

    public void writePatient(Patient patient) throws IOException {
        // TODO FPD
    }

    @Override
    public void close() throws IOException {
        _writer.close();
    }
}
