/*
 * Copyright (C) 2023 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class DictionaryFromCsv {

    public static void main(String[] args) throws IOException, CsvException {

        // Date Item Introduced (MM/DD/YYYY),Date Item Last Updated (MM/DD/YYYY),Retired Date (MM/DD/YYYY),NAACCR ID (max. 32 characters),Item Name,"Data Item Number",Length,"Parent XML Element","Record Type",Data Type,Description,Codes,Required Status from Reporting Facilities,,,

        try (CSVReader reader = new CSVReader(new FileReader("path-to-csv-file"))) {

            NaaccrDictionary dictionary = new NaaccrDictionary();
            dictionary.setDictionaryUri("<URI>");
            dictionary.setDescription("<DESCRIPTION>");

            for (String[] row : reader.readAll()) {
                String id = row[3];
                String name = row[4];
                String num = row[5];
                String length = row[6];
                String parent = row[7];
                String recTypes = row[8];
                if ("A, C, I, M".equals(recTypes))
                    recTypes = "A,M,C,I";
                else
                    throw new IllegalStateException("Unexpected record types: " + recTypes);
                String type = row[9];

                NaaccrDictionaryItem item = new NaaccrDictionaryItem();
                item.setNaaccrId(id.trim());
                item.setNaaccrName(name.trim());
                item.setNaaccrNum(Integer.valueOf(num));
                item.setLength(Integer.valueOf(length));
                item.setParentXmlElement(parent);
                item.setRecordTypes(recTypes);
                item.setDataType(type);

                dictionary.addItem(item);
            }

            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, new File("<path-to-xml-file>"));
        }
    }
}
