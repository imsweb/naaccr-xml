/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.Set;

import com.imsweb.naaccrxml.NaaccrIOException;
import com.imsweb.naaccrxml.NaaccrOptions;

public class NaaccrStreamContext {

    protected RuntimeNaaccrDictionary _dictionary;

    protected NaaccrStreamConfiguration _configuration;

    protected NaaccrOptions _options;

    public RuntimeNaaccrDictionary getDictionary() {
        return _dictionary;
    }

    public void setDictionary(RuntimeNaaccrDictionary dictionary) {
        _dictionary = dictionary;
    }

    public NaaccrStreamConfiguration getConfiguration() {
        return _configuration;
    }

    public void setConfiguration(NaaccrStreamConfiguration configuration) {
        _configuration = configuration;
    }

    public NaaccrOptions getOptions() {
        return _options;
    }

    public void setOptions(NaaccrOptions options) {
        _options = options;
    }

    /**
     * Returns the current line number.
     * @return current line number (from the parser).
     */
    public int getLineNumber() {
        return _configuration.getParser().getLineNumber();
    }

    /**
     * Extracts the tag from the given raw tag (which might contain a namespace).
     * @param tag tag without any namespace
     * @return the tag without any namespace
     * @throws NaaccrIOException if anything goes wrong
     */
    public String extractTag(String tag) throws NaaccrIOException {
        if (tag == null)
            throw new NaaccrIOException("missing tag");
        int idx = tag.indexOf(':');
        if (idx != -1) {
            String namespace = tag.substring(0, idx), cleanTag = tag.substring(idx + 1);
            // check for the namespace only if the tag is a default one (Patient, Tumor, etc...) or if extensions are enabled
            if (_configuration.getAllowedTagsForNamespacePrefix(null).contains(cleanTag) || !Boolean.TRUE.equals(_options.getIgnoreExtensions())) {
                Set<String> allowedTags = _configuration.getAllowedTagsForNamespacePrefix(namespace);
                if (allowedTags == null || !allowedTags.contains(cleanTag))
                    throw new NaaccrIOException("tag '" + cleanTag + "' is not allowed for namespace '" + namespace + "'");
            }
            return cleanTag;
        }
        else {
            if (Boolean.TRUE.equals(_options.getUseStrictNamespaces()) && !_configuration.getAllowedTagsForNamespacePrefix(null).contains(tag))
                throw new NaaccrIOException("tag '" + tag + "' needs to be defined within a namespace");
            return tag;
        }
    }
}
