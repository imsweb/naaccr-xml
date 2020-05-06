/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.SortedSet;
import java.util.TreeSet;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class DictionaryToCsv {

    public static void main(String[] args) throws IOException {

        // re-create the csv standard dictionaries in the docs folder...
        for (String version : NaaccrFormat.getSupportedVersions())
            NaaccrXmlDictionaryUtils.writeDictionaryToCsv(NaaccrXmlDictionaryUtils.getMergedDictionaries(version), Paths.get("docs/naaccr-xml-items-" + version + ".csv").toFile());

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
