/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class DictionaryToCsv {

    public static void main(String[] args) throws IOException {
        fullAbstract();
        //incidenceOnly();
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

    private static void fullAbstract() throws IOException {
        AtomicInteger max = new AtomicInteger();
        for (String version : NaaccrFormat.getSupportedVersions()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get("docs/naaccr-xml-items-" + version + ".csv").toFile()))) {
                writer.write("Item Number,Item Name,Item Start column,Item Length,Record Types,NAACCR XML ID,NAACCR XML Parent Element");
                writer.newLine();
                NaaccrXmlDictionaryUtils.getMergedDictionaries(version).getItems().stream()
                        .sorted((o1, o2) -> {
                            if (o1.getStartColumn() == null && o2.getStartColumn() == null)
                                return o1.getNaaccrId().compareTo(o2.getNaaccrId());
                            if (o1.getStartColumn() == null)
                                return 1;
                            if (o2.getStartColumn() == null)
                                return -1;
                            return o1.getStartColumn().compareTo(o2.getStartColumn());
                        })
                        .forEach(item -> {
                            max.set(Math.max(max.get(), item.getNaaccrId().length()));
                            try {
                                writer.write(
                                        item.getNaaccrNum() + ",\"" + item.getNaaccrName() + "\"," + item.getStartColumn() + "," + item.getLength() + ",\"" + item.getRecordTypes() + "\"," + item
                                                .getNaaccrId() + "," + item.getParentXmlElement());
                                writer.newLine();
                            }
                            catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
        System.out.println(max.get());
    }

    private static void incidenceOnly() throws IOException {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get("docs/naaccr-xml-incidence-" + version + ".csv").toFile()))) {
                writer.newLine();
                NaaccrXmlDictionaryUtils.getMergedDictionaries(version).getItems().stream()
                        .sorted((o1, o2) -> {
                            if (o1.getStartColumn() == null && o2.getStartColumn() == null)
                                return o1.getNaaccrId().compareTo(o2.getNaaccrId());
                            if (o1.getStartColumn() == null)
                                return 1;
                            if (o2.getStartColumn() == null)
                                return -1;
                            return o1.getStartColumn().compareTo(o2.getStartColumn());
                        })
                        .forEach(item -> {
                            if (item.getRecordTypes().contains("I")) {
                                try {
                                    writer.write(item.getNaaccrId() + ",\"" + item.getNaaccrNum());
                                    writer.newLine();
                                }
                                catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
            }
        }
    }

}
