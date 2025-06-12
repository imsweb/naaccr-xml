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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.imsweb.naaccr.api.client.NaaccrApiClient;
import com.imsweb.naaccr.api.client.entity.NaaccrDataItem;

@SuppressWarnings("ConstantValue")
public class EvaluateNewVersion {

    public static void main(String[] args) throws IOException {
        String version1 = "25";
        String version2 = "26";

        boolean allowCaching = false; // this should only be true when fixing the comparison logic...

        System.out.println();
        System.out.println("Comparing version " + version1 + " and " + version2 + " from API on " + SimpleDateFormat.getDateTimeInstance().format(new Date()));

        ObjectMapper mapper = new ObjectMapper();
        File file1 = new File(System.getProperty("user.dir") + "\\build\\items1.json");
        File file2 = new File(System.getProperty("user.dir") + "\\build\\items2.json");
        if (file1.exists() && !allowCaching)
            FileUtils.deleteQuietly(file1);
        if (file2.exists() && !allowCaching)
            FileUtils.deleteQuietly(file2);

        List<NaaccrDataItem> items1;
        if (file1.exists())
            items1 = mapper.readValue(file1, mapper.getTypeFactory().constructCollectionType(List.class, NaaccrDataItem.class));
        else {
            items1 = NaaccrApiClient.getInstance().getDataItems(version1);
            if (allowCaching)
                mapper.writeValue(file1, items1);
        }
        System.out.println("> Got " + items1.size() + " items for " + version1);

        List<NaaccrDataItem> items2;
        if (file2.exists())
            items2 = mapper.readValue(file2, mapper.getTypeFactory().constructCollectionType(List.class, NaaccrDataItem.class));
        else {
            items2 = NaaccrApiClient.getInstance().getDataItems(version2);
            if (allowCaching)
                mapper.writeValue(file2, items2);
        }
        System.out.println("> Got " + items2.size() + " items" + " for " + version2);

        Map<Number, NaaccrDataItem> oldItems = items1.stream().collect(Collectors.toMap(NaaccrDataItem::getItemNumber, i -> i));

        List<NaaccrDataItem> addedItems = new ArrayList<>();
        List<NaaccrDataItem> removedItems = new ArrayList<>();
        List<Pair<NaaccrDataItem, NaaccrDataItem>> commonItems = new ArrayList<>();
        for (NaaccrDataItem item : items2) {

            // temporary fix for items mixing they data type
            if (item.getItemDataType() == null)
                item.setItemDataType("text");

            if (item.getVersionRetired() != null) {
                if (version2.equals(item.getVersionRetired()))
                    removedItems.add(item);
            }
            else {
                NaaccrDataItem oldItem = oldItems.get(item.getItemNumber());
                if (oldItem != null) {

                    // temporary fix for items mixing they data type
                    if (oldItem.getItemDataType() == null)
                        oldItem.setItemDataType("text");

                    commonItems.add(Pair.of(oldItem, item));
                }
                else
                    addedItems.add(item);
            }
        }

        Map<NaaccrDataItem, List<String>> differences = new HashMap<>();
        for (Pair<NaaccrDataItem, NaaccrDataItem> pair : commonItems) {
            List<String> diff = compareItems(pair.getLeft(), pair.getRight());
            if (!diff.isEmpty())
                differences.put(pair.getRight(), diff);
        }

        System.out.println();
        System.out.println("Items removed from version " + version2);
        if (removedItems.isEmpty())
            System.out.println("> none");
        else {
            for (NaaccrDataItem item : removedItems.stream().sorted(Comparator.comparing(NaaccrDataItem::getItemName)).collect(Collectors.toList()))
                System.out.println("> " + item.getItemName() + " (#" + item.getItemNumber() + ")");

        }

        System.out.println();
        System.out.println("Items added in version " + version2);
        if (addedItems.isEmpty())
            System.out.println("> none");
        else {
            for (NaaccrDataItem item : addedItems.stream().sorted(Comparator.comparing(NaaccrDataItem::getItemName)).collect(Collectors.toList()))
                System.out.println("> " + item.getItemName() + " (#" + item.getItemNumber() + ")");

        }

        System.out.println();
        System.out.println("Items updated in version " + version2);
        if (differences.isEmpty())
            System.out.println("> none");
        else {
            for (NaaccrDataItem item : differences.keySet().stream().sorted(Comparator.comparing(NaaccrDataItem::getItemName)).collect(Collectors.toList())) {
                System.out.println("> " + item.getItemName() + " (#" + item.getItemNumber() + "):");
                for (String diff : differences.get(item))
                    System.out.println("  >> " + diff);
            }
        }

        System.out.println();
        System.out.println();
        System.out.println("*************   NEW ITEMS (don't copy this into the persisted summary file)");
        System.out.println();
        for (NaaccrDataItem item : addedItems) {
            System.out.println("        <ItemDef naaccrId=\"" + item.getXmlNaaccrId() + "\"");
            System.out.println("            naaccrNum=\"" + item.getItemNumber() + "\"");
            System.out.println("            naaccrName=\"" + item.getItemName() + "\"");
            System.out.println("            length=\"" + item.getItemLength() + "\"");
            System.out.println("            recordTypes=\"" + formatRecordTypes(item.getRecordTypes()) + "\"");
            if (!"text".equals(item.getItemDataType())) {
                System.out.println("            parentXmlElement=\"" + item.getXmlParentId() + "\"");
                System.out.println("            dataType=\"" + item.getItemDataType() + "\"/>");
            }
            else
                System.out.println("            parentXmlElement=\"" + item.getXmlParentId() + "\"/>");
            if (item.getFormat() != null)
                throw new IllegalStateException("Got a format, need to figure out if zero-padding needs to be applied!");
        }

        System.out.println();
        System.out.println();
        System.out.println("*************   EXTRA INFO NEEDED IN LAYOUT LIBRARY (don't copy this into the persisted summary file)");
        System.out.println();
        for (NaaccrDataItem item : addedItems) {
            System.out.println(item.getXmlNaaccrId());
            System.out.println(" > section: " + item.getSection());
        }
    }

