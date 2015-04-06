/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.runtime;

import org.naaccr.xml.NaaccrOptions;
import org.xmlpull.v1.XmlPullParser;

public class NaaccrStreamContext {

    protected RuntimeNaaccrDictionary _dictionary;

    protected NaaccrOptions _options;

    protected XmlPullParser _parser;

    public RuntimeNaaccrDictionary getDictionary() {
        return _dictionary;
    }

    public void setDictionary(RuntimeNaaccrDictionary dictionary) {
        _dictionary = dictionary;
    }

    public NaaccrOptions getOptions() {
        return _options;
    }

    public void setOptions(NaaccrOptions options) {
        _options = options;
    }

    public XmlPullParser getParser() {
        return _parser;
    }

    public void setParser(XmlPullParser parser) {
        _parser = parser;
    }
}
