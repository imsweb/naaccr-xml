/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.lab;

import java.io.File;
import java.io.FileReader;

import org.naaccr.xml.NaaccrXmlDictionaryUtils;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

public class NaaccrIdGenerator {

    public static void main(String[] args) throws Exception {

        // read the CSV file
        String version = "150";
        NaaccrDictionary dictionary = CsvToXmlDictionaryLab.readDictionaryFromCsv(new FileReader(new File(System.getProperty("user.dir") + "/docs/fabian/naaccr-dictionary-" + version + ".csv")));
        System.out.println("Read " + dictionary.getItems().size() + " items from base CSV dictionary for " + version);

        // iterate over the items and display their old name/new name
        for (NaaccrDictionaryItem item : dictionary.getItems())
            System.out.println(item.getNaaccrName() + " (#" + item.getNaaccrNum() + ")\n    " + item.getNaaccrId() + " -> " + NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(item.getNaaccrName()));
    }
}
