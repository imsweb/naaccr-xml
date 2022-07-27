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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class XmlDictionaryComparator {

    public static void main(String[] args) throws IOException {
        compareDictionaryVersions("220", "230");
    }

    /**
     * Compares dictionary items between two versions - finds items added, items removed, items whose name changed, and items whose name AND Id changed
     * @param oldVersion older dictionary version
     * @param newVersion newer dictionary version
     * @throws IOException if error loading dictionaries
     */
    private static void compareDictionaryVersions(String oldVersion, String newVersion) throws IOException {
        List<NaaccrDictionaryItem> removedItems = new ArrayList<>();
        List<NaaccrDictionaryItem> addedItems = new ArrayList<>();
        List<Pair<NaaccrDictionaryItem, NaaccrDictionaryItem>> modifiedItemsId = new ArrayList<>();
        List<Pair<NaaccrDictionaryItem, NaaccrDictionaryItem>> modifiedItemsName = new ArrayList<>();
        List<Pair<NaaccrDictionaryItem, NaaccrDictionaryItem>> modifiedItemsLength = new ArrayList<>();
        List<Pair<NaaccrDictionaryItem, NaaccrDictionaryItem>> modifiedItemsLevel = new ArrayList<>();
        List<Pair<NaaccrDictionaryItem, NaaccrDictionaryItem>> modifiedItemsType = new ArrayList<>();
        List<Pair<NaaccrDictionaryItem, NaaccrDictionaryItem>> modifiedItemsPadding = new ArrayList<>();
        List<Pair<NaaccrDictionaryItem, NaaccrDictionaryItem>> modifiedItemsTrimming = new ArrayList<>();
        List<Pair<NaaccrDictionaryItem, NaaccrDictionaryItem>> modifiedItemsUnlimitedText = new ArrayList<>();

        NaaccrDictionary oldDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(oldVersion);
        NaaccrDictionary newDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(newVersion);

        for (NaaccrDictionaryItem newItem : newDictionary.getItems()) {
            NaaccrDictionaryItem oldItem = oldDictionary.getItemByNaaccrNum(newItem.getNaaccrNum());
            if (oldItem == null)
                addedItems.add(newItem);
            else {
                if (!Objects.equals(oldItem.getNaaccrId(), newItem.getNaaccrId()))
                    modifiedItemsId.add(Pair.of(oldItem, newItem));
                if (!Objects.equals(oldItem.getNaaccrName(), newItem.getNaaccrName()))
                    modifiedItemsName.add(Pair.of(oldItem, newItem));
                if (!Objects.equals(oldItem.getLength(), newItem.getLength()))
                    modifiedItemsLength.add(Pair.of(oldItem, newItem));
                if (!Objects.equals(oldItem.getParentXmlElement(), newItem.getParentXmlElement()))
                    modifiedItemsLevel.add(Pair.of(oldItem, newItem));
                if (!Objects.equals(oldItem.getDataType(), newItem.getDataType()))
                    modifiedItemsType.add(Pair.of(oldItem, newItem));
                if (!Objects.equals(oldItem.getPadding(), newItem.getPadding()))
                    modifiedItemsPadding.add(Pair.of(oldItem, newItem));
                if (!Objects.equals(oldItem.getTrim(), newItem.getTrim()))
                    modifiedItemsTrimming.add(Pair.of(oldItem, newItem));
                if (!Objects.equals(oldItem.getAllowUnlimitedText(), newItem.getAllowUnlimitedText()))
                    modifiedItemsUnlimitedText.add(Pair.of(oldItem, newItem));
            }
        }
        for (NaaccrDictionaryItem oldItem : oldDictionary.getItems())
            if (newDictionary.getItemByNaaccrNum(oldItem.getNaaccrNum()) == null)
                removedItems.add(oldItem);

        //Write differences to a file in build/test-tmp
        File outputFile = TestingUtils.createFile("dictionary-differences-v" + oldVersion + "-and-v" + newVersion + ".txt", false);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write("Comparison summary of base " + oldVersion + " and " + newVersion + " dictionaries.\n");

            // added
            if (!addedItems.isEmpty()) {
                writer.write("\nFollowing item(s) were added to version v" + newVersion + ":\n");
                for (NaaccrDictionaryItem item : addedItems)
                    writer.write(" - " + item.getNaaccrId() + " (#" + item.getNaaccrNum() + ") - " + item.getNaaccrName() + "\n");
            }

            // removed
            if (!removedItems.isEmpty()) {
                writer.write("\nFollowing item(s) were removed from v" + newVersion + ":\n");
                for (NaaccrDictionaryItem item : removedItems)
                    writer.write(" - " + item.getNaaccrId() + " (#" + item.getNaaccrNum() + ") - " + item.getNaaccrName() + "\n");
            }

            // ID change
            if (!modifiedItemsId.isEmpty()) {
                writer.write("\nFollowing item(s) got their ID changed in v" + newVersion + ":\n");
                for (Pair<NaaccrDictionaryItem, NaaccrDictionaryItem> pair : modifiedItemsId)
                    writer.write(" - " + pair.getLeft().getNaaccrName() + " (#" + pair.getLeft().getNaaccrNum() + ") - from \"" + pair.getLeft().getNaaccrId()
                            + "\" to \"" + pair.getRight().getNaaccrId() + "\"\n");
            }

            // name change
            if (!modifiedItemsName.isEmpty()) {
                writer.write("\nFollowing item(s) got their name changed in v" + newVersion + ":\n");
                for (Pair<NaaccrDictionaryItem, NaaccrDictionaryItem> pair : modifiedItemsName)
                    writer.write(" - " + pair.getLeft().getNaaccrId() + " (#" + pair.getLeft().getNaaccrNum() + ") - from \"" + pair.getLeft().getNaaccrName()
                            + "\" to \"" + pair.getRight().getNaaccrName() + "\"\n");
            }

            // length change
            if (!modifiedItemsLength.isEmpty()) {
                writer.write("\nFollowing item(s) got their length changed in v" + newVersion + ":\n");
                for (Pair<NaaccrDictionaryItem, NaaccrDictionaryItem> pair : modifiedItemsLength)
                    writer.write(" - " + pair.getLeft().getNaaccrId() + " (#" + pair.getLeft().getNaaccrNum() + ") - from \"" + pair.getLeft().getLength()
                            + "\" to \"" + pair.getRight().getLength() + "\"\n");
            }

            // level change
            if (!modifiedItemsLevel.isEmpty()) {
                writer.write("\nFollowing item(s) got their data level changed in v" + newVersion + ":\n");
                for (Pair<NaaccrDictionaryItem, NaaccrDictionaryItem> pair : modifiedItemsLevel)
                    writer.write(" - " + pair.getLeft().getNaaccrId() + " (#" + pair.getLeft().getNaaccrNum() + ") - from \"" + pair.getLeft().getParentXmlElement()
                            + "\" to \"" + pair.getRight().getParentXmlElement() + "\"\n");
            }

            // type change
            if (!modifiedItemsType.isEmpty()) {
                writer.write("\nFollowing item(s) got their data type changed in v" + newVersion + ":\n");
                for (Pair<NaaccrDictionaryItem, NaaccrDictionaryItem> pair : modifiedItemsType)
                    writer.write(" - " + pair.getLeft().getNaaccrId() + " (#" + pair.getLeft().getNaaccrNum() + ") - from \"" + pair.getLeft().getDataType()
                            + "\" to \"" + pair.getRight().getDataType() + "\"\n");
            }

            // padding change
            if (!modifiedItemsPadding.isEmpty()) {
                writer.write("\nFollowing item(s) got their padding changed in v" + newVersion + ":\n");
                for (Pair<NaaccrDictionaryItem, NaaccrDictionaryItem> pair : modifiedItemsPadding)
                    writer.write(" - " + pair.getLeft().getNaaccrId() + " (#" + pair.getLeft().getNaaccrNum() + ") - from \"" + pair.getLeft().getPadding()
                            + "\" to \"" + pair.getRight().getPadding() + "\"\n");
            }

            // trimming change
            if (!modifiedItemsTrimming.isEmpty()) {
                writer.write("\nFollowing item(s) got their trimming changed in v" + newVersion + ":\n");
                for (Pair<NaaccrDictionaryItem, NaaccrDictionaryItem> pair : modifiedItemsTrimming)
                    writer.write(" - " + pair.getLeft().getNaaccrId() + " (#" + pair.getLeft().getNaaccrNum() + ") - from \"" + pair.getLeft().getTrim()
                            + "\" to \"" + pair.getRight().getTrim() + "\"\n");
            }

            // unlimited text change
            if (!modifiedItemsUnlimitedText.isEmpty()) {
                writer.write("\nFollowing item(s) got their allow-unlimited attribute changed in v" + newVersion + ":\n");
                for (Pair<NaaccrDictionaryItem, NaaccrDictionaryItem> pair : modifiedItemsUnlimitedText)
                    writer.write(" - " + pair.getLeft().getNaaccrId() + " (#" + pair.getLeft().getNaaccrNum() + ") - from \"" + pair.getLeft().getAllowUnlimitedText()
                            + "\" to " + (pair.getRight().getAllowUnlimitedText() == null ? "<not specified>" : ("\"" + pair.getRight().getAllowUnlimitedText() + "\"")) + "\n");
            }
        }
        System.out.println("Created " + outputFile.getPath());
        System.out.println("\n");
        Files.readAllLines(outputFile.toPath()).forEach(System.out::println);
    }
}
