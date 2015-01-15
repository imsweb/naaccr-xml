/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;

import org.naaccr.xml.entity.Patient;

public class PatientXmlReader implements AutoCloseable {

    private ObjectInputStream _ois;

    public PatientXmlReader(Reader reader) throws IOException {
        _ois = XmlUtils.getXStream().createObjectInputStream(reader);
    }

    public Patient readPatient() throws IOException {
        try {
            Object object = _ois.readObject();
            if (object instanceof Patient)
                return (Patient)object;
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

    public static void main(String[] args) throws Exception {
        PatientXmlReader reader = new PatientXmlReader(new FileReader(new File(System.getProperty("user.dir") + "/build/test.xml")));
        Patient patient = reader.readPatient();
        System.out.println(patient.getItems());
        System.out.println(patient.getTumors());
        reader.close();

    }
}
