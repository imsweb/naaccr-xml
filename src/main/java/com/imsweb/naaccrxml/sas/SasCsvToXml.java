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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Use this class to convert a given CSV file into a NAACCR XML file.
 */
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
        return SasUtils.getFields(_naaccrVersion, _recordType);
    }

    public void convert() throws IOException {
        convert(null);
    }

    public void convert(String fields) throws IOException {
        System.out.println("Starting converting CSV to XML...");
        try {
            Set<String> requestedFields = null;
            if (fields != null && !fields.trim().isEmpty()) {
                requestedFields = new HashSet<>();
                for (String s : fields.split(","))
                    requestedFields.add(s.trim());
            }

            List<String> rootFields = new ArrayList<>(), patientFields = new ArrayList<>(), tumorFields = new ArrayList<>();
            for (SasFieldInfo field : getFields()) {
                if (requestedFields == null || requestedFields.contains(field.getNaaccrId())) {
                    if ("NaaccrData".equals(field.getParentTag()))
                        rootFields.add(field.getNaaccrId());
                    else if ("Patient".equals(field.getParentTag()))
                        patientFields.add(field.getNaaccrId());
                    else if ("Tumor".equals(field.getParentTag()))
                        tumorFields.add(field.getNaaccrId());
                }
            }

            LineNumberReader reader = null;
            BufferedWriter writer = null;
            try {
                reader = new LineNumberReader(new InputStreamReader(new FileInputStream(_csvFile), StandardCharsets.UTF_8));
                writer = SasUtils.createWriter(_xmlFile);

                Pattern p = Pattern.compile(",");

                List<String> headers = new ArrayList<>();
                headers.addAll(Arrays.asList(p.split(reader.readLine())));

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
                        writer.write("<NaaccrData");
                        writer.write(" baseDictionaryUri=\"http://naaccr.org/naaccrxml/naaccr-dictionary-" + _naaccrVersion + ".xml\"");
                        writer.write(" recordType=\"" + _recordType + "\"");
                        writer.write(" specificationVersion=\"1.3\"");
                        writer.write(" xmlns=\"http://naaccr.org/naaccrxml\"");
                        writer.write(">\n");
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
