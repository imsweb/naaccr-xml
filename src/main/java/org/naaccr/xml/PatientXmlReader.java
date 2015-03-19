/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public class PatientXmlReader implements AutoCloseable {

    protected NaaccrData _rootData;

    protected HierarchicalStreamReader _reader;

    protected XStream _xstream;

    protected NaaccrStreamContext _context;

    public PatientXmlReader(Reader reader) throws NaaccrIOException {
        this(reader, null, null, null);
    }

    public PatientXmlReader(Reader reader, NaaccrXmlOptions options) throws NaaccrIOException {
        this(reader, options, null, null);
    }

    public PatientXmlReader(Reader reader, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws NaaccrIOException {
        this(reader, options, userDictionary, null);
    }

    public PatientXmlReader(Reader reader, NaaccrXmlOptions options, NaaccrDictionary userDictionary, NaaccrStreamConfiguration configuration) throws NaaccrIOException {

        // we always need options
        if (options == null)
            options = new NaaccrXmlOptions();

        // we always need a configuration
        if (configuration == null)
            configuration = new NaaccrStreamConfiguration();

        // create the XML reader
        _reader = configuration.getDriver().createReader(reader);
        if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT))
            throw new NaaccrIOException("Was expecting " + NaaccrXmlUtils.NAACCR_XML_TAG_ROOT + " root tag but got " + _reader.getNodeName(), configuration.getParser().getLineNumber());

        // create the root data holder (it will be use for every fields except the patients)
        _rootData = new NaaccrData(); // TODO FPD what if the root object has been extended?

        // read the standard attribute: base dictionary
        _rootData.setBaseDictionaryUri(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT));
        if (_rootData.getBaseDictionaryUri() == null)
            throw new NaaccrIOException("The " + NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT + " attribute is required", configuration.getParser().getLineNumber());
        NaaccrDictionary baseDictionary = NaaccrDictionaryUtils.getBaseDictionaryByUri(_rootData.getBaseDictionaryUri());
        if (baseDictionary == null)
            throw new NaaccrIOException("Unknown base dictionary: " + _rootData.getBaseDictionaryUri(), configuration.getParser().getLineNumber());

        // read the standard attribute: user dictionary
        _rootData.setUserDictionaryUri(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT));
        if (_rootData.getUserDictionaryUri() != null && (userDictionary == null || !_rootData.getUserDictionaryUri().equals(userDictionary.getDictionaryUri())))
            throw new NaaccrIOException("Unknown user dictionary: " + _rootData.getUserDictionaryUri(), configuration.getParser().getLineNumber());

        // read the standard attribute: record type            
        _rootData.setRecordType(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE));
        if (!NaaccrFormat.isRecordTypeSupported(_rootData.getRecordType()))
            throw new NaaccrIOException("Invalid record type: " + _rootData.getRecordType(), configuration.getParser().getLineNumber());

        // read the standard attribute: time generated
        try {
            _rootData.setTimeGenerated(new SimpleDateFormat(NaaccrXmlUtils.GENERATED_TIME_FORMAT).parse(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED)));
        }
        catch (ParseException e) {
            throw new NaaccrIOException("Bad format for " + NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED + " attributes (expects " + NaaccrXmlUtils.GENERATED_TIME_FORMAT + ")",
                    configuration.getParser().getLineNumber());
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
        _context = new NaaccrStreamContext();
        _context.setDictionary(new RuntimeNaaccrDictionary(_rootData.getRecordType(), baseDictionary, userDictionary));
        _context.setOptions(options);
        _context.setParser(configuration.getParser());
        configuration.getPatientConverter().setContext(_context);

        // check order of the tags
        if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM) && !_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT))
            throw new NaaccrIOException("Unexpected tag: " + _reader.getNodeName(), configuration.getParser().getLineNumber());

        // read the root items
        while (_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM)) {
            String rawId = _reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID);
            String rawNum = _reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM);
            configuration.getPatientConverter().readItem(_rootData, "/NaaccrData", NaaccrXmlUtils.NAACCR_XML_TAG_ROOT, rawId, rawNum, _reader.getValue());
            _reader.moveUp();
            _reader.moveDown();
        }

        // for now, ignore the root extension...
        if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT)) {
            _reader.moveDown();
            _reader.moveUp();
        }

        if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT))
            throw new NaaccrIOException("Unexpected tag: " + _reader.getNodeName(), configuration.getParser().getLineNumber());

        // need to expose xstream so the other methods can use it...
        _xstream = configuration.getXstream();
    }

    /**
     * Reads the next patient on this stream.
     * @return the next available patient, null if not such patient
     * @throws NaaccrIOException
     */
    public Patient readPatient() throws NaaccrIOException {
        if (_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT))
            return null;

        if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT))
            throw new NaaccrIOException("Unexpected tag: " + _reader.getNodeName(), _context.getParser().getLineNumber());

        Patient patient;
        try {
            patient = (Patient)_xstream.unmarshal(_reader);
            _reader.moveUp();
            if (_reader.hasMoreChildren())
                _reader.moveDown();
        }
        catch (ConversionException ex) {
            String msg = ex.get("message");
            if (msg == null)
                msg = ex.getMessage();
            NaaccrIOException e = new NaaccrIOException(msg);
            e.setLineNumber(Integer.valueOf(ex.get("lineNumber")));
            e.setPath(ex.get("path"));
            throw e;
        }

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
    public void close() {
        _reader.moveUp();
        _reader.close();
    }
}
