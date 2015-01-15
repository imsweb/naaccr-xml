/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

import au.com.bytecode.opencsv.CSVReader;

public class DictionaryUtils {
    
    public static final String NAACCR_DICTIONARY_FORMAT_CSV = "csv";

    public static NaaccrDictionary readDictionary(File file) throws IOException {
        if (file == null)
            throw new IOException("File is required");
        if (!file.exists())
            throw new IOException("File does not exist");

        NaaccrDictionary dictionary;

        if (file.getName().endsWith(".csv"))
            dictionary = readDictionaryFromCsv(new FileReader(file));
        else
            throw new IOException("Unsupported format");
        
        return dictionary;
    }
    
    public static NaaccrDictionary readDictionary(URL url, String format) throws IOException{
        if (url == null)
            throw new IOException("URL is required");

        NaaccrDictionary dictionary;
        
        switch (format) {
            case NAACCR_DICTIONARY_FORMAT_CSV:
                dictionary = readDictionaryFromCsv(new InputStreamReader(url.openStream()));
                break;
            default:
                throw new IOException("Unsupported format: " + format);
                
        }
        
        return dictionary;
    }
    
    private static NaaccrDictionary readDictionaryFromCsv(Reader reader) throws IOException {
        NaaccrDictionary dictionary = new NaaccrDictionary();

        try (CSVReader csvReader = new CSVReader(reader)) {
            for (String[] line : csvReader.readAll()) {
                if (line.length != 11)
                    throw new IOException("Wrong number of fields, expected 11, got " + line.length + " (Item Number #" + line[0] + ")");
                
                if (!line[0].matches("\\d+"))
                    continue;

                // TODO add validation, trim values, be safer since it could be a user-defined dictionary...
                NaaccrDictionaryItem item = new NaaccrDictionaryItem();
                item.setNumber(Integer.valueOf(line[0]));
                item.setName(line[1]);
                item.setStartColumn(Integer.valueOf(line[2]));
                item.setLength(Integer.valueOf(line[3]));
                item.setSection(line[4]);
                item.setRecordTypes(new HashSet<>(Arrays.asList(line[5].split(","))));
                item.setSourceOfStandard(line[6]);
                item.setElementName(line[7]);
                item.setParentElement(line[8]);
                item.setRegexValidation(line[9]);
                item.setDataType(line[10]);
                
                dictionary.getItems().add(item);
            }
        }
        
        return dictionary;
    }
    
    public static void main(String[] args) throws IOException {
        NaaccrDictionary dictionary = readDictionary(Thread.currentThread().getContextClassLoader().getResource("fabian/naaccr-dictionary-v14.csv"), NAACCR_DICTIONARY_FORMAT_CSV);
        System.out.println("Read " + dictionary.getItems().size() + " items...");
    }
}
