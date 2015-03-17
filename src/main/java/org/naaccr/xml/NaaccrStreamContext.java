/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;
import org.xmlpull.v1.XmlPullParser;

public class NaaccrStreamContext {

    protected RuntimeNaaccrDictionary _dictionary;

    protected NaaccrXmlOptions _options;

    protected XmlPullParser _parser;

    public RuntimeNaaccrDictionary getDictionary() {
        return _dictionary;
    }

    public void setDictionary(RuntimeNaaccrDictionary dictionary) {
        _dictionary = dictionary;
    }

    public NaaccrXmlOptions getOptions() {
        return _options;
    }

    public void setOptions(NaaccrXmlOptions options) {
        _options = options;
    }

    public XmlPullParser getParser() {
        return _parser;
    }

    public void setParser(XmlPullParser parser) {
        _parser = parser;
    }
}
