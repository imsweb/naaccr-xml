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

import org.apache.commons.lang3.StringUtils;

/**
 * Use this class to convert a given CSV file into a NAACCR XML file.
 */
public class SasCsvToXml {

    private static final Map<String, String> _TO_ESCAPE = new HashMap<>();

    static {
        _TO_ESCAPE.put("&", "&amp;");
        _TO_ESCAPE.put("<", "&lt;");
        _TO_ESCAPE.put(">", "&gt;");
        _TO_ESCAPE.put("\"", "&quot;");
        _TO_ESCAPE.put("'", "&apos;'");
        // not really a special character, but behaves like one (to handle new lines)
        _TO_ESCAPE.put("::", "\n");
    }

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

                List<String> headers = new ArrayList<>();
                headers.addAll(Arrays.asList(StringUtils.split(reader.readLine(), ',')));

                int patNumIdx = headers.indexOf("patientIdNumber");
                if (patNumIdx == -1)
                    throw new IOException("Unable to find 'patientIdNumber' in the headers");

                String currentPatNum = null;
                String line = reader.readLine();
                while (line != null) {
                    List<String> valList = parseCsvLine(reader.getLineNumber(), line, '\"', ',');
                    if (headers.size() != valList.size())
                        throw new IOException("Line " + reader.getLineNumber() + ": expected " + headers.size() + " values but got " + valList.size());

                    Map<String, String> values = new HashMap<>();
                    for (int i = 0; i < valList.size(); i++)
                        values.put(headers.get(i), valList.get(i));

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
                                writer.write("    <Item naaccrId=\"" + id + "\">" + cleanUpValue(val) + "</Item>\n");
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
                                writer.write("        <Item naaccrId=\"" + id + "\">" + cleanUpValue(val) + "</Item>\n");
                        }
                    }

                    // we always have to write the tumor?
                    writer.write("        <Tumor>\n");
                    for (String id : tumorFields) {
                        String val = values.get(id);
                        if (val != null && !val.trim().isEmpty())
                            writer.write("            <Item naaccrId=\"" + id + "\">" + cleanUpValue(val) + "</Item>\n");
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

    private String cleanUpValue(String value) {
        StringBuilder buf = new StringBuilder(value);
        for (Map.Entry<String, String> entry : _TO_ESCAPE.entrySet()) {
            int idx = buf.indexOf(entry.getKey());
            while (idx != -1) {
                buf.replace(idx, idx + entry.getKey().length(), entry.getValue());
                idx = buf.indexOf(entry.getKey(), idx + 1);
            }
        }
        return buf.toString();
    }

    protected List<String> parseCsvLine(int lineNumber, String line, char cQuote, char cDelimiter) throws IOException {
        List<String> result = new ArrayList<>();

        int curIndex = 0, nextQuote, nextDelimiter;

        StringBuilder buf = new StringBuilder();
        buf.append(cQuote);
        String singleQuotes = buf.toString();
        buf.append(cQuote);
        String doubleQuotes = buf.toString();

        String value;
        while (curIndex < line.length()) {
            if (line.charAt(curIndex) == cQuote) {
                // handle quoted value
                nextQuote = getNextSingleQuote(line, cQuote, curIndex);
                if (nextQuote < 0)
                    throw new IOException("Line " + lineNumber + ": found an unmatched quote");
                else {
                    result.add(line.substring(curIndex + 1, nextQuote).replace(doubleQuotes, singleQuotes));
                    // update the current index to be after delimiter, after the ending quote
                    curIndex = nextQuote;
                    if (curIndex + 1 < line.length()) {
                        // if there is a next value, set current index to be after delimiter
                        if (line.charAt(curIndex + 1) == cDelimiter) {
                            curIndex += 2;
                            // handle case where last value is empty
                            if (curIndex == line.length())
                                result.add("");
                        }
                        // else character after ending quote is not EOL and not delimiter, stop parsing
                        else
                            throw new IOException("Line " + lineNumber + ": expected a delimiter after the quote");
                    }
                    else
                        // end of line is after ending quote, stop parsing
                        curIndex++;
                }
            }
            else {
                // handle unquoted value
                nextDelimiter = getNextDelimiter(line, cDelimiter, curIndex);
                value = line.substring(curIndex, nextDelimiter).replace(doubleQuotes, singleQuotes);
                // unquoted values should not contain any quotes
                if (value.contains(singleQuotes))
                    throw new IOException("Line " + lineNumber + ": value contains some quotes but does not start with a quote");
                else {
                    result.add(value);
                    curIndex = nextDelimiter + 1;
                    // handle case where last value is empty
                    if (curIndex == line.length())
                        result.add("");
                }
            }
        }

        return result;
    }

    private int getNextSingleQuote(String line, char quote, int from) {
        if (from >= line.length())
            return -1;

        int index = from + 1;
        boolean found = false;
        while ((index < line.length()) && !found) {
            if (line.charAt(index) != quote)
                index++;
            else {
                if ((index + 1 == line.length()) || (line.charAt(index + 1) != quote))
                    found = true;
                else
                    index += 2;
            }

        }

        index = (index == line.length()) ? -1 : index;

        return index;
    }

    private int getNextDelimiter(String line, char delimiter, int from) {
        if (from >= line.length())
            return line.length();

        int index = from;
        while ((index < line.length()) && (line.charAt(index) != delimiter))
            index++;

        return index;
    }
}
