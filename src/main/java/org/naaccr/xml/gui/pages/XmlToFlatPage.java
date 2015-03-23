/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui.pages;

import java.io.File;
import java.util.List;

import org.naaccr.xml.NaaccrIOException;
import org.naaccr.xml.NaaccrStreamObserver;
import org.naaccr.xml.NaaccrXmlOptions;
import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.gui.StandaloneOptions;

public class XmlToFlatPage extends AbstractProcessingPage {
    
    @Override
    protected String getSourceLabelText() {
        return "Source XML File:";
    }

    @Override
    protected String getTargetLabelText() {
        return "Target Flat File:";
    }

    @Override
    protected StandaloneOptions createOptions() {
        return new StandaloneOptions(false, true, true, false);
    }

    @Override
    protected void runProcessing(File source, File target, NaaccrXmlOptions options, NaaccrDictionary dictionary, NaaccrStreamObserver observer) throws NaaccrIOException {
        NaaccrXmlUtils.xmlToFlat(source, target, options, dictionary, observer);
    }

    @Override
    protected int calculateProgressOffset(List<Patient> patients) {
        int offset = 0;
        // this isn't perfect, technically we should use the ending patient tag, but this is using the staring tag; that would be hard to fix though...
        for (Patient patient : patients)
            offset = Math.max(offset, patient.getLineNumber());
        return offset;
    }
}
