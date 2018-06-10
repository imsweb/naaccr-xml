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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Use this class to convert a given NAACCR XML file into a CSV file.
 */
public class SasXmlToCsv {

    private File _xmlFile, _csvFile;

    private String _naaccrVersion, _recordType;

    public SasXmlToCsv(String xmlPath, String naaccrVersion, String recordType) {
        this(xmlPath, xmlPath.replace(".xml", ".csv"), naaccrVersion, recordType);
    }

    public SasXmlToCsv(String xmlPath, String csvPath, String naaccrVersion, String recordType) {
        _xmlFile = new File(xmlPath);
        if (!_xmlFile.exists())
            System.err.println("!!! Invalid XML file: " + xmlPath);
        System.out.println(" > input XML: " + _xmlFile.getAbsolutePath());

        if (csvPath.endsWith(".gz"))
            csvPath = csvPath.replace(".gz", "");
        _csvFile = new File(csvPath);
        System.out.println(" > temp CSV: " + _csvFile.getAbsolutePath());

        _naaccrVersion = naaccrVersion;
        _recordType = recordType;
    }

    public String getXmlPath() {
        return _xmlFile.getPath();
    }

    public String getCsvPath() {
        return _csvFile.getPath();
    }

    public String getNaaccrVersion() {
        return _naaccrVersion;
    }

    public String getRecordType() {
        return _recordType;
    }

    public List<SasFieldInfo> getFields() {
        return SasUtils.getFields(_recordType, _naaccrVersion);
    }

    public void convert() throws IOException {
        convert(null);
    }

    public void convert(String fields) throws IOException {
        convert(fields, true);
    }

    public void convert(String fields, boolean addExtraCharFields) throws IOException {

        Set<String> requestedFields = null;
        if (fields != null) {
            requestedFields = new HashSet<>();
            for (String s : fields.split(","))
                requestedFields.add(s.trim());
        }

        List<String> allFields = new ArrayList<>();
        for (SasFieldInfo field : getFields())
            if (requestedFields == null || requestedFields.contains(field.getNaaccrId()))
                allFields.add(field.getNaaccrId());

        SasXmlReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new SasXmlReader(_xmlFile.getPath());
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_csvFile), StandardCharsets.UTF_8));
            StringBuilder buf = new StringBuilder();

            // write the headers
            for (String field : allFields)
                buf.append(field).append(",");
            buf.setLength(buf.length() - 1);
            writer.write(buf.toString());
            writer.write("\n");
            buf.setLength(0);

            // hack alert - force SAS to recognize all variables as characters...  Sigh...
            if (addExtraCharFields) {
                for (String field : allFields)
                    buf.append("-,");
                buf.setLength(buf.length() - 1);
                writer.write(buf.toString());
                writer.write("\n");
                buf.setLength(0);
            }

            while (reader.nextRecord() > 0) {
                for (String field : allFields) {
                    String val = reader.getValue(field);
                    if (val != null && val.contains(","))
                        val = "\"" + val + "\"";
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

    public void cleanup() {
        if (!_csvFile.delete())
            System.err.println("!!! Unable to cleanup tmp CSV file.");
    }
}
