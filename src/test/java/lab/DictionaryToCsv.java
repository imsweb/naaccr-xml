/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class DictionaryToCsv {

    public static void main(String[] args) throws IOException {

        // re-create the csv standard dictionaries in the docs folder...
        for (String version : NaaccrFormat.getSupportedVersions()) {
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.getMergedDictionaries(version);
            NaaccrXmlDictionaryUtils.writeDictionaryToCsv(dictionary, Paths.get("docs/naaccr-xml-items-" + version + ".csv").toFile());

            File file = new File("docs/grouped-items/naaccr-xml-grouped-items-" + version + ".csv");
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.US_ASCII))) {
                writer.write("NAACCR XML ID,NAACCR Number,Name,Length,Record Types,Parent XML Element,Contained Items");
                writer.write(System.lineSeparator());
                dictionary.getGroupedItems().stream()
                        .sorted(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId))
                        .forEach(item -> {
                            try {
                                writer.write(item.getNaaccrId());
                                writer.write(",");
                                writer.write(item.getNaaccrNum() == null ? "" : item.getNaaccrNum().toString());
                                writer.write(",\"");
                                writer.write(item.getNaaccrName() == null ? "" : item.getNaaccrName());
                                writer.write("\",");
                                writer.write(item.getLength().toString());
                                writer.write(",\"");
                                writer.write(item.getRecordTypes() == null ? "" : item.getRecordTypes());
                                writer.write("\",");
                                writer.write(item.getParentXmlElement());
                                writer.write(",\"");
                                writer.write(item.getContains());
                                writer.write("\"");
                                writer.write(System.lineSeparator());
                            }
                            catch (IOException | RuntimeException ex1) {
                                throw new RuntimeException(ex1); // doing that to make sure the loop is broken...
                            }
                        });
            }
        }

        //csv16to18diff();
    }

    private static void csv16to18diff() {
        System.out.println("NAACCR ID,NAACCR Num, N16 Start, N16 Length,N18 Start,N18 Length");

        NaaccrDictionary dict16 = NaaccrXmlDictionaryUtils.getMergedDictionaries("160");
        NaaccrDictionary dict18 = NaaccrXmlDictionaryUtils.getMergedDictionaries("180");

        SortedSet<String> allIds = new TreeSet<>();
        dict16.getItems().forEach(i -> allIds.add(i.getNaaccrId()));
        dict18.getItems().forEach(i -> allIds.add(i.getNaaccrId()));

        for (String id : allIds) {
            NaaccrDictionaryItem item16 = dict16.getItemByNaaccrId(id);
            NaaccrDictionaryItem item18 = dict18.getItemByNaaccrId(id);
            String num = item18 != null ? item18.getNaaccrNum().toString() : item16.getNaaccrNum().toString();
            String n16start = item16 != null ? item16.getStartColumn().toString() : "";
            String n16Length = item16 != null ? item16.getLength().toString() : "";
            String n18start = item18 != null ? item18.getStartColumn().toString() : "";
            String n18Length = item18 != null ? item18.getLength().toString() : "";
            System.out.println(id + "," + num + "," + n16start + "," + n16Length + "," + n18start + "," + n18Length);
        }
    }
}
