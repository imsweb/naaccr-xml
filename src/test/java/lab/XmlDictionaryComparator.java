/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package lab;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class XmlDictionaryComparator {

    public static void main(String[] args) throws IOException {
        compareDictionaryVersions("160", "180");
    }

    /**
     * helper method - gets all items and grouped items from the base and default user dictionary, based on the dictionary version
     * @param version dictionary version
     * @return List of all items
     */
    private static List<NaaccrDictionaryItem> getAllDictionaryItems(String version) {
        NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(version);

        List<NaaccrDictionaryItem> allItems = new ArrayList<>(baseDictionary.getItems());
        allItems.addAll(baseDictionary.getGroupedItems());
        allItems.addAll(NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(version).getItems());

        return allItems;
    }

    /**
     * Compares dictionary items between two versions - finds items added, items removed, items whose name changed, and items whose name AND Id changed
     * @param oldVersion older dictionary version
     * @param newVersion newer dictionary version
     * @throws IOException
     */
    private static void compareDictionaryVersions(String oldVersion, String newVersion) throws IOException {
        List<NaaccrDictionaryItem> removedItems = new ArrayList<>();    //List of items that have been removed
        List<NaaccrDictionaryItem> addedItems = new ArrayList<>();      //List of items that have been added
        Map<NaaccrDictionaryItem, NaaccrDictionaryItem> newNameNewId = new HashMap<>(); //Map of Old Item, New Item
        Map<NaaccrDictionaryItem, NaaccrDictionaryItem> newNameSameId = new HashMap<>(); //Map of Old Item, New Item

        NaaccrDictionary newDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(newVersion);
        NaaccrDictionary newUserDictionary = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(newVersion);

        List<NaaccrDictionaryItem> newDictionaryItems = getAllDictionaryItems(newVersion);
        List<NaaccrDictionaryItem> oldDictionaryItems = getAllDictionaryItems(oldVersion);

        for (NaaccrDictionaryItem oldItem : oldDictionaryItems) {
            //Check if the new dictionary (base or default user) still contains the old dictionary item using NAACCR Number (Number will never change between versions)
            int naaccrNum = oldItem.getNaaccrNum();
            NaaccrDictionaryItem newItem = newDictionary.getItemByNaaccrNum(naaccrNum);
            if (newItem == null)
                newItem = newDictionary.getGroupedItemByNaaccrNum(naaccrNum);
            if (newItem == null)
                newItem = newUserDictionary.getItemByNaaccrNum(naaccrNum);

            if (newItem == null)
                removedItems.add(oldItem);
            else {
                if (!newItem.getNaaccrName().equals(
                        oldItem.getNaaccrName()))   //If new dictionary still contains the item, check if the name changed - if not, assume no other changes occurred
                    if (newItem.getNaaccrId().equals(oldItem.getNaaccrId()))    //If the names don't match, check to see if the ID changed as well (would happen to reflect name change)
                        newNameSameId.put(oldItem, newItem);
                    else
                        newNameNewId.put(oldItem, newItem);

                newDictionaryItems.remove(newItem);
            }
        }
        //Any items left in newDictionaryItems weren't found in the old dictionary - they are newly added items
        addedItems.addAll(newDictionaryItems);

        //Write differences to a file in build/test-tmp
        File outputFile = TestingUtils.createFile("dictionary-differences-v" + oldVersion + "-and-v" + newVersion, false);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write("New Items ADDED in version " + newVersion + "\n");
            writer.write("Item information is displayed as: NAAACCR Number,NAACCR ID,NAACCR Name\n");
            for (NaaccrDictionaryItem item : addedItems)
                writer.write(item.getNaaccrNum() + "," + item.getNaaccrId() + "," + item.getNaaccrName() + "\n");

            writer.write("\nItems in version " + oldVersion + " that have been REMOVED in version " + newVersion + "\n");
            writer.write("Item information is displayed as: NAAACCR Number,NAACCR ID,NAACCR Name\n");
            for (NaaccrDictionaryItem item : removedItems)
                writer.write(item.getNaaccrNum() + "," + item.getNaaccrId() + "," + item.getNaaccrName() + "\n");

            writer.write("\nItems that appear in both versions (using the same NAACCR Number AND the same NAACCR ID), but the NAACCR Name has changed" + "\n");
            writer.write("Item information is displayed as: NAAACCR Number,NAACCR ID,OLD NAACCR Name,NEW NAACCR Name\n");
            //Key = old Value = new
            for (Entry<NaaccrDictionaryItem, NaaccrDictionaryItem> entry : newNameSameId.entrySet()) {
                NaaccrDictionaryItem oldItem = entry.getKey();
                NaaccrDictionaryItem newItem = entry.getValue();
                writer.write(newItem.getNaaccrNum() + "," + newItem.getNaaccrId() + "," + oldItem.getNaaccrName() + "," + newItem.getNaaccrName() + "\n");
            }

            writer.write("\nItems that appear in both versions (using the same NAACCR Number), but the NAACCR Name AND the NAACCR ID have changed" + "\n");
            writer.write("Item information is displayed as: NAAACCR Number,OLD NAACCR ID,OLD NAACCR Name,NEW NAACCR ID,NEW NAACCR Name\n");
            //Key = old Value = new
            for (Entry<NaaccrDictionaryItem, NaaccrDictionaryItem> entry : newNameNewId.entrySet()) {
                NaaccrDictionaryItem oldItem = entry.getKey();
                NaaccrDictionaryItem newItem = entry.getValue();
                writer.write(newItem.getNaaccrNum() + "," + oldItem.getNaaccrId() + "," + oldItem.getNaaccrName() + "," + newItem.getNaaccrId() + "," + newItem.getNaaccrName() + "\n");
            }
        }
    }
}
