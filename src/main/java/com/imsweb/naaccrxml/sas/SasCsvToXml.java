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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class SasCsvToXml {

    private File _csvFile, _xmlFile;

    private String _naaccrVersion, _recordType;

    public SasCsvToXml(String csvPath, String naaccrVersion, String recordType) {
        this(csvPath, csvPath.replace(".csv", ".xml"), naaccrVersion, recordType);
    }

    public SasCsvToXml(String csvPath, String xmlPath, String naaccrVersion, String recordType) {
        _csvFile = new File(xmlPath);
        if (!_csvFile.exists())
            System.err.println("!!! Invalid CSV file: " + csvPath);
        System.out.println(" > input CSV: " + _csvFile.getAbsolutePath());

        _xmlFile = new File(xmlPath);
        System.out.println(" > output XML: " + _xmlFile.getAbsolutePath());

        _naaccrVersion = naaccrVersion;
        _recordType = recordType;
    }

    public String getCsvPath() {
        return _csvFile.getPath();
    }

    public String getXmlPath() {
        return _xmlFile.getPath();
    }

    public String getNaaccrVersion() {
        return _naaccrVersion;
    }

    public String getRecordType() {
        return _recordType;
    }

    public void convert() throws IOException {
        convert(null);
    }

    public void convert(String fields) throws IOException {

        Set<String> requestedFields = null;
        if (fields != null) {
            requestedFields = new HashSet<>();
            for (String s : fields.split(","))
                requestedFields.add(s.trim());
        }

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(_csvFile), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_xmlFile), StandardCharsets.UTF_8));

            Pattern p = Pattern.compile(",");

            String line = reader.readLine();
            while (line != null) {
                String[] values = p.split(line);


                line = reader.readLine();
            }
        }
        finally {
            if (writer != null)
                writer.close();
            if (reader != null)
                reader.close();
        }
    }
}
