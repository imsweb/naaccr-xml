/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui.pages;

import java.io.File;

import org.naaccr.xml.NaaccrFormat;
import org.naaccr.xml.NaaccrIOException;
import org.naaccr.xml.NaaccrObserver;
import org.naaccr.xml.NaaccrOptions;
import org.naaccr.xml.NaaccrXmlDictionaryUtils;
import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.PatientXmlReader;
import org.naaccr.xml.entity.NaaccrData;
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
    protected void runProcessing(File source, File target, NaaccrOptions options, NaaccrDictionary dictionary, NaaccrObserver observer) throws NaaccrIOException {
        NaaccrXmlUtils.xmlToFlat(source, target, options, dictionary, observer);
    }

    @Override
    protected NaaccrFormat getFormatForInputFile(File file) {

        // make sure the file exists
        if (file == null || !file.exists()) {
            reportAnalysisError("unable to find selected file");
            return null;
        }

        try (PatientXmlReader reader = new PatientXmlReader(NaaccrXmlUtils.createReader(file), null, null)) {
            NaaccrData rootData = reader.getRootData(); // all the validation happens when creating the root object in the reader...
            return NaaccrFormat.getInstance(NaaccrXmlDictionaryUtils.extractVersionFromUri(rootData.getBaseDictionaryUri()), rootData.getRecordType());
        }
        catch (NaaccrIOException e) {
            reportAnalysisError("line " + e.getLineNumber() + ", " + e.getMessage());
            return null;
        }
    }
}
