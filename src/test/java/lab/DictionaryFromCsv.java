/*
 * Copyright (C) 2023 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.IOException;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class DictionaryFromCsv {

    public static void main(String[] args) throws IOException {

        // Date Item Introduced (MM/DD/YYYY),Date Item Last Updated (MM/DD/YYYY),Retired Date (MM/DD/YYYY),NAACCR ID (max. 32 characters),Item Name,"Data Item Number",Length,"Parent XML Element","Record Type",Data Type,Description,Codes,Required Status from Reporting Facilities,,,

        NaaccrDictionary dictionary = new NaaccrDictionary();
        dictionary.setDictionaryUri("<URI>");
        dictionary.setDescription("<DESCRIPTION>");
        
        File file = new File("change-me");
        try (CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(file.toPath())) {
            reader.stream().forEach(line -> {
                String id = line.getField(3);
                String name = line.getField(4);
                String num = line.getField(5);
                String length = line.getField(6);
                String parent = line.getField(7);
                String recTypes = line.getField(8);
                if ("A, C, I, M".equals(recTypes))
                    recTypes = "A,M,C,I";
                else
                    throw new IllegalStateException("Unexpected record types: " + recTypes);
                String type = line.getField(9);

                NaaccrDictionaryItem item = new NaaccrDictionaryItem();
                item.setNaaccrId(id.trim());
                item.setNaaccrName(name.trim());
                item.setNaaccrNum(Integer.valueOf(num));
                item.setLength(Integer.valueOf(length));
                item.setParentXmlElement(parent);
                item.setRecordTypes(recTypes);
                item.setDataType(type);

                dictionary.addItem(item);
            });

            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, new File("<path-to-xml-file>"));
        }
    }
}
