/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Use this class to convert a given NAACCR XML file into a CSV file.
 * <br/><br/>
 * THIS CLASS IS IMPLEMENTED TO BE COMPATIBLE WITH JAVA 7; BE CAREFUL WHEN MODIFYING IT.
 */
@SuppressWarnings("ALL")
public class SasXmlToCsv {

    private File _xmlFile, _csvFile, _dictFile;

    private String _naaccrVersion, _recordType;

    public SasXmlToCsv(String xmlPath, String naaccrVersion, String recordType) {
        this(xmlPath, xmlPath.replace(".xml", ".csv"), naaccrVersion, recordType);
    }

    public SasXmlToCsv(String xmlPath, String csvPath, String naaccrVersion, String recordType) {
        this(xmlPath, xmlPath.replace(".xml", ".csv"), naaccrVersion, recordType, null);
    }

    public SasXmlToCsv(String xmlPath, String csvPath, String naaccrVersion, String recordType, String dictPath) {
        _xmlFile = new File(xmlPath);
        if (!_xmlFile.exists())
            System.err.println("!!! Invalid XML file: " + xmlPath);
        else
            System.out.println(" > input XML: " + _xmlFile.getAbsolutePath());

        if (csvPath.endsWith(".gz"))
            csvPath = csvPath.replace(".gz", "");
        _csvFile = new File(csvPath);
        System.out.println(" > temp CSV: " + _csvFile.getAbsolutePath());

        if (dictPath != null && !dictPath.trim().isEmpty()) {
            _dictFile = new File(dictPath);
            if (!_dictFile.exists())
                System.err.println("!!! Invalid CSV dictionary " + dictPath);
            else
                System.out.println(" > dictionary: " + _dictFile.getAbsolutePath());
        }

        _naaccrVersion = naaccrVersion;
        _recordType = recordType;

    }

    public String getXmlPath() {
        return _xmlFile.getAbsolutePath();
    }

    public String getCsvPath() {
        return _csvFile.getAbsolutePath();
    }

    public String getDictionaryPath() {
        return _dictFile.getAbsolutePath();
    }

    public String getNaaccrVersion() {
        return _naaccrVersion;
    }

    public String getRecordType() {
        return _recordType;
    }

    public List<SasFieldInfo> getFields() {
        return SasUtils.getFields(_naaccrVersion, _recordType, _dictFile);
    }

    public void convert() throws IOException {
        convert(null);
    }

    public void convert(String fields) throws IOException {
        convert(fields, true);
    }

    public void convert(String fields, boolean addExtraCharFields) throws IOException {
        try {
            Set<String> requestedFields = null;
            if (fields != null && !fields.trim().isEmpty()) {
                requestedFields = new HashSet<>();
                for (String s : fields.replace(" ", "").split(",", -1))
                    requestedFields.add(s);
            }

            Map<String, Integer> allFields = new LinkedHashMap<>();
            for (SasFieldInfo field : getFields()) {
                if (requestedFields == null || requestedFields.contains(field.getNaaccrId())) {
                    allFields.put(field.getTruncatedNaaccrId(), field.getLength());
                    if (!field.getNaaccrId().equals(field.getTruncatedNaaccrId()))
                        System.out.println("Truncated '" + field.getNaaccrId() + "' into '" + field.getTruncatedNaaccrId() + "'...");
                }
            }

            System.out.println("Starting converting XML to CSV...");

            SasXmlReader reader = null;
            BufferedWriter writer = null;
            try {
                reader = new SasXmlReader(_xmlFile.getPath());
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_csvFile), StandardCharsets.UTF_8));
                StringBuilder buf = new StringBuilder();

                // write the headers
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

                Pattern quotePattern = Pattern.compile("\"", Pattern.LITERAL);
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

        System.out.println("Successfully created " + _csvFile.getAbsolutePath());
    }

    public void cleanup() {
        if (!_csvFile.delete())
            System.err.println("!!! Unable to cleanup tmp CSV file.");
    }
}
