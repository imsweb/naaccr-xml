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
 * Use this class to convert a given NAACCR XML file into a (temp) fixed-columns file that SAS can easily import.
 * <br/><br/>
 * THIS CLASS IS IMPLEMENTED TO BE COMPATIBLE WITH JAVA 7; BE CAREFUL WHEN MODIFYING IT.
 */
@SuppressWarnings("ALL")
public class SasXmlToFlat {

    // the source XML file
    private File _xmlFile;

    // the target (temp) flat file
    private File _flatFile;

    // the target (temp) format file SAS needs to read the flat file
    private File _formatFile;

    // the (optional) user-provided dictionaries
    private List<File> _dictionaryFiles;

    // the (required) NAACCR version
    private String _naaccrVersion;

    // the (required) record type
    private String _recordType;

    // whether grouped items should be included (only applicable pre-N23)
    private boolean _includeGroupedItems;

    /**
     * Constructor.
     */
    public SasXmlToFlat(String xmlPath) {
        initFiles(xmlPath, false);
    }

    /**
     * Constructor.
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

    /**
     * Sets the user-defined dictionaries.
     */
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

    /**
     * Returns the path of the source XML file.
     */
    public String getXmlPath() {
        return _xmlFile.getAbsolutePath();
    }

    /**
     * Returns the path of the (temp) target flat file.
     */
    public String getFlatPath() {
        return _flatFile.getAbsolutePath();
    }

    /**
     * Returns the path of the (temp) format file.
     */
    public String getFormatPath() {
        return _formatFile.getAbsolutePath();
    }

    /**
     * Retruns the path of each user-defined dictionary, separated by a semi-colon.
     */
    public String getDictionaryPath() {
        StringBuilder buf = new StringBuilder();
        for (File dictionaryFile : _dictionaryFiles) {
            if (buf.length() > 0)
                buf.append(";");
            buf.append(dictionaryFile.getAbsolutePath());
        }
        return buf.toString();
    }

    /**
     * Returns the user-defined dictionaries.
     */
    List<File> getUserDictionaryFiles() {
        return _dictionaryFiles;
    }

    /**
     * Returns the NAACCR vesrion.
     */
    public String getNaaccrVersion() {
        return _naaccrVersion;
    }

    /**
     * Returns the record type.
     */
    public String getRecordType() {
        return _recordType;
    }

    /**
     * Returns the list of fields for the parameters set on the object.
     */
    public List<SasFieldInfo> getFields() {
        return SasUtils.getFields(_naaccrVersion, _recordType, _dictionaryFiles);
    }

    /**
     * Sets the include-grouped-items option.
     */
    public void setIncludeGroupedItems(String option) {
        if ("yes".equalsIgnoreCase(option))
            _includeGroupedItems = true;
        else if (!"no".equalsIgnoreCase(option))
            SasUtils.logError("Invalid includeGroupItems option: " + option);
    }

    /**
     * Returns the list of grouped fields for the parameters set on the object.
     */
    public List<SasFieldInfo> getGroupedFields() {
        return SasUtils.getGroupedFields(_naaccrVersion, _recordType);
    }

    /**
     * Creates the target temp flat file from the source XML file based on the parmaters set on the object.
     */
    public void convert() throws IOException {
        convert(null);
    }

    /**
     * Creates the target temp flat file from the source XML file based on the parmaters set on the object.
     */
    public void convert(String fields) throws IOException {
        convert(fields, getFields());
    }

    /**
     * Creates the target temp flat file from the source XML file based on the parmaters set on the object.
     */
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

            int expectedLineLength = 0;
            for (SasFieldInfo field : fieldsToWrite.values())
                expectedLineLength += field.getLength();

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
                SasUtils.logInfo("Successfully created input format file with " + fieldsToWrite.size() + " fields (variables) with total line length of " + expectedLineLength);
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

    /**
     * Cleans up temp files.
     */
    public void cleanup() {
        cleanup("yes");
    }

    /**
     * Cleans up temp files.
     */
    public void cleanup(String option) {
        if ("no".equalsIgnoreCase(option))
            SasUtils.logInfo("Skipping temp files cleanup...");
        else if (!_flatFile.delete() || !_formatFile.delete())
            SasUtils.logError("Unable to cleanup temp files, they will have to be manually deleted...");
        else
            SasUtils.logInfo("Successfully deleted temp files...");
    }
}
