/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.pages;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrIOException;
import com.imsweb.naaccrxml.NaaccrObserver;
import com.imsweb.naaccrxml.NaaccrOptions;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.PatientXmlReader;
import com.imsweb.naaccrxml.PatientXmlWriter;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.gui.StandaloneOptions;

public class XmlToXmlPage extends AbstractProcessingPage {

    public XmlToXmlPage() {
        super(true);
    }

    @Override
    protected String getSourceLabelText() {
        return "Source XML File:";
    }

    @Override
    protected String getTargetLabelText() {
        return "Target XML File:";
    }

    @Override
    protected List<String> getRequiredUserDefinedDictionaries(File file) {
        if (file == null || !file.exists())
            return Collections.emptyList();

        String rawDictionaries = NaaccrXmlUtils.getAttributesFromXmlFile(file).get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT);
        if (StringUtils.isEmpty(rawDictionaries))
            return Collections.emptyList();

        return Arrays.asList(StringUtils.split(rawDictionaries, ' '));
    }

    @Override
    protected StandaloneOptions createOptions() {
        return new StandaloneOptions(false, false, true, true);
    }

    @Override
    protected void runProcessing(File source, File target, NaaccrOptions options, List<NaaccrDictionary> dictionaries, NaaccrObserver observer) throws NaaccrIOException {
        if (source == null)
            throw new NaaccrIOException("Source XML file is required");
        if (!source.exists())
            throw new NaaccrIOException("Source XML file must exist");
        if (!target.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        // create the reader and writer and let them do all the work!
        try (PatientXmlReader reader = new PatientXmlReader(NaaccrXmlUtils.createReader(source), options, dictionaries)) {
            try (PatientXmlWriter writer = new PatientXmlWriter(NaaccrXmlUtils.createWriter(target), reader.getRootData(), options, dictionaries)) {
                Patient patient = reader.readPatient();
                while (patient != null && !Thread.currentThread().isInterrupted()) {
                    if (observer != null)
                        observer.patientRead(patient);
                    writer.writePatient(patient);
                    if (observer != null)
                        observer.patientWritten(patient);
                    patient = reader.readPatient();
                }
            }
        }
    }

    @Override
    protected NaaccrFormat getFormatForInputFile(File file) {

        if (file == null || !file.exists()) {
            reportAnalysisError(new Exception("unable to find selected file"));
            return null;
        }

        try {
            NaaccrFormat format = NaaccrFormat.getInstance(NaaccrXmlUtils.getFormatFromXmlFile(file));

            // if the format was properly found but the file references more than one user-defined dictionary, fail the analysis
            if (getRequiredUserDefinedDictionaries(file).size() > 1) {
                reportAnalysisError(new Exception("the source file references more than one user-defined dictionary, this application only supports a single one."));
                return null;
            }

            return format;
        }
        catch (RuntimeException e) {
            reportAnalysisError(new Exception("unable to identify file format"));
            return null;
        }
    }
}
