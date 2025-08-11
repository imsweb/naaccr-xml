/*
 * Copyright (C) 2025 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.imsweb.naaccr.api.client.NaaccrApiClient;
import com.imsweb.naaccr.api.client.entity.NaaccrDataItem;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

@SuppressWarnings("ConstantValue")
public class EvaluateUpdatedVersion {

    public static void main(String[] args) throws IOException {
        String version = "26";

        boolean allowCaching = false; // this should only be true when fixing the comparison logic...

        System.out.println();
        System.out.println("Comparing API version " + version + " to existing base dictionary on " + SimpleDateFormat.getDateTimeInstance().format(new Date()));

        ObjectMapper mapper = new ObjectMapper();
        File file = new File(System.getProperty("user.dir") + "\\build\\items1.json");
        if (file.exists() && !allowCaching)
            FileUtils.deleteQuietly(file);

        List<NaaccrDataItem> items;
        if (file.exists())
            items = mapper.readValue(file, mapper.getTypeFactory().constructCollectionType(List.class, NaaccrDataItem.class));
        else {
            items = NaaccrApiClient.getInstance().getDataItems(version);
            if (allowCaching)
                mapper.writeValue(file, items);
        }
        items = items.stream().filter(i -> i.getVersionRetired() == null).toList();
        System.out.println("> Got " + items.size() + " items for " + version);

        List<NaaccrDictionaryItem> existingItems = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(version + "0").getItems();
        System.out.println("> Got " + existingItems.size() + " items for base dictionary");

        Map<Number, NaaccrDictionaryItem> oldItems = existingItems.stream().collect(Collectors.toMap(NaaccrDictionaryItem::getNaaccrNum, i -> i));

        List<NaaccrDataItem> addedItems = new ArrayList<>();
        List<NaaccrDictionaryItem> removedItems = new ArrayList<>();
        List<Pair<NaaccrDictionaryItem, NaaccrDataItem>> commonItems = new ArrayList<>();
        Set<Integer> processed = new HashSet<>();
        for (NaaccrDataItem item : items) {

            // temporary fix for items mixing they data type
            if (item.getItemDataType() == null)
                item.setItemDataType("text");

            NaaccrDictionaryItem oldItem = oldItems.get(item.getItemNumber());
            if (oldItem != null)
                commonItems.add(Pair.of(oldItem, item));
            else
                addedItems.add(item);

            processed.add(item.getItemNumber());
        }
        for (NaaccrDictionaryItem item : existingItems)
            if (!processed.contains(item.getNaaccrNum()))
                removedItems.add(item);

        Map<NaaccrDataItem, List<String>> differences = new HashMap<>();
        for (Pair<NaaccrDictionaryItem, NaaccrDataItem> pair : commonItems) {
            List<String> diff = compareItems(pair.getLeft(), pair.getRight());
            if (!diff.isEmpty())
                differences.put(pair.getRight(), diff);
        }

        System.out.println();
        System.out.println("Items removed:");
        if (removedItems.isEmpty())
            System.out.println("> none");
        else {
            for (NaaccrDictionaryItem item : removedItems.stream().sorted(Comparator.comparing(NaaccrDictionaryItem::getNaaccrName)).collect(Collectors.toList()))
                System.out.println("> " + item.getNaaccrName() + " (#" + item.getNaaccrNum() + ")");

        }

        System.out.println();
        System.out.println("Items added:");
        if (addedItems.isEmpty())
            System.out.println("> none");
        else {
            for (NaaccrDataItem item : addedItems.stream().sorted(Comparator.comparing(NaaccrDataItem::getItemName)).collect(Collectors.toList()))
                System.out.println("> " + item.getItemName() + " (#" + item.getItemNumber() + ")");

        }

        System.out.println();
        System.out.println("Items updated:");
        if (differences.isEmpty())
            System.out.println("> none");
        else {
            for (NaaccrDataItem item : differences.keySet().stream().sorted(Comparator.comparing(NaaccrDataItem::getItemName)).collect(Collectors.toList())) {
                System.out.println("> " + item.getItemName() + " (#" + item.getItemNumber() + "):");
                for (String diff : differences.get(item))
                    System.out.println("  >> " + diff);
            }
        }
    }

    private static String formatRecordTypes(String value) {
        if (value == null)
            return "A,M,C,I";
        if (value.length() == 2)
            return "A,M";
        if (value.length() == 3)
            return "A,M,C";
        return "A,M,C,I";
    }

    private static List<String> compareItems(NaaccrDictionaryItem item1, NaaccrDataItem item2) {
        List<String> differences = new ArrayList<>();

        // no need to compare the number since we use that as a key

        if (!item1.getNaaccrId().equals(item2.getXmlNaaccrId()))
            differences.add("ID changed from \"" + item1.getNaaccrId() + "\" to \"" + item2.getXmlNaaccrId() + "\"");

        if (!item1.getNaaccrName().equals(item2.getItemName().trim()))
            differences.add("name changed from \"" + item1.getNaaccrName() + "\" to \"" + item2.getItemName().trim() + "\"");

        if (!item1.getLength().equals(item2.getItemLength()))
            differences.add("length changed from \"" + item1.getLength() + "\" to \"" + item2.getItemLength() + "\"");

        if (!item1.getParentXmlElement().equals(item2.getXmlParentId()))
            differences.add("data level changed from \"" + item1.getParentXmlElement() + "\" to \"" + item2.getXmlParentId() + "\"");

        if (!item1.getDataType().equals(item2.getItemDataType()))
            differences.add("data type changed from \"" + item1.getDataType() + "\" to \"" + item2.getItemDataType() + "\"");

        if (!formatRecordTypes(item1.getRecordTypes()).equals(formatRecordTypes(item2.getRecordTypes())))
            differences.add("record types changed from \"" + item1.getRecordTypes() + "\" to \"" + item2.getRecordTypes() + "\"");

        return differences;
    }
}
