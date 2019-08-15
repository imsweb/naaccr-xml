/*
 * Copyright (C) 2019 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import com.imsweb.naaccrxml.NaaccrXmlExtension;

/**
 * This class is used to add line number when reading extension objects.
 */
public class LineNumberExtensionConverter extends AbstractReflectionConverter {

    NaaccrStreamConfiguration _conf;

    public LineNumberExtensionConverter(NaaccrStreamConfiguration conf) {
        super(conf.getXstream().getMapper(), conf.getXstream().getReflectionProvider());
        _conf = conf;
    }

    @Override
    public boolean canConvert(Class type) {
        return NaaccrXmlExtension.class.isAssignableFrom(type);
    }

    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        int lineNumber = _conf.getParser().getLineNumber();
        Object object = super.unmarshal(reader, context);
        ((NaaccrXmlExtension)object).setStartLineNumber(lineNumber);
        return object;
    }
}
