/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.runtime.NaaccrStreamConfiguration;
import org.naaccr.xml.runtime.NaaccrStreamContext;
import org.naaccr.xml.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

/**
 * This class can be used to wrap a generic writer into a patient writer handling the NAACCR XML format.
 */
public class PatientXmlWriter implements AutoCloseable {

    protected HierarchicalStreamWriter _writer;

    protected XStream _xstream;

    public PatientXmlWriter(Writer writer, NaaccrData rootData) throws NaaccrIOException {
        this(writer, rootData, null, null, null);
    }

    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrOptions options) throws NaaccrIOException {
        this(writer, rootData, options, null, null);
    }

    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrOptions options, NaaccrDictionary userDictionary) throws NaaccrIOException {
        this(writer, rootData, options, userDictionary, null);
    }

    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrStreamConfiguration configuration) throws NaaccrIOException {

        try {
            // we always need options
            if (options == null)
                options = new NaaccrOptions();

            // we always need a configuration
            if (configuration == null)
                configuration = new NaaccrStreamConfiguration();

            NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByUri(rootData.getBaseDictionaryUri());

            // create the writer
            _writer = new PrettyPrintWriter(writer, new char[] {' ', ' ', ' ', ' '});

            // would be better to use a "header writer", I think XStream has one actually; that would be better...
            try {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + System.getProperty("line.separator") + System.getProperty("line.separator"));
            }
            catch (IOException e) {
                throw new NaaccrIOException(e.getMessage());
            }

            // write standard attributes
            _writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT);
            if (rootData.getBaseDictionaryUri() == null)
                throw new NaaccrIOException("base dictionary URI is required");
            _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT, rootData.getBaseDictionaryUri());
            if (rootData.getUserDictionaryUri() != null)
                _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT, rootData.getUserDictionaryUri());
            if (rootData.getRecordType() == null)
                throw new NaaccrIOException("record type is required");
            _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE, rootData.getRecordType());
            if (rootData.getTimeGenerated() == null)
                throw new NaaccrIOException("time generated is required");
            Calendar cal = Calendar.getInstance();
            cal.setTime(rootData.getTimeGenerated());
            _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED, DatatypeConverter.printDateTime(cal));

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
            for (Item item : rootData.getItems())
                configuration.getPatientConverter().writeItem(item, _writer);

            // for now, ignore the root extension...

            // need to expose xstream so the other methods can use it...
            _xstream = configuration.getXstream();
        }
        catch (ConversionException ex) {
            throw convertSyntaxException(ex);
        }
    }

    public void writePatient(Patient patient) throws NaaccrIOException {
        try {
            _xstream.marshal(patient, _writer);
        }
        catch (ConversionException ex) {
            throw convertSyntaxException(ex);
        }
    }

    @Override
    public void close() {
        _writer.endNode();
        _writer.close();
    }

    /**
     * We don't want to expose the conversion exceptions, so let's translate them into our own exception...
     */
    private NaaccrIOException convertSyntaxException(ConversionException ex) {
        String msg = ex.get("message");
        if (msg == null)
            msg = ex.getMessage();
        NaaccrIOException e = new NaaccrIOException(msg);
        if (ex.get("lineNumber") != null)
            e.setLineNumber(Integer.valueOf(ex.get("lineNumber")));
        e.setPath(ex.get("path"));
        return e;
    }
}
