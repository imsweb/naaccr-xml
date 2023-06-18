/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Use this class to convert a given NAACCR XML file into a CSV file.
 * <br/><br/>
 * THIS CLASS IS IMPLEMENTED TO BE COMPATIBLE WITH JAVA 7; BE CAREFUL WHEN MODIFYING IT.
 */
@SuppressWarnings("ALL")
public class SasXmlToFlat {

    private File _xmlFile;

    private File _flatFile;

    private File _formatFile;

    private List<File> _dictionaryFiles;

    private String _naaccrVersion;

    private String _recordType;

    private boolean _includeGroupedItems;

    /**
     * This constructor is used to do the full conversion.
     * @param xmlPath path to the XML file to convert
     * @param naaccrVersion NAACCR version
     * @param recordType record type
     */
    public SasXmlToFlat(String xmlPath, String naaccrVersion, String recordType) {
        initFiles(xmlPath, true);

        _dictionaryFiles = new ArrayList<>();

        _naaccrVersion = naaccrVersion;
        if (_naaccrVersion == null || _naaccrVersion.trim().isEmpty())
            SasUtils.logError("NAACCR version needs to be provided");
        if (!"140".equals(naaccrVersion) && !"150".equals(naaccrVersion) && !"160".equals(naaccrVersion) && !"180".equals(naaccrVersion) && !"210".equals(naaccrVersion) && !"220".equals(
                naaccrVersion) && !"230".equals(naaccrVersion))
            SasUtils.logError("NAACCR version must be 140, 150, 160, 180, 210, 220 or 230; got " + _naaccrVersion);

        _recordType = recordType;
        if (_recordType == null || _recordType.trim().isEmpty())
            SasUtils.logError("Record type needs to be provided");
        if (!"A".equals(_recordType) && !"M".equals(_recordType) && !"C".equals(_recordType) && !"I".equals(_recordType))
            SasUtils.logError("Record type must be A, M, C or I; got " + _recordType);

        _includeGroupedItems = false;
    }

    /**
     * This constructor is used to cleanup the temp file.
     * @param xmlPath path to the XML file that was converted
     */
    public SasXmlToFlat(String xmlPath) {
        initFiles(xmlPath, false);
    }

    private void initFiles(String xmlPath, boolean logInfo) {
        if (xmlPath == null || xmlPath.trim().isEmpty())
            SasUtils.logError("No source XML path was provided");
        else {
            _xmlFile = new File(xmlPath);
            if (!_xmlFile.exists())
                SasUtils.logError("Invalid source XML file: " + xmlPath);
            else if (logInfo)
                SasUtils.logInfo("Source XML: " + _xmlFile.getAbsolutePath());

            _flatFile = new File(SasUtils.computeFlatPathFromXmlPath(xmlPath));
            if (!new File(_flatFile.getAbsolutePath()).getParentFile().exists())
                SasUtils.logError("Parent directory for target XML file doesn't exist: " + _flatFile.getParentFile().getAbsolutePath());
            else if (logInfo)
                SasUtils.logInfo("Target temp flat: " + _flatFile.getAbsolutePath());

            _formatFile = new File(SasUtils.computeInputPathFromXmlPath(xmlPath));
            if (new File(_formatFile.getAbsolutePath()).getParentFile().exists() && logInfo)
                SasUtils.logInfo("Target input format: " + _formatFile.getAbsolutePath());
        }
    }

