/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Use this class to convert a given CSV file into a NAACCR XML file.
 * <br/><br/>
 * THIS CLASS IS IMPLEMENTED TO BE COMPATIBLE WITH JAVA 7; BE CAREFUL WHEN MODIFYING IT.
 */
@SuppressWarnings("ALL")
public class SasCsvToXml {

    private File _csvFile, _xmlFile;

    private List<File> _dictionaryFiles;

    private String _naaccrVersion, _recordType;

    private String _dictionaryUris;

    public SasCsvToXml(String xmlPath, String naaccrVersion, String recordType) {
        this(xmlPath.replace(".xml", ".csv"), xmlPath, naaccrVersion, recordType);
    }

    public SasCsvToXml(String csvPath, String xmlPath, String naaccrVersion, String recordType) {
        if (csvPath == null || csvPath.trim().isEmpty())
            System.err.println("!!! No source CSV path was provided");
        else {
            _csvFile = new File(csvPath);
            if (!_csvFile.exists())
                System.err.println("!!! Invalid source CSV file: " + csvPath);
            else
                System.out.println(" > source CSV: " + _csvFile.getAbsolutePath());

            if (xmlPath == null || xmlPath.trim().isEmpty())
                System.err.println("!!! No target XML path was provided");
            else {
                // never EVER return the same path for the XML than the CSV!
                if (xmlPath.equalsIgnoreCase(csvPath))
                    xmlPath = csvPath + ".xml";
                _xmlFile = new File(xmlPath);
                if (!new File(_xmlFile.getAbsolutePath()).getParentFile().exists())
                    System.err.println("!!! Parent directory for target XML path doesn't exist: " + _xmlFile.getParentFile().getAbsolutePath());
                else
                    System.out.println(" > target XML: " + _xmlFile.getAbsolutePath());
            }
        }

        _dictionaryFiles = new ArrayList<>();

        _naaccrVersion = naaccrVersion;
        if (_naaccrVersion == null || _naaccrVersion.trim().isEmpty())
            System.err.println("!!! NAACCR version needs to be provided");
        if (!"140".equals(naaccrVersion) && !"150".equals(naaccrVersion) && !"160".equals(naaccrVersion) && !"180".equals(naaccrVersion))
            System.err.println("!!! NAACCR version must be 140, 150, 160 or 180; got " + _naaccrVersion);

        _recordType = recordType;
        if (_recordType == null || _recordType.trim().isEmpty())
            System.err.println("!!! Record type needs to be provided");
        if (!"A".equals(_recordType) && !"M".equals(_recordType) && !"C".equals(_recordType) && !"I".equals(_recordType))
            System.err.println("!!! Record type must be A, M, C or I; got " + _recordType);
    }

    public void setDictionary(String dictionaryPath, String dictionaryUri) {
        if (dictionaryPath != null && !dictionaryPath.trim().isEmpty()) {
            for (String path : dictionaryPath.split(" ")) {
                File dictionaryFile = new File(dictionaryPath);
                if (!dictionaryFile.exists())
                    System.err.println("!!! Invalid CSV dictionary path " + dictionaryPath);
                else {
                    try {
                        SasUtils.validateCsvDictionary(dictionaryFile);
                        _dictionaryFiles.add(dictionaryFile);
                        System.out.println(" > dictionary: " + dictionaryFile.getAbsolutePath());
                    }
                    catch (IOException e) {
                        System.err.println("!!! Invalid CSV dictionary: " + e.getMessage());
                    }
                }
            }
        }

        _dictionaryUris = dictionaryUri;
    }

    public String getCsvPath() {
        return _csvFile.getAbsolutePath();
    }

    public String getXmlPath() {
        return _xmlFile.getAbsolutePath();
    }

    public String getNaaccrVersion() {
        return _naaccrVersion;
    }

    public String getRecordType() {
        return _recordType;
    }

    public List<SasFieldInfo> getFields() {
        return SasUtils.getFields(_naaccrVersion, _recordType, _dictionaryFiles);
    }

    public void convert() throws IOException {
        convert(null);
    }

    public void convert(String fields) throws IOException {
        convert(fields, getFields());
    }

