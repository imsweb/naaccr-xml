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
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class SasXmlToFixed {

    private File _xmlFile, _fixedFile;

    private String _naaccrVersion, _recordType;

    public SasXmlToFixed(String xmlPath, String naaccrVersion, String recordType) {
        this(xmlPath, xmlPath.replace(".xml", ".txd"), naaccrVersion, recordType);
    }

    public SasXmlToFixed(String xmlPath, String fixedPath, String naaccrVersion, String recordType) {
        _xmlFile = new File(xmlPath);
        if (!_xmlFile.exists())
            System.err.println("!!! Invalid XML file: " + xmlPath);
        System.out.println(" > input XML: " + _xmlFile.getAbsolutePath());

        if (!fixedPath.endsWith(".gz"))
            fixedPath = fixedPath + ".gz";
        _fixedFile = new File(fixedPath);
        System.out.println(" > temp Fixed: " + _fixedFile.getAbsolutePath());

        _naaccrVersion = naaccrVersion;
        _recordType = recordType;
    }

    public String getXmlPath() {
        return _xmlFile.getPath();
    }

    public String getFixedPath() {
        return _fixedFile.getPath();
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

        Map<String, Integer> lengths = SasUtils.getFieldLengths(_naaccrVersion, _recordType);

        SasXmlReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new SasXmlReader(_xmlFile.getPath(), _naaccrVersion, _recordType);
            writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(_fixedFile)), StandardCharsets.UTF_8));

            StringBuilder buf = new StringBuilder();
            while (reader.nextRecord() > 0) {
                for (Map.Entry<String, Integer> entry : lengths.entrySet()) {

                    String val = null;
                    if (requestedFields == null || requestedFields.contains(entry.getKey()))
                        val = reader.getValue(entry.getKey());
                    if (val == null)
                        val = "";
                    buf.append(val);
                    for (int i = val.length(); i < entry.getValue(); i++)
                        buf.append(" ");

                }
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
        if (!_fixedFile.delete())
            System.err.println("!!! Unable to cleanup tmp Fixed file.");
    }

     public static void main(String[] args) throws IOException {
     String xmlPath = "D:\\Users\\depryf\\Documents\\synthetic-data_naaccr-16-abstract_100-recs.xml.gz";

     long start = System.currentTimeMillis();
     SasXmlToFixed reader = new SasXmlToFixed(xmlPath, "160", "A");
     reader.convert();
     //reader.cleanup();
     System.out.println((System.currentTimeMillis() - start) + "ms");
     }
}
