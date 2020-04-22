/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Use this class to convert a given NAACCR XML file into a CSV file.
 * <br/><br/>
 * THIS CLASS IS IMPLEMENTED TO BE COMPATIBLE WITH JAVA 7; BE CAREFUL WHEN MODIFYING IT.
 */
@SuppressWarnings("ALL")
public class SasXmlToCsv {

    private File _xmlFile, _csvFile;

    private List<File> _dictionaryFiles;

    private String _naaccrVersion, _recordType;

    public SasXmlToCsv(String xmlPath, String naaccrVersion, String recordType) {
        this(xmlPath, null, naaccrVersion, recordType);
    }

    public SasXmlToCsv(String xmlPath, String csvPath, String naaccrVersion, String recordType) {
        if (xmlPath == null || xmlPath.trim().isEmpty())
            System.err.println("!!! No source XML path was provided");
        else {
            _xmlFile = new File(xmlPath);
            if (!_xmlFile.exists())
                System.err.println("!!! Invalid source XML file: " + xmlPath);
            else
                System.out.println(" > input XML: " + _xmlFile.getAbsolutePath());

            if (csvPath == null || csvPath.trim().isEmpty())
                csvPath = SasUtils.computeCsvPathFromXmlPath(xmlPath);
            // never EVER return the same path for the CSV than the XML!
            if (csvPath.equalsIgnoreCase(xmlPath))
                csvPath = xmlPath + ".csv";
            _csvFile = new File(csvPath);
            if (!new File(_csvFile.getAbsolutePath()).getParentFile().exists())
                System.err.println("!!! Parent directory for CSV path doesn't exist: " + _csvFile.getParentFile().getAbsolutePath());
            else
                System.out.println(" > temp CSV: " + _csvFile.getAbsolutePath());
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

    public void setDictionary(String dictionaryPath) {
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
    }

    public String getXmlPath() {
        return _xmlFile.getAbsolutePath();
    }

    public String getCsvPath() {
        return _csvFile.getAbsolutePath();
    }

    public String getDictionaryPath() {
        StringBuilder buf = new StringBuilder();
        for (File dictionaryFile : _dictionaryFiles) {
            if (buf.length() > 0)
                buf.append(" ");
            buf.append(dictionaryFile.getAbsolutePath());
        }
        return buf.toString();
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
        convert(fields, true);
    }

    public void convert(String fields, boolean addExtraCharFields) throws IOException {
        convert(fields, addExtraCharFields, getFields());
    }

    public void convert(String fields, boolean addExtraCharFields, List<SasFieldInfo> availableFields) throws IOException {
        try {
            Set<String> requestedFields = null;
            if (fields != null && !fields.trim().isEmpty()) {
                requestedFields = new HashSet<>();
                for (String s : fields.replace(" ", "").split(",", -1))
                    requestedFields.add(s);
            }

            Map<String, Integer> allFields = new LinkedHashMap<>();
            for (SasFieldInfo field : availableFields) {
                if (requestedFields == null || requestedFields.contains(field.getNaaccrId())) {
                    allFields.put(field.getTruncatedNaaccrId(), field.getLength());
                    if (!field.getNaaccrId().equals(field.getTruncatedNaaccrId()))
                        System.out.println("Truncated '" + field.getNaaccrId() + "' into '" + field.getTruncatedNaaccrId() + "'...");
                }
            }

            System.out.println("Starting converting XML to CSV...");

            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_csvFile), StandardCharsets.UTF_8));

                // write the headers
                StringBuilder buf = new StringBuilder();
                for (String field : allFields.keySet())
                    buf.append(field).append(",");
                buf.setLength(buf.length() - 1);
                writer.write(buf.toString());
                writer.write("\n");
                buf.setLength(0);

                // hack alert - force SAS to recognize all variables as characters...  Sigh...
                if (addExtraCharFields) {
                    for (Entry<String, Integer> field : allFields.entrySet()) {
                        for (int i = 0; i < field.getValue(); i++)
                            buf.append("-");
                        buf.append(",");
                    }
                    buf.setLength(buf.length() - 1);
                    writer.write(buf.toString());
                    writer.write("\n");
                    buf.setLength(0);
                }

                if (_xmlFile.getName().toLowerCase().endsWith(".zip")) {
                    ZipFile zipFile = null;
                    ZipInputStream zipIs = null;
                    try {
                        zipFile = new ZipFile(_xmlFile);
                        zipIs = new ZipInputStream(new FileInputStream(_xmlFile));
                        ZipEntry entry = zipIs.getNextEntry();
                        while (entry != null) {
                            SasXmlReader reader = new SasXmlReader(new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8)));
                            convertSingleFile(reader, writer, addExtraCharFields, allFields);
                            entry = zipIs.getNextEntry();
                        }
                    }
                    finally {
                        if (zipIs != null)
                            zipIs.close();
                        if (zipFile != null)
                            zipFile.close();
                    }
                }
                else {
                    SasXmlReader reader = null;
                    try {
                        reader = new SasXmlReader(SasUtils.createReader(_xmlFile));
                        convertSingleFile(reader, writer, addExtraCharFields, allFields);
                    }
                    finally {
                        if (reader != null)
                            reader.close();
                    }
                }
            }
            finally {
                if (writer != null)
                    writer.close();
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

        System.out.println("Successfully created " + _csvFile.getAbsolutePath());
    }

    private void convertSingleFile(SasXmlReader reader, BufferedWriter writer, boolean addExtraCharFields, Map<String, Integer> allFields) throws IOException {
        Pattern quotePattern = Pattern.compile("\"", Pattern.LITERAL);
        StringBuilder buf = new StringBuilder();
        while (reader.nextRecord() > 0) {
            for (String field : allFields.keySet()) {
                String val = reader.getValue(field);
                if (val != null && val.contains(","))
                    val = "\"" + quotePattern.matcher(val).replaceAll("\"\"") + "\"";
                buf.append(val == null ? "" : val).append(",");
            }
            buf.setLength(buf.length() - 1);
            writer.write(buf.toString());
            writer.write("\n");
            buf.setLength(0);
        }
    }

    public void cleanup() {
        if (!_csvFile.delete())
            System.err.println("!!! Unable to cleanup tmp CSV file.");
    }
}
