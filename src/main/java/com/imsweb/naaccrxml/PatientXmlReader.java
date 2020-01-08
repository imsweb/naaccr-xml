/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.Reader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;
import com.imsweb.naaccrxml.runtime.NaaccrStreamContext;
import com.imsweb.naaccrxml.runtime.RuntimeNaaccrDictionary;

import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_NAMESPACE;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_SPEC_VERSION;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_TAG_ITEM;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT;
import static com.imsweb.naaccrxml.NaaccrXmlUtils.NAACCR_XML_TAG_ROOT;

/**
 * This class can be used to wrap a generic reader into a patient reader handling the NAACCR XML format.
 */
public class PatientXmlReader implements PatientReader {

    // the root data
    protected NaaccrData _rootData;

    // XStream object responsible for reading patient objects
    protected XStream _xstream;

    // underlined reader
    protected HierarchicalStreamReader _reader;

    // context for this reader (some stuff got a bit convoluted and using a context made them a cleaner)
    protected NaaccrStreamContext _context;

    // sometimes we want to finalize the reading operation without closing the reader itself...
    protected boolean _hasBeenFinalized = false;

    /**
     * Constructor.
     * @param reader required underlined reader
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlReader(Reader reader) throws NaaccrIOException {
        this(reader, null, (NaaccrDictionary)null, null);
    }

    /**
     * Constructor.
     * @param reader required underlined reader
     * @param options optional options
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlReader(Reader reader, NaaccrOptions options) throws NaaccrIOException {
        this(reader, options, (NaaccrDictionary)null, null);
    }

    /**
     * Constructor.
     * @param reader required underlined reader
     * @param options optional options
     * @param userDictionary optional user-defined dictionary
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlReader(Reader reader, NaaccrOptions options, NaaccrDictionary userDictionary) throws NaaccrIOException {
        this(reader, options, Collections.singletonList(userDictionary), null);
    }

    /**
     * Constructor.
     * @param reader required underlined reader
     * @param options optional options
     * @param userDictionaries optional user-defined dictionaries (can be null or empty)
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlReader(Reader reader, NaaccrOptions options, List<NaaccrDictionary> userDictionaries) throws NaaccrIOException {
        this(reader, options, userDictionaries, null);
    }

    /**
     * Constructor.
     * @param reader required underlined reader
     * @param options optional options
     * @param userDictionary optional user-defined dictionary
     * @param conf optional stream configuration
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlReader(Reader reader, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrStreamConfiguration conf) throws NaaccrIOException {
        this(reader, options, Collections.singletonList(userDictionary), conf);
    }

    /**
     * Constructor.
     * @param reader required underlined reader
     * @param options optional options
     * @param userDictionaries optional user-defined dictionaries (can be null or empty)
     * @param conf optional stream configuration
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlReader(Reader reader, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrStreamConfiguration conf) throws NaaccrIOException {

        try {
            // we always need options
            if (options == null)
                options = NaaccrOptions.getDefault();

            // we always need a configuration
            if (conf == null)
                conf = NaaccrStreamConfiguration.getDefault();

            // clean-up the dictionaries
            Map<String, NaaccrDictionary> dictionaries = new HashMap<>();
            if (userDictionaries != null)
                for (NaaccrDictionary userDictionary : userDictionaries)
                    if (userDictionary != null)
                        dictionaries.put(userDictionary.getDictionaryUri(), userDictionary);

            // create the context
            _context = new NaaccrStreamContext();
            _context.setOptions(options);
            _context.setConfiguration(conf);

            // create the XML reader
            _reader = conf.getDriver().createReader(reader);
            if (!isRootTag(_reader.getNodeName()))
                throw new NaaccrIOException("was expecting " + NAACCR_XML_TAG_ROOT + " root tag but got " + _reader.getNodeName(), conf.getParser().getLineNumber());

            // create the root data holder (it will be use for every field except the list of patients)
            _rootData = createRootData();

            // line number (we can only populate the start line number for the root since we can't read the entire file to know where it ends)
            _rootData.setStartLineNumber(conf.getParser().getLineNumber());
            _rootData.setEndLineNumber(null);

            // read the standard attribute: specification version (we do it first because the format of other attributes can depend on the specs version)
            String specVersion = _reader.getAttribute(NAACCR_XML_ROOT_ATT_SPEC_VERSION);
            if (specVersion == null)
                specVersion = SpecificationVersion.SPEC_1_0;
            if (!SpecificationVersion.isSpecificationSupported(specVersion))
                throw new NaaccrIOException("invalid specification version: " + specVersion);
            _rootData.setSpecificationVersion(specVersion);

            // read the standard attribute: base dictionary
            _rootData.setBaseDictionaryUri(_reader.getAttribute(NAACCR_XML_ROOT_ATT_BASE_DICT));
            if (_rootData.getBaseDictionaryUri() == null)
                throw new NaaccrIOException("the \"" + NAACCR_XML_ROOT_ATT_BASE_DICT + "\" attribute is required", conf.getParser().getLineNumber());
            String version = NaaccrXmlDictionaryUtils.extractVersionFromUri(_rootData.getBaseDictionaryUri());
            if (version == null || version.trim().isEmpty())
                throw new NaaccrIOException("unable to extract NAACCR version from base dictionary URI \"" + _rootData.getBaseDictionaryUri() + "\"", conf.getParser().getLineNumber());
            if (!NaaccrFormat.isVersionSupported(version))
                throw new NaaccrIOException("invalid/unsupported NAACCR version: " + version, conf.getParser().getLineNumber());
            NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(version);

            // read the standard attribute: user dictionaries
            if (!StringUtils.isBlank(_reader.getAttribute(NAACCR_XML_ROOT_ATT_USER_DICT))) {
                List<String> dataUserDictionaries = Arrays.asList(StringUtils.split(_reader.getAttribute(NAACCR_XML_ROOT_ATT_USER_DICT), ' '));
                if (SpecificationVersion.compareSpecifications(specVersion, SpecificationVersion.SPEC_1_2) < 0 && dataUserDictionaries.size() > 1)
                    throw new NaaccrIOException("multiple user dictionaries can only be provided under specification 1.2+", conf.getParser().getLineNumber());
                _rootData.setUserDictionaryUri(dataUserDictionaries);
            }
            // let's use only the dictionaries that are referenced in the data file (more can be provided to library, that's OK; it's also OK if some are missing in the library)
            for (String uri : new HashSet<>(dictionaries.keySet()))
                if (!_rootData.getUserDictionaryUri().contains(uri))
                    dictionaries.remove(uri);

            // read the standard attribute: record type            
            _rootData.setRecordType(_reader.getAttribute(NAACCR_XML_ROOT_ATT_REC_TYPE));
            if (_rootData.getRecordType() == null || _rootData.getRecordType().trim().isEmpty())
                throw new NaaccrIOException("the \"" + NAACCR_XML_ROOT_ATT_REC_TYPE + "\" attribute is required", conf.getParser().getLineNumber());
            if (!NaaccrFormat.isRecordTypeSupported(_rootData.getRecordType()))
                throw new NaaccrIOException("invalid record type: " + _rootData.getRecordType(), conf.getParser().getLineNumber());

            // read the standard attribute: time generated
            String generatedTime = _reader.getAttribute(NAACCR_XML_ROOT_ATT_TIME_GENERATED);
            if (generatedTime != null) {
                // it sucks that ISO 8601 allows with and without a time zone (offset) and it sucks even more that Java doesn't have a single formatter to handle both!
                try {
                    _rootData.setTimeGenerated(Date.from(ZonedDateTime.parse(generatedTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()));
                }
                catch (RuntimeException e1) {
                    try {
                        _rootData.setTimeGenerated(Date.from(LocalDateTime.parse(generatedTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC)));
                    }
                    catch (RuntimeException e2) {
                        _rootData.addValidationError(new NaaccrValidationError(NaaccrErrorUtils.CODE_BAD_TIME_GENERATED, generatedTime));
                    }
                }
            }

            // read the non-standard attributes
            Set<String> standardAttributes = new HashSet<>();
            standardAttributes.add(NAACCR_XML_ROOT_ATT_BASE_DICT);
            standardAttributes.add(NAACCR_XML_ROOT_ATT_USER_DICT);
            standardAttributes.add(NAACCR_XML_ROOT_ATT_REC_TYPE);
            standardAttributes.add(NAACCR_XML_ROOT_ATT_TIME_GENERATED);
            standardAttributes.add(NAACCR_XML_ROOT_ATT_SPEC_VERSION);
            Map<String, String> attributeValues = new HashMap<>(), namespaces = new HashMap<>();
            for (int i = 0; i < _reader.getAttributeCount(); i++) {
                String attrName = _reader.getAttributeName(i);
                if (standardAttributes.contains(attrName))
                    continue;
                if (attrName.startsWith("xmlns")) {
                    int idx = attrName.indexOf(':');
                    if (idx != -1) {
                        String namespacePrefix = attrName.substring(idx + 1);
                        if (options.getUseStrictNamespaces() && !conf.getRegisterNamespaces().containsKey(namespacePrefix))
                            throw new NaaccrIOException("namespace " + _reader.getAttribute(i) + " (prefix=" + namespacePrefix + ") has not been defined in the configuration");
                        namespaces.put(namespacePrefix, _reader.getAttribute(i));
                    }
                    else {
                        // the only default namespace allowed is the NAACCR one
                        if (!NAACCR_XML_NAMESPACE.equals(_reader.getAttribute(i)))
                            throw new NaaccrIOException("default namespace can only be set to " + NAACCR_XML_NAMESPACE);
                        namespaces.put("", _reader.getAttribute(i));
                    }
                }
                else
                    attributeValues.put(attrName, _reader.getAttribute(i));
            }

            // in strict namespace mode, the NAACCR namespace must be defined (either as the default namespace or as a prefixed namespace, that doesn't matter)
            if (options.getUseStrictNamespaces() && !namespaces.containsValue(NAACCR_XML_NAMESPACE))
                throw new NaaccrIOException("namespace " + NAACCR_XML_NAMESPACE + " must be defined in the root attributes");

            // in strict namespace mode, any non-standard attribute must be prefixed by a defined namespace
            for (Map.Entry<String, String> entry : attributeValues.entrySet()) {
                String prefix = null, attrName;
                int idx = entry.getKey().indexOf(':');
                if (idx != -1) {
                    prefix = entry.getKey().substring(0, idx);
                    attrName = entry.getKey().substring(idx + 1);
                }
                else
                    attrName = entry.getKey();

                if (options.getUseStrictNamespaces()) {
                    if (prefix == null)
                        throw new NaaccrIOException("attribute " + attrName + " must use a namespace prefix");
                    if (!namespaces.containsKey(prefix))
                        throw new NaaccrIOException("attribute " + attrName + " uses a prefix " + prefix + " that is not properly defined");
                }
                _rootData.addExtraRootParameters(attrName, entry.getValue());
            }

            // create or get the runtime dictionary
            if (conf.getCachedDictionary() == null || !conf.getCachedDictionary().getId().equals(RuntimeNaaccrDictionary.computeId(_rootData.getRecordType(), baseDictionary, dictionaries.values())))
                conf.setCachedDictionary(new RuntimeNaaccrDictionary(_rootData.getRecordType(), baseDictionary, dictionaries.values()));

            // now we are ready to setup our reading context and make it available to the patient converter
            _context.setDictionary(conf.getCachedDictionary());
            conf.getPatientConverter().setContext(_context);

            // handle the case where no patients nor items are provided
            if (!_reader.hasMoreChildren())
                return;
            _reader.moveDown();

            // read the root items
            Set<String> itemsAlreadySeen = new HashSet<>();
            while (isItemTag(_reader.getNodeName())) {
                String rawId = _reader.getAttribute(NAACCR_XML_ITEM_ATT_ID);
                String rawNum = _reader.getAttribute(NAACCR_XML_ITEM_ATT_NUM);
                if (rawId != null && itemsAlreadySeen.contains(rawId))
                    throw new NaaccrIOException("item '" + rawId + "' should be unique within the \"" + NAACCR_XML_TAG_ROOT + "\" tags");
                else
                    itemsAlreadySeen.add(rawId);
                // following call will ensure that proper validation runs
                conf.getPatientConverter().readItem(_rootData, "/NaaccrData", NAACCR_XML_TAG_ROOT, rawId, rawNum, _reader.getValue());
                _reader.moveUp();
                if (_reader.hasMoreChildren())
                    _reader.moveDown();
            }

            // handle root extensions
            while (!isPatientTag(_reader.getNodeName()) && !isRootTag(_reader.getNodeName())) {
                if (!Boolean.TRUE.equals(options.getIgnoreExtensions()))
                    _rootData.addExtension(conf.getXstream().unmarshal(_reader));
                _reader.moveUp();
                if (_reader.hasMoreChildren())
                    _reader.moveDown();
            }

            // if we are back at the root level, there is no more children, and we are done
            if (isRootTag(_reader.getNodeName()))
                return;

            // at this point, either we are done (and the method already return) or there should be a patient tag
            if (!isPatientTag(_reader.getNodeName()))
                throw new NaaccrIOException("unexpected tag: " + _context.extractTag(_reader.getNodeName()), conf.getParser().getLineNumber());

            // need to expose xstream so the other methods can use it...
            _xstream = conf.getXstream();
        }
        catch (ConversionException ex) {
            throw convertSyntaxException(ex);
        }
        catch (StreamException ex) {
            throw new NaaccrIOException("invalid XML syntax, unable to find root tag", ex);
        }
        catch (RuntimeException ex) {
            // an unknown tag in the extension is a common mistake, so let's make sure we report that nicely
            if (ex instanceof CannotResolveClassException)
                throw new NaaccrIOException("invalid tag: " + ex.getMessage());
            throw new NaaccrIOException("invalid XML syntax", ex);
        }
    }

    private boolean isRootTag(String tag) throws NaaccrIOException {
        return _context.extractTag(tag).equals(NAACCR_XML_TAG_ROOT);
    }

    private boolean isPatientTag(String tag) throws NaaccrIOException {
        return _context.extractTag(tag).equals(NAACCR_XML_TAG_PATIENT);
    }

    private boolean isItemTag(String tag) throws NaaccrIOException {
        return _context.extractTag(tag).equals(NAACCR_XML_TAG_ITEM);
    }

    @Override
    public Patient readPatient() throws NaaccrIOException {
        if (_context.extractTag(_reader.getNodeName()).equals(NAACCR_XML_TAG_ROOT))
            return null;

        if (!_context.extractTag(_reader.getNodeName()).equals(NAACCR_XML_TAG_PATIENT))
            throw new NaaccrIOException("Unexpected tag: " + _reader.getNodeName(), _context.getLineNumber());

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
        catch (StreamException ex) {
            throw new NaaccrIOException("invalid XML syntax", ex);
        }
        catch (RuntimeException ex) {
            // an unknown tag in the extension is a common mistake, so let's make sure we report that nicely
            if (ex instanceof CannotResolveClassException)
                throw new NaaccrIOException("invalid tag: " + ex.getMessage());
            throw new NaaccrIOException("invalid XML syntax", ex);
        }

        return patient;
    }

    @Override
    public NaaccrData getRootData() {
        return _rootData;
    }

    @Override
    public void closeAndKeepAlive() {
        if (!_hasBeenFinalized) {
            _reader.moveUp();
            _hasBeenFinalized = true;
        }
    }

    @Override
    public void close() {
        closeAndKeepAlive();
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

        // reading a file that has bad tags is very common, so let's try to get a better error message in that case:
        if (CannotResolveClassException.class.getName().equals(ex.get("cause-exception")))
            msg = "invalid tag: " + ex.get("cause-message");
        else if (StreamException.class.getName().equals(ex.get("cause-exception")))
            msg = "invalid XML syntax";

        if (msg == null)
            msg = ex.getMessage();

        NaaccrIOException e = new NaaccrIOException(msg, ex);
        if (ex.get("line number") != null)
            e.setLineNumber(Integer.valueOf(ex.get("line number")));
        e.setPath(ex.get("path"));
        return e;
    }
}
