/*
 * Copyright (C) 2020 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.internal;

import java.io.Writer;

import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class NaaccrXmlPrettyPrintWriter extends PrettyPrintWriter {

    private QuickWriter _internalWriter;

    private final String _newLine;

    public NaaccrXmlPrettyPrintWriter(Writer writer, String newLine) {
        super(writer, new char[]{' ', ' ', ' ', ' '});
        _newLine = newLine;
    }

    @Override
    protected String getNewLine() {
        return _newLine;
    }

    @Override
    protected void writeAttributeValue(QuickWriter writer, String text) {
        super.writeAttributeValue(writer, text);
        if (_internalWriter == null)
            _internalWriter = writer;
    }

    public void addAttributeWithNewLine(String key, String value) {
        super.addAttribute(key, value);

        _internalWriter.write(getNewLine());

        // the indentation is hard-coded to this value because that's what works for all cases;
        // this might need to be reviewed if more attributes are supported in the future...
        _internalWriter.write("           ");
    }
}
