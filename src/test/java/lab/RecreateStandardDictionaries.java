/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public class RecreateStandardDictionaries {

    public static void main(String[] args) throws IOException {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            Path path = Paths.get("src/main/resources/naaccr-dictionary-" + version + ".xml");
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.readDictionary(path.toFile());
            applyFix(dictionary, true);
            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());

            path = Paths.get("src/main/resources/user-defined-naaccr-dictionary-" + version + ".xml");
            dictionary = NaaccrXmlDictionaryUtils.readDictionary(path.toFile());
            applyFix(dictionary, false);
            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());
        }
    }

    private static void applyFix(NaaccrDictionary dictionary, boolean isBase) {
        // default is to do nothing
    }
}
