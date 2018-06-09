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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SasUtils {

    public static BufferedReader createReader(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        if (file.getName().endsWith(".gz"))
            is = new GZIPInputStream(is);
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    public static BufferedWriter createWriter(File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        if (file.getName().endsWith(".gz"))
            os = new GZIPOutputStream(os);
        return new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    }

    public static Map<String, String> getFields(String version, String recordType) {
        Map<String, String> result = new LinkedHashMap<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-xml-items-" + version + ".csv"), StandardCharsets.US_ASCII));
            //reader = new BufferedReader(new FileReader("D:\\Users\\depryf\\dev\\projects_github\\naaccr-xml\\docs\\naaccr-xml-items-" + version + ".csv"));

            Map<String, AtomicInteger> counters = new HashMap<>();

            reader.readLine();
            String line = reader.readLine();
            while (line != null) {
                int idx2 = line.lastIndexOf('"');
                int idx1 = line.lastIndexOf('"', idx2 - 1);
                int idx3 = line.lastIndexOf(',');
                String recTypes = line.substring(idx1 + 1, idx2);
                String naaccrId = line.substring(idx2 + 2, idx3);
                String parentTag = line.substring(idx3 + 1);

                if (naaccrId.length() > 32) {
                    String prefix = naaccrId.substring(0, 30);
                    AtomicInteger counter = counters.get(prefix);
                    if (counter == null) {
                        counter = new AtomicInteger();
                        counters.put(prefix, counter);
                    }
                    naaccrId = prefix + "_" + counter.getAndIncrement();
                }

                if (!naaccrId.startsWith("reserved") && recTypes.contains(recordType))
                    result.put(naaccrId, parentTag);

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

    public static Map<String, Integer> getFieldLengths(String version, String recordType) {
        Map<String, Integer> result = new LinkedHashMap<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-xml-items-" + version + ".csv"), StandardCharsets.US_ASCII));
            //reader = new BufferedReader(new FileReader("D:\\Users\\depryf\\dev\\projects_github\\naaccr-xml\\docs\\naaccr-xml-items-" + version + ".csv"));

            Map<String, AtomicInteger> counters = new HashMap<>();

            reader.readLine();
            String line = reader.readLine();
            while (line != null) {
                int idx3 = line.lastIndexOf(',');
                int idx2 = line.lastIndexOf('"');
                int idx1 = line.lastIndexOf('"', idx2 - 1);
                int idx0 = line.lastIndexOf(',', idx1 - 2);
                String recTypes = line.substring(idx1 + 1, idx2);
                String naaccrId = line.substring(idx2 + 2, idx3);
                Integer length = Integer.valueOf(line.substring(idx0 + 1, idx1 - 1));

                if (naaccrId.length() > 32) {
                    String prefix = naaccrId.substring(0, 30);
                    AtomicInteger counter = counters.get(prefix);
                    if (counter == null) {
                        counter = new AtomicInteger();
                        counters.put(prefix, counter);
                    }
                    naaccrId = prefix + "_" + counter.getAndIncrement();
                }

                if (recTypes.contains(recordType))
                    result.put(naaccrId, length);

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

    public static void main(String[] args) {
        SasUtils.getFields("180", "A");
    }
}
