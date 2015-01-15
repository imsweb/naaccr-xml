/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;

import org.naaccr.xml.entity.Patient;

public class PatientReader {

    private ObjectInputStream _ois;

    public PatientReader(Reader reader) throws IOException {
        _ois = XmlUtils.getXStream().createObjectInputStream(reader);
    }

    public Patient readPatient() throws IOException {
        try {
            Object object = _ois.readObject();
            if (object instanceof Patient)
                return (Patient)object;
            else
                System.out.println(object.getClass());
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }

        return null;
    }

    public void close() throws IOException {
        _ois.close();
    }

    public static void main(String[] args) throws Exception {
        PatientReader reader = new PatientReader(new FileReader(new File(System.getProperty("user.dir") + "/build/other-test.xml")));
        Patient patient = reader.readPatient();
        System.out.println(patient.getItems());
        System.out.println(patient.getTumors());
        reader.close();

    }
}
