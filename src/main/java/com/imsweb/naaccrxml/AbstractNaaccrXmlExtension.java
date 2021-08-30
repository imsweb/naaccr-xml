/*
 * Copyright (C) 2021 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Any class that is registered via the <code>NaaccrStreamConfiguration</code> to deal with reading/writing extensions and that extends this abstract class
 * will be automatically populated with a line number when the framework reads XML data.
 */
public abstract class AbstractNaaccrXmlExtension implements NaaccrXmlExtension {

    @XStreamOmitField
    private Integer _startLineNumber;

    @Override
    public Integer getStartLineNumber() {
        return _startLineNumber;
    }

    @Override
    public void setStartLineNumber(Integer startLineNumber) {
        _startLineNumber = startLineNumber;
    }
}