    public void setDictionary(String dictionaryPath) {
        if (dictionaryPath != null && !dictionaryPath.trim().isEmpty()) {
            for (String path : dictionaryPath.split(";")) {
                File dictionaryFile = new File(path.trim());
                if (!dictionaryFile.exists())
                    SasUtils.logError("Invalid CSV dictionary path: " + path);
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
    }

    public String getXmlPath() {
        return _xmlFile.getAbsolutePath();
    }

    public String getFlatPath() {
        return _flatFile.getAbsolutePath();
    }

    public String getFormatPath() {
        return _formatFile.getAbsolutePath();
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

    public List<SasFieldInfo> getGroupedFields() {
        return SasUtils.getGroupedFields(_naaccrVersion, _recordType);
    }

    public void convert() throws IOException {
        convert(null);
    }

    public void convert(String fields) throws IOException {
        convert(fields, getFields());
    }

    public void convert(String fields, List<SasFieldInfo> availableFields) throws IOException {
        try {
            Set<String> requestedFieldIds = SasUtils.extractRequestedFields(fields, availableFields);

            Map<String, SasFieldInfo> fieldsToWrite = new TreeMap<>();
            for (SasFieldInfo field : availableFields) {
                if (requestedFieldIds == null || requestedFieldIds.contains(field.getNaaccrId())) {
                    fieldsToWrite.put(field.getNaaccrId(), field);
                    if (!field.getNaaccrId().equals(field.getTruncatedNaaccrId()))
                        SasUtils.logInfo("Truncated '" + field.getNaaccrId() + "' into '" + field.getTruncatedNaaccrId() + "'...");
                }
            }

            Map<String, SasFieldInfo> allFields = null;
            if (_includeGroupedItems) {
                for (SasFieldInfo groupedField : getGroupedFields())
                    fieldsToWrite.put(groupedField.getNaaccrId(), groupedField);

                allFields = new HashMap<>();
                for (SasFieldInfo field : availableFields)
                    allFields.put(field.getNaaccrId(), field);
            }

            SasUtils.logInfo("Generating input format file...");
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_formatFile), StandardCharsets.US_ASCII));
                writer.write("input\n");
                int counter = 1;
                for (Entry<String, SasFieldInfo> entry : fieldsToWrite.entrySet()) {
                    writer.write("@" + counter + " " + entry.getKey() + " $" + entry.getValue().getLength() + ".\n");
                    counter += entry.getValue().getLength();
                }
                writer.write(";");
                SasUtils.logInfo("Successfully created input format file with " + fieldsToWrite.size() + " fields (variables)");
            }
            finally {
                if (writer != null)
                    writer.close();
            }

            SasUtils.logInfo("Starting converting XML to flat...");
            writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_flatFile), StandardCharsets.UTF_8));

                int count = 0;
                if (_xmlFile.getName().toLowerCase().endsWith(".zip")) {
                    ZipFile zipFile = null;
                    ZipInputStream zipIs = null;
                    try {
                        zipFile = new ZipFile(_xmlFile);
                        zipIs = new ZipInputStream(new FileInputStream(_xmlFile));
                        ZipEntry entry = zipIs.getNextEntry();
                        while (entry != null) {
                            SasXmlReader reader = new SasXmlReader(SasUtils.createReader(zipFile.getInputStream(entry), entry.getName()));
                            count += convertSingleFile(reader, writer, fieldsToWrite, allFields);
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
                        count += convertSingleFile(reader, writer, fieldsToWrite, allFields);
                    }
                    finally {
                        if (reader != null)
                            reader.close();
                    }
                }

                SasUtils.logInfo("Successfully created flat file with " + count + " lines (observations)");
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
    }

    private int convertSingleFile(SasXmlReader reader, BufferedWriter writer, Map<String, SasFieldInfo> fieldsToWrite, Map<String, SasFieldInfo> allFields) throws IOException {
        int count = 0;

        StringBuilder buf = new StringBuilder();
        while (reader.nextRecord() > 0) {
            for (Entry<String, SasFieldInfo> entry : fieldsToWrite.entrySet()) {
                String val = null;
                if (entry.getValue().getContains() == null)
                    val = reader.getValue(entry.getKey());
                else {
                    StringBuilder childrenValue = new StringBuilder();
                    for (String child : entry.getValue().getContains()) {
                        SasFieldInfo childField = allFields.get(child);
                        if (childField == null)
                            throw new IOException("Unable to find field definition for '" + child + "'");
                        String childValue = reader.getValue(child);
                        if (childValue == null)
                            childValue = "";
                        childrenValue.append(childValue);
                        for (int i = 0; i < childField.getLength() - childValue.length(); i++)
                            childrenValue.append(" ");
                    }
                    val = childrenValue.toString();
                }
                buf.append(SasUtils.rightPadWithSpaces(val, entry.getValue().getLength()));
            }
            writer.write(buf.toString());
            writer.write("\n");
            buf.setLength(0);
            count++;
        }

        return count;
    }

    public void cleanup() {
        cleanup("yes");
    }

    public void cleanup(String option) {
        if ("no".equalsIgnoreCase(option))
            SasUtils.logInfo("Skipping temp files cleanup...");
        else if (!_flatFile.delete() || !_formatFile.delete())
            SasUtils.logError("Unable to cleanup temp files, they will have to be manually deleted...");
        else
            SasUtils.logInfo("Successfully deleted temp files...");
    }

    List<File> getUserDictionaryFiles() {
        return _dictionaryFiles;
    }

    public void setIncludeGroupedItems(String option) {
        if ("yes".equalsIgnoreCase(option))
            _includeGroupedItems = true;
        else if (!"no".equalsIgnoreCase(option))
            SasUtils.logError("Invalid includeGroupItems option: " + option);
    }
}
