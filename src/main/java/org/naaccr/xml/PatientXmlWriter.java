/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class PatientXmlWriter implements AutoCloseable {

    protected ObjectOutputStream _oos;

    public PatientXmlWriter(Writer writer, String format) throws IOException {
        this(writer, new NaaccrData(format), null, null, null);
    }

    public PatientXmlWriter(Writer writer, NaaccrData rootData) throws IOException {
        this(writer, rootData, null, null, null);
    }

    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrXmlOptions options) throws IOException {
        this(writer, rootData, options, null, null);
    }

    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws IOException {
        this(writer, rootData, options, userDictionary, null);
    }
    
    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrXmlOptions options, NaaccrDictionary userDictionary, XmlStreamConfiguration configuration) throws IOException {

        // we always need options
        if (options == null)
            options = new NaaccrXmlOptions();

        // we always need a configuration
        if (configuration == null)
            configuration = new XmlStreamConfiguration();
        
        // TODO FPD add more validation

        NaaccrDictionary baseDictionary = NaaccrDictionaryUtils.getBaseDictionaryByUri(rootData.getBaseDictionaryUri());
        final RuntimeNaaccrDictionary runtimeDictionary = new RuntimeNaaccrDictionary(rootData.getRecordType(), baseDictionary, userDictionary);
        
        // by default, XStream will write the root tag, but there is no easy way to add attributes to it; this is what this code does...
        PrettyPrintWriter prettyWriter = new PrettyPrintWriter(writer) {
            @Override
            public void startNode(String name) {
                // TODO FPD use the rootData, not the dictionary!
                super.startNode(name);
                if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(name) && runtimeDictionary != null) {
                    addAttribute("baseDictionaryUri", runtimeDictionary.getBaseDictionaryUri());
                    addAttribute("userDictionaryUri", runtimeDictionary.getUserDictionaryUri()); // TODO FDP we shouldn't write it if this is the default one...
                    addAttribute("recordType", runtimeDictionary.getRecordType());
                    addAttribute("timeGenerated", new SimpleDateFormat(NaaccrXmlUtils.GENERATED_TIME_FORMAT).format(new Date()));
                }
            }
        };
        
        // TODO FPD don't use an object stream, not flexible enough; use a writer...
        _oos = configuration.getXstream().createObjectOutputStream(prettyWriter, "NaaccrDataExchange");
        
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
}
