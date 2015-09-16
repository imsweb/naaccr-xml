/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.pages;

import java.io.File;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrIOException;
import com.imsweb.naaccrxml.NaaccrObserver;
import com.imsweb.naaccrxml.NaaccrOptions;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.PatientXmlReader;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.gui.StandaloneOptions;

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
