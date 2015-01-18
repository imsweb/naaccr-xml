/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class PatientXmlWriter implements AutoCloseable {

    private ObjectOutputStream _oos;
    
    private RuntimeNaaccrDictionary _dictionary;

    public PatientXmlWriter(Writer writer, String format, NaaccrDictionary nonStandardDictionary) throws IOException {
        _dictionary = new RuntimeNaaccrDictionary(format, NaaccrXmlUtils.getStandardDictionary(), nonStandardDictionary);

        // by default, XStream will write the root tag, but there is no easy way to add attributes to it; this is what this code does...
        PrettyPrintWriter prettyWriter = new PrettyPrintWriter(writer) {
            @Override
            public void startNode(String name) {
                super.startNode(name);
                if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(name)) {
                    addAttribute("naaccrVersion", _dictionary.getFormat().getNaaccrVersion());
                    addAttribute("recordType", _dictionary.getFormat().getRecordType());
                }
            }
        };
        _oos = NaaccrXmlUtils.getXStream().createObjectOutputStream(prettyWriter, "NaaccrDataExchange");
        
        // it's a bit manual, but I am not sure how else to do this...
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

    // TODO remove this testing method
    public static void main(String[] args) throws IOException {
        Patient patient = new Patient();
        patient.getItems().add(new Item("nameLast", "DEPRY"));
        patient.getItems().add(new Item("nameFirst", "FABIAN"));
        patient.getTumors().add(new Tumor());
        patient.getTumors().get(0).getItems().add(new Item("primarySite", "C619"));
        patient.getTumors().get(0).getItems().add(new Item("hosptialAbstractorId", "FDEPRY")); // should be ignored because not defined
        patient.getTumors().add(new Tumor());
        patient.getTumors().get(1).getItems().add(new Item("primarySite", "C447"));
        File outputFile = new File(System.getProperty("user.dir") + "/build/write-xml-test.xml");
        PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(outputFile), NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, null);
        writer.writePatient(patient);
        writer.close();
    }
}
