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
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;

public class PatientXmlReader implements AutoCloseable {

    protected ObjectInputStream _ois;

    public PatientXmlReader(Reader reader, XStream xstream) throws IOException {
        _ois = xstream.createObjectInputStream(reader);
    }

    public Patient readPatient() throws IOException, NaaccrValidationException {
        try {
            Object object = _ois.readObject();
            if (object instanceof Patient)
                return handlePatientObject((Patient)object);
            else
                handleNonPatientObject(object);
        }
        catch (ClassNotFoundException e) {
            throw new NaaccrValidationException(e.getMessage());
        }
        catch (ConversionException e) {
            NaaccrValidationException ex = new NaaccrValidationException(e.get("message"));
            ex.setLineNumber(e.get("line number") == null ? null : Integer.valueOf(e.get("line number")));
            ex.setPath(e.get("path"));
            throw ex;
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

    protected Patient handlePatientObject(Patient patient) {
        return patient; // default behavior is to do no post-processing of the patient...
    }
    
    protected void handleNonPatientObject(Object object) {
        // default behavior is to ignore the object...
    }

    // TODO remove this testing method
    public static void main(String[] args) throws Exception {
        File inputFile = new File(System.getProperty("user.dir") + "/src/test/resources/data/test-num-bad-item.xml");
        String format = NaaccrXmlUtils.getFormatFromXmlFile(inputFile);
        RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(format, NaaccrXmlUtils.getStandardDictionary(), null);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(inputFile), NaaccrXmlUtils.getStandardXStream(dictionary, new NaaccrXmlOptions()))) {
            Patient patient = reader.readPatient();
            System.out.println(patient.getItemById("nameLast"));
        }
        catch (IOException ex) {
            System.err.println("Unable to read next patient: " + ex.getMessage());
        }
        catch (NaaccrValidationException ex) {
            System.err.println("Unable to read next patient: line " + ex.getLineNumber() + " (path=" + ex.getPath() + "): " + ex.getMessage());
        }
    }
}
