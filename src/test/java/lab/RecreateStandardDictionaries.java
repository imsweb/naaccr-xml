/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Collectors;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class RecreateStandardDictionaries {

    @SuppressWarnings("ConstantValue")
    public static void main(String[] args) throws IOException {

        // I think this will need to change to a list of included versions (or excluded ones if it's easier); at the end,
        // the date should be updated only if there were actual changes in the dictionary...
        boolean updateLastModified = false;

        // I don't want random-looking timestamps, and so I am setting the time portion to noon;
        // the only reason to ever change that is the dictionary to be changed twice in the same day...
        Date lastModifiedValue = Date.from(LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0).atZone(ZoneId.systemDefault()).toInstant());

        for (String version : Collections.singletonList("260")) { // NaaccrFormat.getSupportedVersions()) {
            Path path = Paths.get("src/main/resources/naaccr-dictionary-" + version + ".xml");
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.readDictionary(path.toFile());
            if (updateLastModified)
                dictionary.setDateLastModified(lastModifiedValue);
            dictionary.setItems(dictionary.getItems().stream().sorted(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId)).collect(Collectors.toList()));
            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());

            if (version.compareTo(NaaccrFormat.NAACCR_VERSION_210) <= 0) {
                path = Paths.get("src/main/resources/user-defined-naaccr-dictionary-" + version + ".xml");
                dictionary = NaaccrXmlDictionaryUtils.readDictionary(path.toFile());
                if (updateLastModified)
                    dictionary.setDateLastModified(lastModifiedValue);
                NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());
            }
        }
    }
}
