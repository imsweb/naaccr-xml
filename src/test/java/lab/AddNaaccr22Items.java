/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class AddNaaccr22Items {

    public static void main(String[] args) throws Exception {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            if (!"220".equals(version))
                continue;

            Path path = Paths.get("src/main/resources/naaccr-dictionary-" + version + ".xml");
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.readDictionary(path.toFile());
            dictionary.getItemByNaaccrId("force-caching");

            try (CSVReader reader = new CSVReader(new FileReader(TestingUtils.getWorkingDirectory() + "/docs/naaccr-22/Vol II v22 Layout for N22 Base dictionary.csv"))) {
                String[] line = reader.readNext(); // ignore headers

                line = reader.readNext();
                while (line != null) {

                    Integer length = Integer.parseInt(line[0]);
                    Integer num = Integer.parseInt(line[1]);
                    String name = line[2];
                    String id = line[3];
                    String level = line[6];
                    String type = line[7];
                    boolean isNew = "New".equals(line[8]);

                    if (isNew) {
                        NaaccrDictionaryItem item = new NaaccrDictionaryItem();
                        item.setNaaccrId(id);
                        item.setNaaccrName(name);
                        item.setParentXmlElement(level);
                        item.setNaaccrNum(num);
                        item.setRecordTypes("A,M,C,I");
                        item.setLength(length);
                        if ("Digit".equalsIgnoreCase(type))
                            item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
                        dictionary.addItem(item);
                    }
                    else {
                        NaaccrDictionaryItem item = dictionary.getItemByNaaccrId(id);
                        if (item == null)
                            System.out.println("!!! Unable to find " + id);
                    }

                    line = reader.readNext();
                }
            }

            dictionary.setItems(dictionary.getItems().stream().sorted(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId)).collect(Collectors.toList()));

            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());
        }
    }
}