    public void convert(String fields, List<SasFieldInfo> availableFields) throws IOException {
        System.out.println("Starting converting CSV to XML...");
        try {
            Set<String> requestedFields = null;
            if (fields != null && !fields.trim().isEmpty()) {
                requestedFields = new HashSet<>();
                for (String s : fields.split(",", -1))
                    requestedFields.add(s.trim());
            }

            Map<String, String> rootFields = new HashMap<>(), patientFields = new HashMap<>(), tumorFields = new HashMap<>();
            for (SasFieldInfo field : availableFields) {
                if (requestedFields == null || requestedFields.contains(field.getNaaccrId())) {
                    if ("NaaccrData".equals(field.getParentTag()))
                        rootFields.put(field.getTruncatedNaaccrId(), field.getNaaccrId());
                    else if ("Patient".equals(field.getParentTag()))
                        patientFields.put(field.getTruncatedNaaccrId(), field.getNaaccrId());
                    else if ("Tumor".equals(field.getParentTag()))
                        tumorFields.put(field.getTruncatedNaaccrId(), field.getNaaccrId());
                }
            }

            LineNumberReader reader = null;
            BufferedWriter writer = null;
            try {
                reader = new LineNumberReader(new InputStreamReader(new FileInputStream(_csvFile), StandardCharsets.UTF_8));
                writer = SasUtils.createWriter(_xmlFile);

                List<String> headers = new ArrayList<>();
                String line = reader.readLine();
                if (line == null)
                    throw new IOException("Was expecting to find column headers, didn't find them!");
                headers.addAll(Arrays.asList(line.split(",", -1)));

                String currentPatNum = null;
                line = reader.readLine();
                boolean wroteRoot = false;
                int numPatientWritten = 0;
                while (line != null) {
                    List<String> valList = SasUtils.parseCsvLine(reader.getLineNumber(), line);
                    if (headers.size() != valList.size())
                        throw new IOException("Line " + reader.getLineNumber() + ": expected " + headers.size() + " values but got " + valList.size());

                    Map<String, String> values = new HashMap<>();
                    for (int i = 0; i < valList.size(); i++)
                        values.put(headers.get(i), valList.get(i));

                    String patNum = values.get("patientIdNumber");
                    if (patNum != null && patNum.trim().isEmpty())
                        patNum = null;

                    // do we have to write the root?
                    if (!wroteRoot) {
                        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                        writer.write("\n");
                        writer.write("<NaaccrData");
                        writer.write(" baseDictionaryUri=\"http://naaccr.org/naaccrxml/naaccr-dictionary-" + _naaccrVersion + ".xml\"");
                        if (!_dictionaryFiles.isEmpty() && _dictionaryUris != null && !_dictionaryUris.trim().isEmpty())
                            writer.write(" userDictionaryUri=\"" + _dictionaryUris + "\"");
                        writer.write(" recordType=\"" + _recordType + "\"");
                        writer.write(" specificationVersion=\"1.3\"");
                        writer.write(" xmlns=\"http://naaccr.org/naaccrxml\"");
                        writer.write(">\n");
                        for (Entry<String, String> entry : rootFields.entrySet()) {
                            String val = values.get(entry.getKey());
                            if (val != null && !val.trim().isEmpty())
                                writer.write("    <Item naaccrId=\"" + entry.getValue() + "\">" + SasUtils.cleanUpValueToWriteAsXml(val) + "</Item>\n");
                        }
                        wroteRoot = true;
                    }

                    // do we have to write the patient?
                    if (currentPatNum == null || !currentPatNum.equals(patNum)) {
                        if (numPatientWritten > 0)
                            writer.write("    </Patient>\n");
                        numPatientWritten++;
                        writer.write("    <Patient>\n");
                        for (Entry<String, String> entry : patientFields.entrySet()) {
                            String val = values.get(entry.getKey());
                            if (val != null && !val.trim().isEmpty())
                                writer.write("        <Item naaccrId=\"" + entry.getValue() + "\">" + SasUtils.cleanUpValueToWriteAsXml(val) + "</Item>\n");
                        }
                    }

                    // we always have to write the tumor!
                    writer.write("        <Tumor>\n");
                    for (Entry<String, String> entry : tumorFields.entrySet()) {
                        String val = values.get(entry.getKey());
                        if (val != null && !val.trim().isEmpty())
                            writer.write("            <Item naaccrId=\"" + entry.getValue() + "\">" + SasUtils.cleanUpValueToWriteAsXml(val) + "</Item>\n");
                    }
                    writer.write("        </Tumor>\n");

                    currentPatNum = patNum;
                    line = reader.readLine();
                }

                writer.write("    </Patient>\n");
                writer.write("</NaaccrData>");
            }
            finally {
                if (writer != null)
                    writer.close();
                if (reader != null)
                    reader.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            throw new IOException(e);
        }

        System.out.println("Successfully created " + _xmlFile.getAbsolutePath());
    }

    public void cleanup() {
        if (!_csvFile.delete())
            System.err.println("!!! Unable to cleanup tmp CSV file.");
    }
}
