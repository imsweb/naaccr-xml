/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

import org.naaccr.xml.entity.Patient;

public class PatientFlatReader implements AutoCloseable {

    protected LineNumberReader _reader;

    public PatientFlatReader(Reader reader) throws IOException {
        _reader = new LineNumberReader(reader);
    }

    public Patient readPatient() throws IOException {
        return null; // TODO FPD
    }

    @Override
    public void close() throws IOException {
        _reader.close();
    }
}
