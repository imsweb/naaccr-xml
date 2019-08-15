/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

/**
 * Any class that is registered via the <code>NaaccrStreamConfiguration</code> to deal with reading/writing extensions and that implements this interface
 * will be automatically populated with a line number when the framework reads XML data.
 */
public interface NaaccrXmlExtension {

    Integer getStartLineNumber();

    void setStartLineNumber(Integer startLineNumber);
}
