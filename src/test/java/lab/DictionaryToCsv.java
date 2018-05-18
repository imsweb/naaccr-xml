/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;

public class DictionaryToCsv {

    public static void main(String[] args) throws IOException {
        //fullAbstract();
        incidenceOnly();
    }

    private static void fullAbstract() throws IOException {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get("docs/naaccr-xml-items-" + version + ".csv").toFile()))) {
                writer.write("Item Number,Item Name,Item Start column,NAACCR XML ID,NAACCR XML Parent Element");
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
                            try {
                                writer.write(item.getNaaccrNum() + ",\"" + item.getNaaccrName() + "\"," + item.getStartColumn() + "," + item.getNaaccrId() + "," + item.getParentXmlElement());
                                writer.newLine();
                            }
                            catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
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
