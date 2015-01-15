/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;

import org.naaccr.xml.entity.Patient;

import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class PatientWriter {

    private ObjectOutputStream _oos;

    public PatientWriter(Writer writer) throws IOException {
        _oos = XmlUtils.getXStream().createObjectOutputStream(new PrettyPrintWriter(writer), "NaaccrDataExchange");
    }

    public void writePatient(Patient patient) throws IOException {
        _oos.writeObject(patient);
    }

    public void close() throws IOException {
        _oos.close();
    }
}
