/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.lab;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class FixEntityLevelsLab {

    public static void main(String[] args) throws IOException {
        // first read the new levels in the spreadsheet
        Map<String, String> newLevels = new HashMap<>();
        File file = new File(System.getProperty("user.dir") + "/docs/rich/naaccr-dictionary-140_XMLNesting-reivewed-by-rich-20151312.csv");
        for (String[] row : new CSVReader(new FileReader(file)).readAll()) {
            if (row[0].equals("Item ID"))
                continue;
            String level;
            if ("p".equals(row[6]))
                level = "Patient";
            else if ("t".equals(row[6]))
                level = "Tumor";
            else if ("n".equals(row[6]))
                level = "NaaccrData";
            else
                throw new RuntimeException("bad level: " + row[6]);
            newLevels.put(row[0], level);
        }

        // then fix the csv version of the dictionary
        file = new File(System.getProperty("user.dir") + "/src/main/resources/naaccr-dictionary-140.csv");
        FileReader reader = new FileReader(file);
        List<String[]> rows = new CSVReader(new FileReader(file)).readAll();
        reader.close();
        try (FileWriter writer = new FileWriter(file)) {
            CSVWriter csvWriter = new CSVWriter(writer);
            for (String[] row : rows) {
                if (newLevels.containsKey(row[0]))
                    row[8] = newLevels.get(row[0]);
                csvWriter.writeNext(row);
            }
        }

        // then re-create the XML from the CSV, but that's in another lab class...
    }

}
