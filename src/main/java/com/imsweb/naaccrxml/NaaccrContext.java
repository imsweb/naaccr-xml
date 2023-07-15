/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.util.List;

import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;
import com.imsweb.naaccrxml.runtime.RuntimeNaaccrDictionary;

/**
 * A NaaccrContext is used in NaaccrXmlUtils when translating a single line into a patient and vice-versa.
 * <br/><br/>
 * Those operations require a context because unlike the translation of entire files, the single line translation is
 * expected to happen in loops. But some of the context required for the translation is expensive to create (mainly the
 * runtime dictionary), and so this class is used to cache that expensive data.
 */
public class NaaccrContext {

    // the NAACCR format, see NaaccrFormat
    private String _format;

    // the NAACCR option, can be null
    private NaaccrOptions _options;

    // the user-defined dictionaries, can be null or empty
    private List<NaaccrDictionary> _userDictionaries;

    // the cached stream configuration
    private NaaccrStreamConfiguration _streamConfiguration;

    /**
     * Constructor
     * @param format NAACCR format, required; see NaaccrFormat
     */
    public NaaccrContext(String format) {
        this(format, null, null);
    }

    /**
     * Constructor
     * @param format NAACCR format, required; see NaaccrFormat
     * @param userDictionaries the user-defined dictionaries, can be null or empty
     */
    public NaaccrContext(String format, List<NaaccrDictionary> userDictionaries) {
        this(format, userDictionaries, null);
    }

    /**
     * Constructor
     * @param format NAACCR format, required; see NaaccrFormat
     * @param userDictionaries the user-defined dictionaries, can be null or empty
     * @param options NAACCR option, can be null
     */
    public NaaccrContext(String format, List<NaaccrDictionary> userDictionaries, NaaccrOptions options) {
        if (format == null)
            throw new IllegalStateException("Format cannot be null; see constants in NaaccrFormat class");
        _format = format;
        _userDictionaries = userDictionaries;
        _options = options;

        _streamConfiguration = NaaccrStreamConfiguration.getDefault();

        // let's create and cache the runtime dictionary corresponding to the format and user dictionaries...
        NaaccrFormat nf = NaaccrFormat.getInstance(format);
        NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(nf.getNaaccrVersion());
        try {
            _streamConfiguration.setCachedDictionary(new RuntimeNaaccrDictionary(nf.getRecordType(), baseDictionary, userDictionaries));
        }
        catch (NaaccrIOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getFormat() {
        return _format;
    }

    public NaaccrOptions getOptions() {
        return _options;
    }

    public List<NaaccrDictionary> getUserDictionaries() {
        return _userDictionaries;
    }

    public NaaccrStreamConfiguration getStreamConfiguration() {
        return _streamConfiguration;
    }
}
