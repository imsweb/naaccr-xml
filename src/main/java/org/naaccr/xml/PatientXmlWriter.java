/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;

import org.naaccr.xml.entity.Patient;

import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class PatientXmlWriter implements AutoCloseable {

    private ObjectOutputStream _oos;

    public PatientXmlWriter(Writer writer) throws IOException {
        _oos = XmlUtils.getXStream().createObjectOutputStream(new PrettyPrintWriter(writer), "NaaccrDataExchange");
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + System.getProperty("line.separator") + System.getProperty("line.separator"));
    }

    public void writePatient(Patient patient) throws IOException {
        _oos.writeObject(patient);
    }

    @Override
    public void close() throws IOException {
        _oos.close();
    }
}
