/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class PatientXmlWriter implements AutoCloseable {

    protected ObjectOutputStream _oos;

    public PatientXmlWriter(Writer writer, XStream xstream, final RuntimeNaaccrDictionary dictionary) throws IOException {

        // by default, XStream will write the root tag, but there is no easy way to add attributes to it; this is what this code does...
        PrettyPrintWriter prettyWriter = new PrettyPrintWriter(writer) {
            @Override
            public void startNode(String name) {
                super.startNode(name);
                if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(name) && dictionary != null) {
                    addAttribute("baseDictionaryUri", dictionary.getBaseDictionaryUri());
                    addAttribute("userDictionaryUri", dictionary.getUserDictionaryUri()); // TODO FDP we shouldn't write it if this is the default one...
                    addAttribute("recordType", dictionary.getFormat().getRecordType());
                    addAttribute("timeGenerated", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date())); // TODO verify the format...
                }
            }
        };
        _oos = xstream.createObjectOutputStream(prettyWriter, "NaaccrDataExchange");
        
        // it's a bit manual, but I am not sure how else to do this...
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + System.getProperty("line.separator") + System.getProperty("line.separator"));
    }

    public void writePatient(Patient patient) throws IOException {
        _oos.writeObject(patient);
    }

    @Override
    public void close() throws IOException {
        _oos.close();
    }

    // TODO remove this testing method
    public static void main(String[] args) throws IOException {
        /**
        Patient patient = new Patient();
        patient.getItems().add(new Item("nameLast", "DEPRY"));
        patient.getItems().add(new Item("nameFirst", "FABIAN"));
        patient.getTumors().add(new Tumor());
        patient.getTumors().get(0).getItems().add(new Item("primarySite", "C619"));
        patient.getTumors().get(0).getItems().add(new Item("hosptialAbstractorId", "FDEPRY")); // should be ignored because not defined
        patient.getTumors().add(new Tumor());
        patient.getTumors().get(1).getItems().add(new Item("primarySite", "C447"));
        File outputFile = new File(System.getProperty("user.dir") + "/build/write-xml-test.xml");
        RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, null);
        PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(outputFile), NaaccrXmlUtils.getStandardXStream(dictionary, new NaaccrXmlOptions()), dictionary);
        writer.writePatient(patient);
        writer.close();
         */
    }
}
