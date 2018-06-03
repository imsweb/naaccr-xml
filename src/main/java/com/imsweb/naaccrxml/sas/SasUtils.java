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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public static List<String> getFields(String version, String recordType) {
        List<String> headers = new ArrayList<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-dictionary-"+version+".xml"), StandardCharsets.US_ASCII));

            Pattern pattern = Pattern.compile("<ItemDef naaccrId=\"(.+?)\"");
            Map<String, AtomicInteger> counters = new HashMap<>();

            String line = reader.readLine();
            while (line != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String naaccrId = matcher.group(1);

                    if (naaccrId.length() > 32) {
                        String prefix = naaccrId.substring(0, 30);
                        int counter = counters.computeIfAbsent(prefix, k -> new AtomicInteger()).getAndIncrement();
                        naaccrId = prefix + "_" + counter;
                    }

                    if ("nameLast".equals(naaccrId) && !"A".equals(recordType) && !"M".equals(recordType) && !"C".equals(recordType))
                        break;
                    if ("textDxProcPe".equals(naaccrId) && !"A".equals(recordType) && !"M".equals(recordType))
                        break;
                    if (!naaccrId.startsWith("reserved"))
                        headers.add(naaccrId);
                }
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

        return headers;
    }
}
