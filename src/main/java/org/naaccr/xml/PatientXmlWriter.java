/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class PatientXmlWriter implements AutoCloseable {

    protected HierarchicalStreamWriter _writer;

    protected XStream _xstream;

    public PatientXmlWriter(Writer writer, NaaccrData rootData) throws IOException {
        this(writer, rootData, null, null, null);
    }

    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrXmlOptions options) throws IOException {
        this(writer, rootData, options, null, null);
    }

    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws IOException {
        this(writer, rootData, options, userDictionary, null);
    }
    
    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrXmlOptions options, NaaccrDictionary userDictionary, NaaccrStreamConfiguration configuration) throws IOException {

        // we always need options
        if (options == null)
            options = new NaaccrXmlOptions();

        // we always need a configuration
        if (configuration == null)
            configuration = new NaaccrStreamConfiguration();

        NaaccrDictionary baseDictionary = NaaccrDictionaryUtils.getBaseDictionaryByUri(rootData.getBaseDictionaryUri());

        // create the writer
        _writer = new PrettyPrintWriter(writer, new char[] {' ', ' ', ' ', ' '});

        // write the header // TODO FPD look into a header writer, I think there is a class that does that already...
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + System.getProperty("line.separator") + System.getProperty("line.separator"));

        // write standard attributes
        _writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT);
        _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT, rootData.getBaseDictionaryUri());
        if (rootData.getUserDictionaryUri() != null)
            _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT, rootData.getUserDictionaryUri());
        _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE, rootData.getRecordType());
        _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED, new SimpleDateFormat(NaaccrXmlUtils.GENERATED_TIME_FORMAT).format(rootData.getTimeGenerated()));

        // write non-standard attributes
        Set<String> standardAttributes = new HashSet<>();
        standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT);
        standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT);
        standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE);
        standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED);
        for (Entry<String, String> entry : rootData.getExtraRootParameters().entrySet())
                if (!standardAttributes.contains(entry.getKey()))
                    _writer.addAttribute(entry.getKey(), entry.getValue());

        // now we are ready to create our reading context and make it available to the patient converter
        NaaccrStreamContext context = new NaaccrStreamContext();
        context.setDictionary(new RuntimeNaaccrDictionary(rootData.getRecordType(), baseDictionary, userDictionary));
        context.setOptions(options);
        context.setParser(configuration.getParser());
        configuration.getPatientConverter().setContext(context);

        // write the root items
        for (Item item : rootData.getItems()) {
            if (!options.getItemsToExclude().contains(item.getNaaccrId())) {
                // TODO FPD, wouldn't it be better to define an item converter? But then I want to share it with the patient converter!
                _writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM);
                _writer.addAttribute("naaccrId", item.getNaaccrId());
                if (item.getNaaccrNum() != null)
                    _writer.addAttribute("naaccrNum", item.getNaaccrNum().toString()); // TODO FPD need to use the options to know if the num needs to be written...
                if (item.getValue() != null)
                    _writer.setValue(item.getValue());
                _writer.endNode();
            }
        }

        // for now, ignore the root extension...

        // need to expose xstream so the other methods can use it...
        _xstream = configuration.getXstream();
    }

    public void writePatient(Patient patient) throws IOException {
        _xstream.marshal(patient, _writer);
    }

    @Override
    public void close() throws IOException {
        _writer.endNode();
        _writer.close();
    }
}
