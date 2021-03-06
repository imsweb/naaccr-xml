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

    private boolean _writeNumbers;

    public SasCsvToXml(String xmlPath, String naaccrVersion, String recordType) {
        this(SasUtils.computeCsvPathFromXmlPath(xmlPath), xmlPath, naaccrVersion, recordType);
    }

    public SasCsvToXml(String csvPath, String xmlPath, String naaccrVersion, String recordType) {
        if (csvPath == null || csvPath.trim().isEmpty())
            SasUtils.logError("No source CSV path was provided");
        else {
            _csvFile = new File(csvPath);
            SasUtils.logInfo("Source CSV: " + _csvFile.getAbsolutePath());

            if (xmlPath == null || xmlPath.trim().isEmpty())
                SasUtils.logError("No target XML path was provided");
            else {
                // never EVER return the same path for the XML than the CSV!
                if (xmlPath.equalsIgnoreCase(csvPath))
                    xmlPath = csvPath + ".xml";
                _xmlFile = new File(xmlPath);
                if (!new File(_xmlFile.getAbsolutePath()).getParentFile().exists())
                    SasUtils.logError("Parent directory for target XML path doesn't exist: " + _xmlFile.getParentFile().getAbsolutePath());
                else
                    SasUtils.logInfo("Target XML: " + _xmlFile.getAbsolutePath());
            }
        }

        _dictionaryFiles = new ArrayList<>();

        _naaccrVersion = naaccrVersion;
        if (_naaccrVersion == null || _naaccrVersion.trim().isEmpty())
            SasUtils.logError("NAACCR version needs to be provided");
        if (!"140".equals(naaccrVersion) && !"150".equals(naaccrVersion) && !"160".equals(naaccrVersion) && !"180".equals(naaccrVersion) && !"210".equals(naaccrVersion) && !"220".equals(
                naaccrVersion))
            SasUtils.logError("NAACCR version must be 140, 150, 160, 180, 210 or 220; got " + _naaccrVersion);

        _recordType = recordType;
        if (_recordType == null || _recordType.trim().isEmpty())
            SasUtils.logError("Record type needs to be provided");
        if (!"A".equals(_recordType) && !"M".equals(_recordType) && !"C".equals(_recordType) && !"I".equals(_recordType))
            SasUtils.logError("Record type must be A, M, C or I; got " + _recordType);

        _writeNumbers = false;
    }

    public void setDictionary(String dictionaryPath, String dictionaryUri) {
        if (dictionaryPath != null && !dictionaryPath.trim().isEmpty()) {
            for (String path : dictionaryPath.split(" ")) {
                File dictionaryFile = new File(dictionaryPath);
                if (!dictionaryFile.exists())
                    SasUtils.logError("Invalid CSV dictionary path: " + dictionaryPath);
                else {
                    try {
                        SasUtils.validateCsvDictionary(dictionaryFile);
                        _dictionaryFiles.add(dictionaryFile);
                        SasUtils.logInfo("Dictionary: " + dictionaryFile.getAbsolutePath());
                    }
                    catch (IOException e) {
                        SasUtils.logError("Invalid CSV dictionary: " + e.getMessage());
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

    public void setWriteNumbers(String value) {
        _writeNumbers = value != null && ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value));
    }

    public String getWriteNumbers() {
        return _writeNumbers ? "Yes" : "No";
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
        SasUtils.logInfo("Starting converting CSV to XML...");
        try {
            Set<String> requestedFields = SasUtils.extractRequestedFields(fields, availableFields);

            Map<String, String> itemNumbers = new HashMap<>();
            for (SasFieldInfo info : availableFields)
                itemNumbers.put(info.getNaaccrId(), info.getNum().toString());

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
                            writer.write("\n            userDictionaryUri=\"" + _dictionaryUris + "\"");
                        writer.write("\n            recordType=\"" + _recordType + "\"");
                        writer.write("\n            specificationVersion=\"1.4\"");
                        writer.write("\n            xmlns=\"http://naaccr.org/naaccrxml\"");
                        writer.write(">\n");
                        for (Entry<String, String> entry : rootFields.entrySet()) {
                            String val = values.get(entry.getKey());
                            if (val != null && !val.trim().isEmpty())
                                writer.write(createItemLine("    ", entry.getKey(), itemNumbers.get(entry.getKey()), val));
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
                                writer.write(createItemLine("        ", entry.getKey(), itemNumbers.get(entry.getKey()), val));
                        }
                    }

                    // we always have to write the tumor!
                    writer.write("        <Tumor>\n");
                    for (Entry<String, String> entry : tumorFields.entrySet()) {
                        String val = values.get(entry.getKey());
                        if (val != null && !val.trim().isEmpty())
                            writer.write(createItemLine("            ", entry.getKey(), itemNumbers.get(entry.getKey()), val));
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

        SasUtils.logInfo("Successfully created " + _xmlFile.getAbsolutePath());
    }

    private String createItemLine(String indentation, String itemId, String itemNumber, String value) {
        StringBuilder buf = new StringBuilder(indentation);
        buf.append("<Item naaccrId=\"").append(itemId).append("\"");
        if (itemNumber != null && _writeNumbers)
            buf.append(" naaccrNum=\"").append(itemNumber).append("\"");
        buf.append(">").append(SasUtils.cleanUpValueToWriteAsXml(value)).append("</Item>\n");
        return buf.toString();
    }

    public void cleanup() {
        if (!_csvFile.delete())
            SasUtils.logError("Unable to cleanup tmp CSV file, it will have to be manually deleted...");
    }
}
