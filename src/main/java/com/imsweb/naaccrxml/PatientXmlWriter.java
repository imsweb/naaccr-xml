/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.IOException;
import java.io.Writer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;
import com.imsweb.naaccrxml.runtime.NaaccrStreamContext;
import com.imsweb.naaccrxml.runtime.RuntimeNaaccrDictionary;

import static com.imsweb.naaccrxml.NaaccrOptions.NEW_LINE_CRLF;
import static com.imsweb.naaccrxml.NaaccrOptions.NEW_LINE_LF;

/**
 * This class can be used to wrap a generic writer into a patient writer handling the NAACCR XML format.
 */
public class PatientXmlWriter implements PatientWriter {

    // XStream object responsible for reading patient objects
    protected XStream _xstream;

    // the underlined writer
    protected HierarchicalStreamWriter _writer;

    // cached value for new line character(s)
    protected String _newLine;

    // sometimes we want to finalize the writing operation without closing the writer itself...
    protected boolean _hasBeenFinalized = false;

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param rootData required root data (corresponds to the content of NaaccrData)
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlWriter(Writer writer, NaaccrData rootData) throws NaaccrIOException {
        this(writer, rootData, null, (NaaccrDictionary)null, null);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param rootData required root data (corresponds to the content of NaaccrData)
     * @param options optional options
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrOptions options) throws NaaccrIOException {
        this(writer, rootData, options, (NaaccrDictionary)null, null);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param rootData required root data (corresponds to the content of NaaccrData)
     * @param options optional options
     * @param userDictionary optional user-defined dictionary
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrOptions options, NaaccrDictionary userDictionary) throws NaaccrIOException {
        this(writer, rootData, options, Collections.singletonList(userDictionary), null);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param rootData required root data (corresponds to the content of NaaccrData)
     * @param options optional options
     * @param userDictionaries optional user-defined dictionaries (can be null or empty)
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrOptions options, List<NaaccrDictionary> userDictionaries) throws NaaccrIOException {
        this(writer, rootData, options, userDictionaries, null);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param rootData required root data (corresponds to the content of NaaccrData)
     * @param options optional options
     * @param userDictionary optional user-defined dictionary
     * @param conf optional stream configuration
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrStreamConfiguration conf) throws NaaccrIOException {
        this(writer, rootData, options, Collections.singletonList(userDictionary), conf);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param rootData required root data (corresponds to the content of NaaccrData)
     * @param options optional options
     * @param userDictionaries optional user-defined dictionaries (can be null or empty)
     * @param conf optional stream configuration
     * @throws NaaccrIOException if anything goes wrong
     */
    public PatientXmlWriter(Writer writer, NaaccrData rootData, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrStreamConfiguration conf) throws NaaccrIOException {

        try {
            // we always need options
            if (options == null)
                options = NaaccrOptions.getDefault();

            // we always need a configuration
            if (conf == null)
                conf = NaaccrStreamConfiguration.getDefault();

            // compute the end-of-line character(s)
            _newLine = NEW_LINE_LF.equals(options.getNewLine()) ? "\n" : NEW_LINE_CRLF.equals(options.getNewLine()) ? "\r\n" : System.getProperty("line.separator");

            // need to expose xstream so the other methods can use it...
            _xstream = conf.getXstream();

            // create the context
            NaaccrStreamContext context = new NaaccrStreamContext();
            context.setOptions(options);
            context.setConfiguration(conf);

            // get the base dictionary we need
            NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByUri(rootData.getBaseDictionaryUri());

            // clean-up the dictionaries
            Map<String, NaaccrDictionary> dictionaries = new HashMap<>();
            if (userDictionaries != null)
                for (NaaccrDictionary userDictionary : userDictionaries)
                    if (userDictionary != null)
                        dictionaries.put(userDictionary.getDictionaryUri(), userDictionary);

            // create the writer
            _writer = new PrettyPrintWriter(writer, new char[] {' ', ' ', ' ', ' '}) {
                protected String getNewLine() {
                    return _newLine;
                }
            };

            // would be better to use a "header writer", I think XStream has one actually; that would be better...
            try {
                writer.write("<?xml version=\"1.0\"?>\n\n");
            }
            catch (IOException e) {
                throw new NaaccrIOException(e.getMessage());
            }

            // write standard attributes
            _writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT);
            if (rootData.getBaseDictionaryUri() == null)
                throw new NaaccrIOException("base dictionary URI is required");
            _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT, rootData.getBaseDictionaryUri());
            if (!dictionaries.isEmpty()) {
                if (rootData.getUserDictionaryUri() != null && !rootData.getUserDictionaryUri().isEmpty() && !new HashSet<>(rootData.getUserDictionaryUri()).equals(dictionaries.keySet()))
                    throw new NaaccrIOException("Provided dictionaries are not the ones referenced in the rootData");
                _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT, StringUtils.join(new TreeSet<>(dictionaries.keySet()), ' '));
            }
            if (rootData.getRecordType() == null)
                throw new NaaccrIOException("record type is required");
            _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE, rootData.getRecordType());
            ZonedDateTime generationTime = rootData.getTimeGenerated() != null ? ZonedDateTime.ofInstant(rootData.getTimeGenerated().toInstant(), ZoneId.systemDefault()) : ZonedDateTime.now();
            _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED, generationTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            // always use the current specs; doesn't matter the value on the root object...
            _writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_SPEC_VERSION, NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);

            // write non-standard attributes
            Set<String> standardAttributes = new HashSet<>();
            standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT);
            standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT);
            standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE);
            standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED);
            standardAttributes.add(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_SPEC_VERSION);
            for (Entry<String, String> entry : rootData.getExtraRootParameters().entrySet())
                if (!standardAttributes.contains(entry.getKey()) && !entry.getKey().startsWith("xmlns"))
                    _writer.addAttribute(entry.getKey(), entry.getValue());

            // add the default namespace, always use the library value...
            _writer.addAttribute("xmlns", NaaccrXmlUtils.NAACCR_XML_NAMESPACE);

            // add any user-defined namespaces
            conf.getRegisterNamespaces().forEach((key, value) -> _writer.addAttribute("xmlns:" + key, value));

            // create or get the runtime dictionary
            if (conf.getCachedDictionary() == null || !conf.getCachedDictionary().getId().equals(RuntimeNaaccrDictionary.computeId(rootData.getRecordType(), baseDictionary, dictionaries.values())))
                conf.setCachedDictionary(new RuntimeNaaccrDictionary(rootData.getRecordType(), baseDictionary, dictionaries.values()));

            // now we are ready to create our reading context and make it available to the patient converter
            context.setDictionary(conf.getCachedDictionary());
            conf.getPatientConverter().setContext(context);

            // write the root items
            for (Item item : rootData.getItems())
                conf.getPatientConverter().writeItem(item, _writer);

            // write extensions
            if (!Boolean.TRUE.equals(options.getIgnoreExtensions()) && rootData.getExtensions() != null)
                for (Object extension : rootData.getExtensions())
                    _xstream.marshal(extension, _writer);
        }
        catch (ConversionException ex) {
            throw convertSyntaxException(ex);
        }
        catch (RuntimeException ex) {
            throw new NaaccrIOException("unable to write XML", ex);
        }
    }

    @Override
    public void writePatient(Patient patient) throws NaaccrIOException {
        try {
            _xstream.marshal(patient, _writer);
        }
        catch (ConversionException ex) {
            throw convertSyntaxException(ex);
        }
        catch (RuntimeException ex) {
            throw new NaaccrIOException("unable to write XML", ex);
        }
    }

    @Override
    public void closeAndKeepAlive() {
        if (!_hasBeenFinalized) {
            _writer.endNode();
            _hasBeenFinalized = true;
        }
        _writer.flush();
    }

    @Override
    public void close() {
        closeAndKeepAlive();
        _writer.close();
    }

    /**
     * Returns the new line character(s) this writer uses.
     */
    public String getNewLine() {
        return _newLine;
    }

    /**
     * We don't want to expose the conversion exceptions, so let's translate them into our own exceptions...
     */
    protected NaaccrIOException convertSyntaxException(ConversionException ex) {
        String msg = ex.get("message");
        if (msg == null)
            msg = ex.getMessage();
        NaaccrIOException e = new NaaccrIOException(msg, ex);
        if (ex.get("line number") != null)
            e.setLineNumber(Integer.valueOf(ex.get("line number")));
        e.setPath(ex.get("path"));
        return e;
    }
}
