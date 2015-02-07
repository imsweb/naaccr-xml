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
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

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
            throw new RuntimeException(e);
        }
        catch (ConversionException e) {
            String msg = e.get("message") != null ? e.get("message") : e.getMessage();
            if (msg != null) {
                msg = msg.trim();
                if (msg.startsWith(":"))
                    msg = msg.substring(1);
                int idx = msg.indexOf("from line");
                if (idx != -1)
                    msg = msg.substring(0, idx).trim();
            }
            NaaccrValidationException ex = new NaaccrValidationException(msg);
            ex.setLineNumber(e.get("line number") == null ? null : Integer.valueOf(e.get("line number")));
            ex.setPath(e.get("path"));
            throw ex;
        }

        catch (CannotResolveClassException e)

        {
            NaaccrValidationException ex = new NaaccrValidationException("Unexpected tag: " + e.getMessage());
            // TODO would it be possible to get the line number from the parser? That parser is referenced in the xstream object...
            throw ex;
        }

        catch (EOFException e)

        {
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
        File inputFile = new File(System.getProperty("user.dir") + "/src/test/resources/data/test-num-bad-item3.xml");
        String format = NaaccrXmlUtils.getFormatFromXmlFile(inputFile);
        RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(format, NaaccrXmlUtils.getStandardDictionary(), null);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(inputFile), NaaccrXmlUtils.getStandardXStream(dictionary, new NaaccrXmlOptions()))) {
            do {
                Patient patient = reader.readPatient();
                if (patient == null)
                    break;
                System.out.println("   > patientIdNumber=" + patient.getItemValue("patientIdNumber", 20));
                for (Tumor tumor : patient.getTumors())
                    System.out.println("      > primarySite=" + tumor.getItemValue("primarySite", 400));
                for (NaaccrValidationError error : patient.getAllValidationErrors())
                    System.out.println("   > line " + error.getLineNumber() + " [" + error.getPath() + "]: " + error.getMessage());
            } while (true);
        }
        catch (NaaccrValidationException ex) {
            System.out.println("   > line " + ex.getLineNumber() + " [path=" + ex.getPath() + "]: " + ex.getMessage());
        }
        catch (IOException ex) {
            System.out.println("Unable to read next patient: " + ex.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
