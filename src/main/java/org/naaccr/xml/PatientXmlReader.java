/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public class PatientXmlReader implements AutoCloseable {

    protected NaaccrData _rootData;

    protected HierarchicalStreamReader _reader;

    protected XStream _xstream;

    public PatientXmlReader(Reader reader) throws IOException {
        this(reader, null, null, null);
    }

    public PatientXmlReader(Reader reader, NaaccrXmlOptions options) throws IOException {
        this(reader, options, null, null);
    }

    public PatientXmlReader(Reader reader, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws IOException {
        this(reader, options, userDictionary, null);
    }

    public PatientXmlReader(Reader reader, NaaccrXmlOptions options, NaaccrDictionary userDictionary, NaaccrStreamConfiguration configuration) throws IOException {

        // we always need options
        if (options == null)
            options = new NaaccrXmlOptions();

        // we always need a configuration
        if (configuration == null)
            configuration = new NaaccrStreamConfiguration();

        // create the XML reader
        _reader = configuration.getDriver().createReader(reader);
        if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT))
            throw new IOException("Was expecting " + NaaccrXmlUtils.NAACCR_XML_TAG_ROOT + " root tag but got " + _reader.getNodeName());

        // create the root data holder (it will be use for every fields except the patients)
        _rootData = new NaaccrData(); // TODO FPD what if the root object has been extended?

        // read the standard attribute: base dictionary
        _rootData.setBaseDictionaryUri(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT));
        if (_rootData.getBaseDictionaryUri() == null)
            throw new IOException("The " + NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT + " attribute is required");
        NaaccrDictionary baseDictionary = NaaccrDictionaryUtils.getBaseDictionaryByUri(_rootData.getBaseDictionaryUri());
        if (baseDictionary == null)
            throw new IOException("Unknown base dictionary: " + _rootData.getBaseDictionaryUri());

        // read the standard attribute: user dictionary
        _rootData.setUserDictionaryUri(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT));
        if (_rootData.getUserDictionaryUri() != null && (userDictionary == null || !_rootData.getUserDictionaryUri().equals(userDictionary.getDictionaryUri())))
            throw new IOException("Unknown user dictionary: " + _rootData.getUserDictionaryUri());

        // read the standard attribute: record type            
        _rootData.setRecordType(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE));
        if (!NaaccrFormat.isRecordTypeSupported(_rootData.getRecordType()))
            throw new IOException("Invalid record type: " + _rootData.getRecordType());

        // read the standard attribute: time generated
        try {
            _rootData.setTimeGenerated(new SimpleDateFormat(NaaccrXmlUtils.GENERATED_TIME_FORMAT).parse(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED)));
        }
        catch (ParseException e) {
            throw new IOException("Bad format for " + NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED + " attributes (expects " + NaaccrXmlUtils.GENERATED_TIME_FORMAT + ")");
        }

        // read the non-standard attributes
        Set<String> standardAttributes = new HashSet<>();
        standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT);
        standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT);
        standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE);
        standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED);
        for (int i = 0; i < _reader.getAttributeCount(); i++)
            if (!standardAttributes.contains(_reader.getAttributeName(i)))
                _rootData.getExtraRootParameters().put(_reader.getAttributeName(i), _reader.getAttribute(i));
        _reader.moveDown();

        // now we are ready to create our reading context and make it available to the patient converter
        NaaccrStreamContext context = new NaaccrStreamContext();
        context.setDictionary(new RuntimeNaaccrDictionary(_rootData.getRecordType(), baseDictionary, userDictionary));
        context.setOptions(options);
        context.setParser(configuration.getParser());
        configuration.getPatientConverter().setContext(context);

        // read the root items
        while (_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM)) {
            // TODO FPD, wouldn't it be better to define an item converter? But then I want to share it with the patient converter!
            Item item = new Item();
            item.setId(_reader.getAttribute("naaccrId"));
            if (_reader.getAttribute("naaccrNum") != null)
                item.setNum(Integer.valueOf(_reader.getAttribute("naaccrNum")));
            item.setValue(_reader.getValue());
            _rootData.getItems().add(item);
            _reader.moveUp();
            _reader.moveDown();
        }

        // for now, ignore the root extension...
        if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT)) {
            _reader.moveDown();
            _reader.moveUp();
        }

        if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT))
            throw new IOException("Was expecting " + NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT + " repeating tag but got " + _reader.getNodeName());

        // need to expose xstream so the other methods can use it...
        _xstream = configuration.getXstream();
    }

    /**
     * Reads the next patient on this stream.
     * @return the next available patient, null if not such patient
     * @throws IOException
     * @throws NaaccrValidationException
     */
    public Patient readPatient() throws IOException, NaaccrValidationException {
        Patient patient = null;
        if (_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT)) {
            // TODO FPD deal with the conversion exception, create a naaccrValidationException from it...
            patient = (Patient)_xstream.unmarshal(_reader);
            _reader.moveUp();
            if (_reader.hasMoreChildren())
                _reader.moveDown();
        }
        else if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT))
            throw new IOException("Expected " + NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT + " tag but found " + _reader.getNodeName());
        return patient;
    }

    /**
     * Returns the "root" data; it includes root attributes and the root items.
     * @return the root data, never null
     */
    public NaaccrData getRootData() {
        return _rootData;
    }

    @Override
    public void close() throws IOException {
        _reader.moveUp();
        _reader.close();
    }
}
