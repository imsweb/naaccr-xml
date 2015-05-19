/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.runtime.NaaccrStreamConfiguration;
import org.naaccr.xml.runtime.NaaccrStreamContext;
import org.naaccr.xml.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * This class can be used to wrap a generic reader into a patient reader handling the NAACCR XML format.
 */
public class PatientXmlReader implements AutoCloseable {

    protected NaaccrData _rootData;

    protected HierarchicalStreamReader _reader;

    protected XStream _xstream;

    protected NaaccrStreamContext _context;

    public PatientXmlReader(Reader reader) throws NaaccrIOException {
        this(reader, null, null, null);
    }

    public PatientXmlReader(Reader reader, NaaccrOptions options) throws NaaccrIOException {
        this(reader, options, null, null);
    }

    public PatientXmlReader(Reader reader, NaaccrOptions options, NaaccrDictionary userDictionary) throws NaaccrIOException {
        this(reader, options, userDictionary, null);
    }

    public PatientXmlReader(Reader reader, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrStreamConfiguration configuration) throws NaaccrIOException {

        try {
            // we always need options
            if (options == null)
                options = new NaaccrOptions();

            // we always need a configuration
            if (configuration == null)
                configuration = new NaaccrStreamConfiguration();

            // create the XML reader
            _reader = configuration.getDriver().createReader(reader);
            if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT))
                throw new NaaccrIOException("was expecting " + NaaccrXmlUtils.NAACCR_XML_TAG_ROOT + " root tag but got " + _reader.getNodeName(), configuration.getParser().getLineNumber());

            // create the root data holder (it will be use for every field except the list of patients)
            _rootData = createRootData();

            // read the standard attribute: base dictionary
            _rootData.setBaseDictionaryUri(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT));
            if (_rootData.getBaseDictionaryUri() == null)
                throw new NaaccrIOException("the \"" + NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT + "\" attribute is required", configuration.getParser().getLineNumber());
            String version = NaaccrXmlDictionaryUtils.extractVersionFromUri(_rootData.getBaseDictionaryUri());
            if (version == null || version.trim().isEmpty())
                throw new NaaccrIOException("unable to extract NAACCR version from base dictionary URI \"" + _rootData.getBaseDictionaryUri() + "\"", configuration.getParser().getLineNumber());
            if (!NaaccrFormat.isVersionSupported(version))
                throw new NaaccrIOException("invalid/unsupported NAACCR version: " + version, configuration.getParser().getLineNumber());
            NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(version);

            // read the standard attribute: user dictionary
            _rootData.setUserDictionaryUri(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT));
            if (_rootData.getUserDictionaryUri() != null && (userDictionary == null || !_rootData.getUserDictionaryUri().equals(userDictionary.getDictionaryUri())))
                throw new NaaccrIOException("unknown/invalid user dictionary: " + _rootData.getUserDictionaryUri(), configuration.getParser().getLineNumber());

            // read the standard attribute: record type            
            _rootData.setRecordType(_reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE));
            if (_rootData.getRecordType() == null || _rootData.getRecordType().trim().isEmpty())
                throw new NaaccrIOException("the \"" + NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE + "\" attribute is required", configuration.getParser().getLineNumber());
            if (!NaaccrFormat.isRecordTypeSupported(_rootData.getRecordType()))
                throw new NaaccrIOException("invalid record type: " + _rootData.getRecordType(), configuration.getParser().getLineNumber());

            // read the standard attribute: time generated
            String generatedTime = _reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED);
            if (generatedTime != null) {
                try {
                    _rootData.setTimeGenerated(DatatypeConverter.parseDateTime(generatedTime).getTime());
                } catch (IllegalArgumentException e) {
                    throw new NaaccrIOException("invalid time generated value: " + generatedTime, configuration.getParser().getLineNumber());
                }
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

            // now we are ready to create our reading context and make it available to the patient converter
            _context = new NaaccrStreamContext();
            _context.setDictionary(new RuntimeNaaccrDictionary(_rootData.getRecordType(), baseDictionary, userDictionary));
            _context.setOptions(options);
            _context.setParser(configuration.getParser());
            configuration.getPatientConverter().setContext(_context);

            // handle the case where no patients nor items are provided
            if (!_reader.hasMoreChildren())
                return;
            _reader.moveDown();

            // read the root items
            Set<String> itemsAlreadySeen = new HashSet<>();
            while (_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM)) {
                String rawId = _reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID);
                String rawNum = _reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM);
                configuration.getPatientConverter().readItem(_rootData, "/NaaccrData", NaaccrXmlUtils.NAACCR_XML_TAG_ROOT, rawId, rawNum, _reader.getValue());
                if (rawId != null && itemsAlreadySeen.contains(rawId))
                    throw new NaaccrIOException("item '" + rawId + "' should be unique within the \"" + NaaccrXmlUtils.NAACCR_XML_TAG_ROOT + "\" tags");
                else
                    itemsAlreadySeen.add(rawId);
                _reader.moveUp();
                if (_reader.hasMoreChildren())
                    _reader.moveDown();
            }

            // if we are back at the root level, there is no more children, and we are done
            if (_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT))
                return;

            // for now, ignore the root extension...
            if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT)) {
                _reader.moveUp();
                if (_reader.hasMoreChildren())
                    _reader.moveDown();
            }

            // if we are back at the root level, there is no more children, and we are done
            if (_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT))
                return;

            // at this point, either we are done (and the method already return) or there should be a patient tag
            if (!_reader.getNodeName().equals(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT))
                throw new NaaccrIOException("unexpected tag: " + _reader.getNodeName(), configuration.getParser().getLineNumber());

            // need to expose xstream so the other methods can use it...
            _xstream = configuration.getXstream();
        }
        catch (ConversionException ex) {
            throw convertSyntaxException(ex);
        }
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
            throw convertSyntaxException(ex);
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

    /**
     * Creates the root object.
     * @return the new root object, never null
     */
    protected NaaccrData createRootData() {
        return new NaaccrData();
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
