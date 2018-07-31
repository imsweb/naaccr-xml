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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility methods used to read/write NAACCR XML data files to/from SAS.
 */
public class SasUtils {

    /**
     * Creates a reader from the given file. Support GZIP compressed files.
     * @param file file to read
     * @return reader
     */
    public static BufferedReader createReader(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        if (file.getName().toLowerCase().endsWith(".gz"))
            is = new GZIPInputStream(is);
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    /**
     * Creates a writer from the given file. Supports GZIP compressed files.
     * @param file file to write
     * @return writer
     */
    public static BufferedWriter createWriter(File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        if (file.getName().toLowerCase().endsWith(".gz"))
            os = new GZIPOutputStream(os);
        return new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    }

    /**
     * Returns the fields information for the given parameters.
     * @param version NAACCR version
     * @param recordType record type
     * @param dictionary user-defined dictionary in CSV format (see standard ones in docs folder)
     * @return fields information
     */
    public static List<SasFieldInfo> getFields(String version, String recordType, File dictionary) {
        return getFields(recordType, Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-xml-items-" + version + ".csv"), dictionary);
    }

    /**
     * Returns the fields information for the given parameters.
     * <br/><br/>
     * Note that some NAACCR ID are too long for SAS; this method cuts-off any ID that is longer than 32 characters and
     * add a numeric suffix to ensure unicity.
     * @param recordType record type
     * @param is input stream to the standard dictionary in CSV format
     * @param dictionary user-defined dictionary in CSV format (see standard ones in docs folder)
     * @return fields information
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static List<SasFieldInfo> getFields(String recordType, InputStream is, File dictionary) {
        Map<String, AtomicInteger> counters = new HashMap<>();

        List<SasFieldInfo> result = readCsvDictionary(recordType, is, counters);
        if (dictionary != null) {
            InputStream dictIs = null;
            try {
                dictIs = new FileInputStream(dictionary);
                result.addAll(readCsvDictionary(recordType, dictIs, counters));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
                if (dictIs != null) {
                    try {
                        dictIs.close();
                    }
                    catch (IOException e) {
                        // ignored
                    }
                }
            }
        }

        return result;
    }

    private static List<SasFieldInfo> readCsvDictionary(String recordType, InputStream is, Map<String, AtomicInteger> counters) {
        List<SasFieldInfo> result = new ArrayList<>();

        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new InputStreamReader(is, StandardCharsets.US_ASCII));
            reader.readLine(); // ignore headers
            String line = reader.readLine();
            while (line != null) {
                List<String> values = parseCsvLine(reader.getLineNumber(), line);

                Integer num = values.get(0).isEmpty() ? null : Integer.valueOf(values.get(0));
                String name = values.get(1);
                Integer start = values.get(2).isEmpty() ? null : Integer.valueOf(values.get(2));
                Integer length = values.get(3).isEmpty() ? null : Integer.valueOf(values.get(3));
                String recTypes = values.get(4);
                String naaccrId = values.get(5);
                String parentTag = values.get(6);

                String truncatedNaaccrId = naaccrId;
                if (truncatedNaaccrId.length() > 32) {
                    String prefix = truncatedNaaccrId.substring(0, 30);
                    AtomicInteger counter = counters.get(prefix);
                    if (counter == null) {
                        counter = new AtomicInteger();
                        counters.put(prefix, counter);
                    }
                    truncatedNaaccrId = prefix + "_" + counter.getAndIncrement();
                }

                if (recTypes.contains(recordType))
                    result.add(new SasFieldInfo(naaccrId, truncatedNaaccrId, parentTag, length, num, name, start));

                line = reader.readLine();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    // ignored
                }
            }
        }

        return result;
    }

    public static List<String> parseCsvLine(int lineNumber, String line) throws IOException {
        List<String> result = new ArrayList<>();

        char cQuote = '"', cDelimiter = ',';
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

    private static int getNextSingleQuote(String line, char quote, int from) {
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

    private static int getNextDelimiter(String line, char delimiter, int from) {
        if (from >= line.length())
            return line.length();

        int index = from;
        while ((index < line.length()) && (line.charAt(index) != delimiter))
            index++;

        return index;
    }
}
