/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui.pages;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.naaccr.xml.NaaccrFormat;
import org.naaccr.xml.NaaccrIOException;
import org.naaccr.xml.NaaccrObserver;
import org.naaccr.xml.NaaccrOptions;
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
    protected void runProcessing(File source, File target, NaaccrOptions options, NaaccrDictionary dictionary, NaaccrObserver observer) throws NaaccrIOException {
        NaaccrXmlUtils.flatToXml(source, target, options, dictionary, observer);
    }

    @Override
    protected NaaccrFormat getFormatForInputFile(File file) {

        // make sure the file exists
        if (file == null || !file.exists()) {
            reportAnalysisError("unable to find selected file");
            return null;
        }

        // make sure the first line is available
        String firstLine = null;
        try (BufferedReader reader = new BufferedReader(NaaccrXmlUtils.createReader(file))) {
            firstLine = reader.readLine();
        }
        catch (IOException e) {
            // ignored
        }
        if (firstLine == null || firstLine.isEmpty()) {
            reportAnalysisError("selected file is empty");
            return null;
        }

        // make sure the NAACCR version is valid
        String version = firstLine.length() < 19 ? "" : firstLine.substring(16, 19).trim();
        if (version.isEmpty()) {
            reportAnalysisError("blank NAACCR version on first record");
            return null;
        }
        if (!NaaccrFormat.isVersionSupported(version)) {
            reportAnalysisError("invalid/unsupported NAACCR version on first record: " + version);
            return null;
        }

        // make sure the record type is valid
        String type = firstLine.substring(0, 1).trim();
        if (type.isEmpty()) {
            reportAnalysisError("blank record type on first record");
            return null;
        }
        if (!NaaccrFormat.isRecordTypeSupported(type)) {
            reportAnalysisError("invalid/unsupported record type on first record: " + type);
            return null;
        }

        // make sure the format is valid (it should at this point)
        NaaccrFormat format = NaaccrFormat.getInstance(version, type);
        if (format == null) {
            reportAnalysisError("invalid/unsupported format");
            return null;
        }

        // make sure first line has the correct length
        if (firstLine.length() != format.getLineLength()) {
            reportAnalysisError("invalid line length for first record, expected " + format.getLineLength() + " but got " + firstLine.length());
            return null;
        }

        return format;
    }
}
