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
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.gui.StandaloneOptions;

public class XmlValidationPage extends AbstractProcessingPage {

    @Override
    protected String getSourceLabelText() {
        return "Source XML File:";
    }

    @Override
    protected String getTargetLabelText() {
        return "N/A";
    }

    @Override
    protected boolean showTargetInput() {
        return false;
    }

    @Override
    protected StandaloneOptions createOptions() {
        return new StandaloneOptions(false, true, true, false);
    }

    @Override
    protected void runProcessing(File source, File target, NaaccrOptions options, NaaccrDictionary dictionary, NaaccrObserver observer) throws NaaccrIOException {
        try (PatientXmlReader reader = new PatientXmlReader(NaaccrXmlUtils.createReader(source), options, dictionary)) {
            Patient patient = reader.readPatient();
            while (patient != null && !Thread.currentThread().isInterrupted()) {
                // this is the call that will actually collect the errors and show them in the GUI...
                observer.patientRead(patient);
                patient = reader.readPatient();
            }
        }
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

    @Override
    protected String getProcessingResultText(String path, String time, String size) {
        return "Done validating source XML file.";
    }
}