    private static String formatRecordTypes(String value) {
        if (value.length() == 2)
            return "A,M";
        if (value.length() == 3)
            return "A,M,C";
        return "A,M,C,I";
    }

    private static List<String> compareItems(NaaccrDataItem item1, NaaccrDataItem item2) {
        List<String> differences = new ArrayList<>();

        // no need to compare the number since we use that as a key

        if (!item1.getXmlNaaccrId().equals(item2.getXmlNaaccrId()))
            differences.add("ID changed from " + item1.getXmlNaaccrId() + " to " + item2.getXmlNaaccrId());

        if (!item1.getItemName().equals(item2.getItemName()))
            differences.add("name changed from " + item1.getItemName() + " to " + item2.getItemName());

        if (!item1.getItemLength().equals(item2.getItemLength()))
            differences.add("length changed from " + item1.getItemLength() + " to " + item2.getItemLength());

        if (!item1.getXmlParentId().equals(item2.getXmlParentId()))
            differences.add("data level changed from " + item1.getXmlParentId() + " to " + item2.getXmlParentId());

        if (!item1.getItemDataType().equals(item2.getItemDataType()))
            differences.add("data type changed from " + item1.getItemDataType() + " to " + item2.getItemDataType());

        if (!item1.getRecordTypes().equals(item2.getRecordTypes()))
            differences.add("record types changed from " + item1.getRecordTypes() + " to " + item2.getRecordTypes());

        if (item1.getFormat() == null && item2.getFormat() != null)
            differences.add("format changed from <blank> to " + item2.getRecordTypes());
        if (item1.getFormat() != null && item2.getFormat() == null)
            differences.add("format changed from " + item1.getRecordTypes() + " to <blank>");
        if (item1.getFormat() != null && item2.getFormat() != null && !item1.getFormat().equals(item2.getFormat()))
            differences.add("format changed from " + item1.getFormat() + " to " + item2.getRecordTypes());

        return differences;
    }
}
