/*
 * Copyright (C) 2022 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.imsweb.naaccrxml.NaaccrOptions;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class RemoveUserDictionaryFromDataFileLab {

    public static void main(String[] args) throws IOException {
        File oldDir = new File("<replaced-me>");
        File newDir = new File("<replaced-me>");

        NaaccrOptions options = new NaaccrOptions();
        options.setAllowMissingDictionary(true);

        for (File file : Objects.requireNonNull(oldDir.listFiles())) {
            System.out.println("Processing " + file.getPath());

            NaaccrData data = NaaccrXmlUtils.readXmlFile(file, options, null, null);

            data.setUserDictionaryUri(null);

            Set<String> baseItems = NaaccrXmlDictionaryUtils.getBaseDictionaryByUri(data.getBaseDictionaryUri()).getItems().stream().map(NaaccrDictionaryItem::getNaaccrId).collect(Collectors.toSet());

            Set<String> itemsToRemove = new HashSet<>();
            for (Patient patient : data.getPatients()) {
                for (Item item : patient.getItems())
                    if (!baseItems.contains(item.getNaaccrId()))
                        itemsToRemove.add(item.getNaaccrId());
                itemsToRemove.forEach(patient::removeItem);

                for (Tumor tumor : patient.getTumors()) {
                    for (Item item : tumor.getItems())
                        if (!baseItems.contains(item.getNaaccrId()))
                            itemsToRemove.add(item.getNaaccrId());
                    itemsToRemove.forEach(tumor::removeItem);
                }
            }

            if (!itemsToRemove.isEmpty())
                System.out.println("  > removed " + itemsToRemove);

            NaaccrXmlUtils.writeXmlFile(data, new File(newDir, file.getName()), options, null, null);
        }
    }
}
