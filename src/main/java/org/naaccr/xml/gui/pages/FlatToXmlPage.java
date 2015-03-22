/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui.pages;

import java.io.File;

import org.naaccr.xml.NaaccrIOException;
import org.naaccr.xml.NaaccrStreamObserver;
import org.naaccr.xml.NaaccrXmlOptions;
import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.gui.StandaloneOptions;

public class FlatToXmlPage extends AbstractProcessingPage {

    @Override
    protected String getSourceLabelText() {
        return "Source Flat File:";
    }

    @Override
    protected String getTargetLabelText() {
        return "Target XML File:";
    }

    @Override
    protected StandaloneOptions createOptions() {
        return new StandaloneOptions(true, false, false, true);
    }

    @Override
    protected void runProcessing(File source, File target, NaaccrXmlOptions options, NaaccrDictionary dictionary, NaaccrStreamObserver observer) throws NaaccrIOException {
        NaaccrXmlUtils.flatToXml(source, target, options, dictionary, observer);
    }
}
