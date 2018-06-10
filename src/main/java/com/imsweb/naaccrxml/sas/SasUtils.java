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
     * @return fields information
     */
    public static List<SasFieldInfo> getFields(String version, String recordType) {
        return getFields(recordType, Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-xml-items-" + version + ".csv"));
    }

    /**
     * Returns the fields information for the given parameters.
     * <br/><br/>
     * Note that some NAACCR ID are too long for SAS; this method cuts-off any ID that is longer than 32 characters and
     * add a numeric suffix to ensure unicity.
     * @param recordType record type
     * @param is input stream to the information file
     * @return fields information
     */
    public static List<SasFieldInfo> getFields(String recordType, InputStream is) {
        List<SasFieldInfo> result = new ArrayList<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII));

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

                if (recTypes.contains(recordType))
                    result.add(new SasFieldInfo(naaccrId, parentTag));

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
}
