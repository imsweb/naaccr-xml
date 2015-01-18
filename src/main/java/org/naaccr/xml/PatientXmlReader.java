/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;

import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;

public class PatientXmlReader implements AutoCloseable {

    private ObjectInputStream _ois;

    private RuntimeNaaccrDictionary _dictionary;

    public PatientXmlReader(Reader reader, String format, NaaccrDictionary nonStandardDictionary) throws IOException {
        this(NaaccrXmlUtils.getStandardXStream(), reader, format, nonStandardDictionary);
    }

    public PatientXmlReader(XStream xstream, Reader reader, String format, NaaccrDictionary nonStandardDictionary) throws IOException {
        _dictionary = new RuntimeNaaccrDictionary(format, NaaccrXmlUtils.getStandardDictionary(), nonStandardDictionary);
        _ois = xstream.createObjectInputStream(reader);
    }

    public Patient readPatient() throws IOException {
        try {
            Object object = _ois.readObject();
            if (object instanceof Patient) {
                // TODO use the dictionary and validate the patient
                return (Patient)object;
            }
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        catch (EOFException e) {
            // ignored, null will be returned
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        _ois.close();
    }
}
