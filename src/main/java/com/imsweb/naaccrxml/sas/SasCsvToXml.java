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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

// TODO looks like SAS removes leading 0's of all values!
public class SasCsvToXml {

    private File _csvFile, _xmlFile;

    private String _naaccrVersion, _recordType;

    public SasCsvToXml(String xmlPath, String naaccrVersion, String recordType) {
        this(xmlPath.replace(".xml", ".csv"), xmlPath, naaccrVersion, recordType);
    }

    public SasCsvToXml(String csvPath, String xmlPath, String naaccrVersion, String recordType) {
        _xmlFile = new File(xmlPath);
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

        // TODO use requested fields
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

        LineNumberReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new LineNumberReader(new InputStreamReader(new FileInputStream(_csvFile), StandardCharsets.UTF_8));
            writer = SasUtils.createWriter(_xmlFile);

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
                Map<String, String> values = new HashMap<>();
                for (int i = 0; i < valArray.length; i++)
                    values.put(headers.get(i), valArray[i]);

                String patNum = values.get("patientIdNumber");
                if (patNum == null)
                    throw new IOException("Line " + reader.getLineNumber() + ": patient ID Number is required to write XML files");

                // do we have to write the root?
                if (currentPatNum == null) {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    writer.write("\n");
                    writer.write("<NaaccrData baseDictionaryUri=\"http://naaccr.org/naaccrxml/naaccr-dictionary-" + _naaccrVersion + ".xml\" recordType=\"" + _recordType
                            + "\" xmlns=\"http://naaccr.org/naaccrxml\">\n");
                    for (String id : rootFields) {
                        String val = values.get(id);
                        if (val != null && !val.trim().isEmpty())
                            writer.write("    <Item naaccrId=\"" + id + "\">" + val + "</Item>\n");
                    }
                }

                // do we have to write the patient?
                if (currentPatNum == null || !currentPatNum.equals(patNum)) {
                    if (currentPatNum != null)
                        writer.write("    </Patient>\n");
                    writer.write("    <Patient>\n");
                    for (String id : patientFields) {
                        String val = values.get(id);
                        if (val != null && !val.trim().isEmpty())
                            writer.write("        <Item naaccrId=\"" + id + "\">" + val + "</Item>\n");
                    }
                }

                // we always have to write the tumor!
                writer.write("        <Tumor>\n");
                for (String id : tumorFields) {
                    String val = values.get(id);
                    if (val != null && !val.trim().isEmpty())
                        writer.write("            <Item naaccrId=\"" + id + "\">" + val + "</Item>\n");
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

    public void cleanup() {
        if (!_csvFile.delete())
            System.err.println("!!! Unable to cleanup tmp CSV file.");
    }

    /**
    public static void main(String[] args) throws IOException {
        String xmlPath = "D:\\Users\\depryf\\dev\\synthetic-data_naaccr-18-incidence_100-recs-copy.xml";
        String csvPath = "D:\\Users\\depryf\\dev\\synthetic-data_naaccr-18-incidence_100-recs-copy.csv";

        long start = System.currentTimeMillis();
        SasCsvToXml converter = new SasCsvToXml(csvPath, xmlPath, "180", "I");
        converter.convert();
        //reader.cleanup();
        System.out.println((System.currentTimeMillis() - start) + "ms");
    }
     */
}
