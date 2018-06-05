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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class SasCsvToXml {

    private File _csvFile, _xmlFile;

    private String _naaccrVersion, _recordType;

    public SasCsvToXml(String xmlPath, String naaccrVersion, String recordType) {
        this(xmlPath.replace(".xml", ".csv"), xmlPath, naaccrVersion, recordType);
    }

    public SasCsvToXml(String csvPath, String xmlPath, String naaccrVersion, String recordType) {
        _xmlFile = new File(xmlPath);
        if (!_xmlFile.getParentFile().exists())
            System.err.println("!!! Invalid XML path (parent folder doesn't exist): " + xmlPath);
        System.out.println(" > target XML: " + _xmlFile.getAbsolutePath());

        if (csvPath.endsWith(".gz"))
            csvPath = csvPath.replace(".gz", "");
        _csvFile = new File(csvPath);
        System.out.println(" > temp CSV: " + _csvFile.getAbsolutePath());

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

        List<String> rootFields = new ArrayList<>(), patientFields = new ArrayList<>(), tumorFields = new ArrayList<>();
        for (Map.Entry<String, String> entry : SasUtils.getFields(_naaccrVersion, _recordType).entrySet()) {
            if ("NaaccrData".equals(entry.getValue()))
                rootFields.add(entry.getKey());
            else if ("Patient".equals(entry.getValue()))
                patientFields.add(entry.getKey());
            else if ("Tumor".equals(entry.getValue()))
                tumorFields.add(entry.getKey());

        }

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(_csvFile), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_xmlFile), StandardCharsets.UTF_8));

            Pattern p = Pattern.compile(",");

            List<String> headers = new ArrayList<>();
            for (String s : p.split(reader.readLine()))
                headers.add(s);

            int patNumIdx = headers.indexOf("patientIdNumber");
            if (patNumIdx == -1)
                throw new IOException("Unable to find 'patientIdNumber' in the headers");

            String currentPatNum = null;
            String line = reader.readLine();
            while (line != null) {
                String[] valArray = p.split(line);
                if (valArray.length != headers.size())
                    throw new IOException("Got a row of size " + valArray.length + " but expected " + headers.size());
                Map<String, String> values = new HashMap<>();
                for (int i = 0; i < valArray.length; i++)
                    values.put(headers.get(i), valArray[i]);

                if (currentPatNum == null) {
                    // write root
                }

                String patNum = values.get("patientIdNumber");

                // TODO look at logic of tagsets and apply it here

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

    public void cleanup() {
        if (!_csvFile.delete())
            System.err.println("!!! Unable to cleanup tmp CSV file.");
    }
}
