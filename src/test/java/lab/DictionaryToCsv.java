/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public class DictionaryToCsv {

    public static void main(String[] args) throws IOException {

        // re-create the csv standard dictionaries in the docs folder...
        for (String version : Collections.singletonList("260")) { //NaaccrFormat.getSupportedVersions()) {
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.getMergedDictionaries(version);
            NaaccrXmlDictionaryUtils.writeDictionaryToCsv(dictionary, Paths.get("docs/naaccr-xml-items-" + version + ".csv").toFile());
        }
    }
}
