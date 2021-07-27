/*
 * Copyright (C) 2021 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.io.IOException;
import java.util.Date;

import com.thoughtworks.xstream.converters.SingleValueConverter;

import com.imsweb.naaccrxml.NaaccrXmlUtils;

/**
 * Special convertor used for the ISO 8601 dates.
 * <br/><br/>
 * There is a "native" Java ISO 8601 date formatter, but unfortunately there are two versions of it (with and without offset)
 * and the XML ISO 8601 accepts both, and so it's not as simple as just using one Java formatter...
 */
public class Iso8601DateConverter implements SingleValueConverter {

    @Override
    public boolean canConvert(Class type) {
        return Date.class.equals(type);
    }

    @Override
    public String toString(Object obj) {
        return NaaccrXmlUtils.formatIso8601Date((Date)obj);
    }

    @Override
    public Object fromString(String str) {
        try {
            return NaaccrXmlUtils.parseIso8601Date(str);
        }
        catch (IOException e) {
            throw new RuntimeException("Invalid ISO 8601 value: " + str);
        }
    }
}
