/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryGroupedItem;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class AddNaaccr21Items {

    public static void main(String[] args) throws IOException {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            if (!"210".equals(version))
                continue;

            Path path = Paths.get("src/main/resources/naaccr-dictionary-" + version + ".xml");
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.readDictionary(path.toFile());
            dictionary.getItemByNaaccrId("force-caching");

            try (CSVReader reader = new CSVReader(new FileReader(new File("C:\\dev\\Vol2 v21 New and Revised select fields DRAFT.csv")))) {
                String[] line = reader.readNext();

                line = reader.readNext();
                while (line != null) {

                    Integer num = Integer.parseInt(line[0]);
                    Integer length = Integer.parseInt(line[1]);
                    String name = line[2];
                    String id = line[3];
                    String level = line[4];
                    boolean isNew = "New".equals(line[6]);

                    if (isNew) {
                        NaaccrDictionaryItem item = new NaaccrDictionaryItem();
                        item.setNaaccrId(id);
                        item.setNaaccrName(name);
                        item.setParentXmlElement(level);
                        item.setNaaccrNum(num);
                        item.setRecordTypes("nameBirthSurname".equals(id) ? "A,M,C" : "A,M,C,I");
                        item.setLength(length);
                        dictionary.addItem(item);
                    }
                    else {
                        NaaccrDictionaryItem item = dictionary.getItemByNaaccrId(id);
                        if (item == null)
                            System.out.println("!!! Unable to find " + id);
                        else
                            System.out.println("Need to review " + id);
                    }

                    line = reader.readNext();
                }
            }

            dictionary.getItems().forEach(i -> i.setStartColumn(null));
            dictionary.getGroupedItems().forEach(i -> i.setStartColumn(null));

            dictionary.setItems(dictionary.getItems().stream().sorted(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId)).collect(Collectors.toList()));
            dictionary.setGroupedItems(dictionary.getGroupedItems().stream().sorted(Comparator.comparing(NaaccrDictionaryGroupedItem::getNaaccrId)).collect(Collectors.toList()));

            //NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());
        }
    }
}
