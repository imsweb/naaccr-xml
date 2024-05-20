/*
 * Copyright (C) 2024 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class CreateNaaccr25Dictionary {

    public static void main(String[] args) throws IOException, CsvException {
        
        List<NaaccrDictionaryItem> itemsToAdd = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(TestingUtils.getWorkingDirectory() + "/docs/naaccr-25/New data item V25.csv"))) {
            String[] line = reader.readNext();
            while (line != null) {
                if ("Data Item Number".equals(line[0]) || line[0].isEmpty()) {
                    line = reader.readNext();
                    continue;
                }
                
                NaaccrDictionaryItem item = new NaaccrDictionaryItem();
                item.setNaaccrNum(Integer.valueOf(line[0]));
                item.setNaaccrName(line[1]);
                item.setLength(Integer.valueOf(line[2]));
                item.setRecordTypes(line[3]);
                if ("A,C,I,M".equals(item.getRecordTypes()))
                    item.setRecordTypes("A,M,C,I");
                item.setNaaccrId(line[4]);
                item.setParentXmlElement(line[5]);
                item.setDataType(line[6]);
                if (StringUtils.isBlank(item.getDataType()))
                    item.setDataType("text");
                itemsToAdd.add(item);
                
                line = reader.readNext();
            }
        }

        Map<String, NaaccrDictionaryItem> itemsToModified = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(TestingUtils.getWorkingDirectory() + "/docs/naaccr-25/Changed data items V25.csv"))) {
            String[] line = reader.readNext();
            while (line != null) {
                if ("Data Item Number".equals(line[0]) || line[0].isEmpty()) {
                    line = reader.readNext();
                    continue;
                }

                NaaccrDictionaryItem item = new NaaccrDictionaryItem();
                item.setNaaccrNum(Integer.valueOf(line[0]));
                item.setNaaccrName(line[1]);
                item.setLength(Integer.valueOf(line[2]));
                item.setRecordTypes(line[4]);
                if ("A,C,I,M".equals(item.getRecordTypes()))
                    item.setRecordTypes("A,M,C,I");
                item.setNaaccrId(line[6]);
                item.setParentXmlElement(line[7]);
                item.setDataType(line[23]);
                if (item.getNaaccrId().startsWith("pathDateSpecCollect"))
                    item.setDataType("dateTime");
                else if (item.getNaaccrId().equals("tumorRecordNumber") || item.getNaaccrId().equals("censusIndCode2010"))
                    item.setDataType("digits");
                else if (StringUtils.isBlank(item.getDataType()))
                    item.setDataType("text");
                itemsToModified.put(item.getNaaccrId(), item);

                line = reader.readNext();
            }
        }

        Set<Integer> itemsToRemove = new HashSet<>();
        try (CSVReader reader = new CSVReader(new FileReader(TestingUtils.getWorkingDirectory() + "/docs/naaccr-25/Retired Data Items in V25.csv"))) {
            String[] line = reader.readNext();
            while (line != null) {
                if ("Data Item Number".equals(line[0]) || line[0].isEmpty()) {
                    line = reader.readNext();
                    continue;
                }

                itemsToRemove.add(Integer.valueOf(line[0]));

                line = reader.readNext();
            }
        }

        List<NaaccrDictionaryItem> items = new ArrayList<>(NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("240").getItems());
        items.removeIf(i -> itemsToRemove.contains(i.getNaaccrNum()));
        
        items.addAll(itemsToAdd);
        
        for (NaaccrDictionaryItem item : items) {
            NaaccrDictionaryItem updatedItem = itemsToModified.get(item.getNaaccrId());
            if (updatedItem != null) {
                item.setNaaccrName(updatedItem.getNaaccrName());
                item.setLength(updatedItem.getLength());
                item.setRecordTypes(updatedItem.getRecordTypes());
                item.setParentXmlElement(updatedItem.getParentXmlElement());
                if (!StringUtils.isBlank(updatedItem.getDataType()))
                    item.setDataType(updatedItem.getDataType());
            }
        }

        items.sort(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId));
        
        NaaccrDictionary dictionary = new NaaccrDictionary();
        dictionary.setDictionaryUri("http://naaccr.org/naaccrxml/naaccr-dictionary-250.xml");
        dictionary.setNaaccrVersion("250");
        dictionary.setSpecificationVersion("1.8");
        Date lastModifiedValue = Date.from(LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0).atZone(ZoneId.systemDefault()).toInstant());
        dictionary.setDateLastModified(lastModifiedValue);
        dictionary.setDescription("NAACCR 24 base dictionary");
        dictionary.setItems(items);
        
        NaaccrXmlDictionaryUtils.validateBaseDictionary(dictionary);

        File targetFile = new File(TestingUtils.getWorkingDirectory() + "/src/main/resources/naaccr-dictionary-250.xml");
        NaaccrXmlDictionaryUtils.writeDictionary(dictionary, targetFile);
    }
    
}
