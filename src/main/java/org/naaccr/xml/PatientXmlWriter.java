/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;

import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class PatientXmlWriter implements AutoCloseable {

    private ObjectOutputStream _oos;
    
    private RuntimeNaaccrDictionary _dictionary;

    public PatientXmlWriter(Writer writer, String format, NaaccrDictionary nonStandardDictionary) throws IOException {
        // TODO write some information as attribute of the NaaccrDataExchange tag
        _oos = XmlUtils.getXStream().createObjectOutputStream(new PrettyPrintWriter(writer), "NaaccrDataExchange");
        _dictionary = new RuntimeNaaccrDictionary(format, XmlUtils.getStandardDictionary(), nonStandardDictionary);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + System.getProperty("line.separator") + System.getProperty("line.separator"));
    }

    public void writePatient(Patient patient) throws IOException {
        // TODO FPD use the dictionary to validate the provided patient
        _oos.writeObject(patient);
    }

    @Override
    public void close() throws IOException {
        _oos.close();
    }
}
