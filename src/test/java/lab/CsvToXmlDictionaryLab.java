/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class CsvToXmlDictionaryLab {

    public static void main(String[] args) throws IOException {
        handleVersion("140", "14");
        handleVersion("150", "15");
        handleVersion("160", "16");
    }

    private static void handleVersion(String version, String formattedVersion) throws IOException {

        // first read the data types
        Map<String, String> dataTypes = new HashMap<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(new File(System.getProperty("user.dir") + "/docs/fabian/data-types-" + version + ".csv")), ',', '"', '\\', 1, false)) {
            for (String[] line : csvReader.readAll())
                if (!line[4].trim().isEmpty())
                    dataTypes.put(line[0].trim(), line[4].trim());
        }

        // create base XML dictionary from CSV...
        NaaccrDictionary dictionary = readDictionaryFromCsv(new FileReader(new File(System.getProperty("user.dir") + "/docs/fabian/naaccr-dictionary-" + version + ".csv")), dataTypes);
        System.out.println("Read " + dictionary.getItems().size() + " items from base CSV dictionary for " + version);
        dictionary.setDictionaryUri("http://naaccr.org/naaccrxml/naaccr-dictionary-" + version + ".xml");
        dictionary.setNaaccrVersion(version);
        dictionary.setDescription("NAACCR " + formattedVersion + " base dictionary");
        File outputFile = new File(System.getProperty("user.dir") + "/src/main/resources/naaccr-dictionary-" + version + ".xml");
        FileWriter writer = new FileWriter(outputFile);
        NaaccrXmlDictionaryUtils.writeDictionary(dictionary, writer);
        writer.close();
        System.out.println("Wrote " + outputFile.getPath());

        // create default user XML dictionary from CSV...
        dictionary = readDictionaryFromCsv(new FileReader(new File(System.getProperty("user.dir") + "/docs/fabian/user-defined-naaccr-dictionary-" + version + ".csv")), dataTypes);
        System.out.println("Read " + dictionary.getItems().size() + " items from default user CSV dictionary for " + version);
        dictionary.setDictionaryUri("http://naaccr.org/naaccrxml/user-defined-naaccr-dictionary-" + version + ".xml");
        dictionary.setNaaccrVersion(version);
        dictionary.setDescription("NAACCR " + formattedVersion + " default user dictionary");
        outputFile = new File(System.getProperty("user.dir") + "/src/main/resources/user-defined-naaccr-dictionary-" + version + ".xml");
        writer = new FileWriter(outputFile);
        NaaccrXmlDictionaryUtils.writeDictionary(dictionary, writer);
        writer.close();
        System.out.println("Wrote " + outputFile.getPath());
    }

    public static NaaccrDictionary readDictionaryFromCsv(Reader reader, Map<String, String> dataTypes) throws IOException {
        NaaccrDictionary dictionary = new NaaccrDictionary();

        // always skip first line
        try (CSVReader csvReader = new CSVReader(reader, ',', '"', '\\', 1, false)) {
            for (String[] line : csvReader.readAll()) {
                if (line.length < 3 || line.length > 17)
                    throw new IOException("Wrong number of fields, expected between 3 and 15, got " + line.length + " (Item ID " + line[0] + ")");

                NaaccrDictionaryItem item = new NaaccrDictionaryItem();
                item.setNaaccrId(NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(line[3])); // not using the CSV version anymore...
                if (line[1] != null && !line[1].isEmpty())
                    item.setNaaccrNum(Integer.valueOf(line[1]));
                item.setLength(Integer.valueOf(line[2]));
                if (line.length > 3 && line[3] != null && !line[3].isEmpty())
                    item.setNaaccrName(line[3]);
                if (line.length > 6 && line[6] != null && !line[6].isEmpty())
                    item.setRecordTypes(line[6]);
                if (line.length > 7 && line[7] != null && !line[7].isEmpty())
                    item.setStartColumn(Integer.valueOf(line[7]));
                if (line.length > 8 && line[8] != null && !line[8].isEmpty())
                    item.setParentXmlElement(line[8]);
                if (line.length > 9 && line[9] != null && !line[9].isEmpty())
                    item.setRegexValidation(line[9]);
                if (line.length > 10 && line[10] != null && !line[10].isEmpty())
                    item.setDataType(line[10]);
                if (dataTypes.containsKey(item.getNaaccrId())) {
                    item.setDataType(dataTypes.get(item.getNaaccrId()));
                    if (item.getDataType() != null && NaaccrXmlDictionaryUtils.getDataTypePattern(item.getDataType()) == null)
                        throw new RuntimeException("Unsupported data type: " + item.getDataType() + " for " + item.getNaaccrId());
                }
                else
                    item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
                // section is not used anymore
                if (line.length > 12 && line[12] != null && !line[12].isEmpty())
                    item.setSourceOfStandard(line[12]);
                // retired is not used anymore
                // implementation is not used anymore
                if (line.length > 15 && line[15] != null && !line[15].isEmpty())
                    item.setTrim(line[15]);
                if (line.length > 16 && line[16] != null && !line[16].isEmpty())
                    item.setPadding(line[16]);

                if (item.getRecordTypes() == null)
                    item.setRecordTypes("A,M,C,I");

                // this is temporary until the data types are looked at and fixed...
                //item.setRegexValidation(null);

                dictionary.addItem(item);
            }
        }

        return dictionary;
    }

}
