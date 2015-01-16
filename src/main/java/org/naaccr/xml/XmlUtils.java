/*
 * Copyright (C) 2014 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionaryItem;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

// TODO have to add support for "deprecated items"
// TODO patient should correspond to several lines and vice-versa; this is not supported right now...
// TODO add data schema, maybe generate the entiteis from JAXB, not sure it's necessary though...
// TODO how about using the XStream.toXML() and XStream.fromXML()?

public class XmlUtils {

    // supported NAACCR formats
    public static String NAACCR_FILE_FORMAT_14_ABSTRACT = "naaccr-14-abstract";
    public static String NAACCR_FILE_FORMAT_14_MODIFIED = "naaccr-14-modified";
    public static String NAACCR_FILE_FORMAT_14_CONFIDENTIAL = "naaccr-14-confidential";
    public static String NAACCR_FILE_FORMAT_14_INCIDENCE = "naaccr-14-incidence";

    // structure tags in the XML
    public static String NAACCR_XML_TAG_ROOT = "NaaccrDataExchange";
    public static String NAACCR_XML_TAG_PATIENT = "Patient";
    public static String NAACCR_XML_TAG_TUMOR = "Tumor";
    public static String NAACCR_XML_TAG_ITEM = "Item";

    /**
     * Translates a flat data file into an XML data file.
     * @param flatFile source flat data file, must exists
     * @param xmlFile target XML data file
     * @param format expected NAACCR format
     * @throws IOException
     */
    public static void flatToXml(File flatFile, File xmlFile, String format) throws IOException {
        if (flatFile == null)
            throw new IOException("Source flat file is required");
        if (!flatFile.exists())
            throw new IOException("Source flat file must exist");
        if (format == null)
            throw new IOException("Expected NAACCR format is required");

        // create the dictionary
        RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(format, getStandardDictionary(), null);

        // create the reader and writer and translates the incoming lines into patient objects before writting those...
        try (PatientXmlWriter writer = new PatientXmlWriter(new OutputStreamWriter(createOutputStream(xmlFile), StandardCharsets.UTF_8))) {
            try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(createInputStream(flatFile)))) {
                String line = reader.readLine();
                while (line != null) {
                    writer.writePatient(createPatientFromLine(line, dictionary));
                    line = reader.readLine();
                }
            }
        }
    }

    /**
     * Translates an XML data file into a flat data file.
     * @param xmlFile source XML data file, must exists
     * @param flatFile target flat data file
     * @param format expected NAACCR format
     * @throws IOException
     */
    public static void xmlToFlat(File xmlFile, File flatFile, String format) throws IOException {
        if (xmlFile == null)
            throw new IOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new IOException("Source XML file must exist");
        if (format == null)
            throw new IOException("Expected NAACCR format is required");

        // create the dictionary
        RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(format, getStandardDictionary(), null);

        // create the reader and writer and translates the incoming lines into patient objects before writting those...
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(createOutputStream(flatFile)))) {
            try (PatientXmlReader reader = new PatientXmlReader(new InputStreamReader(createInputStream(xmlFile), StandardCharsets.UTF_8))) {
                Patient patient = reader.readPatient();
                while (patient != null) {
                    writer.write(createLineFromPatient(patient, dictionary));
                    writer.newLine();
                    patient = reader.readPatient();
                    break;
                }
            }
        }
    }

    public static NaaccrDictionary getStandardDictionary() {
        try {
            URL standardDiciontaryUrl = Thread.currentThread().getContextClassLoader().getResource("fabian/naaccr-dictionary-v14.csv");
            return DictionaryUtils.readDictionary(standardDiciontaryUrl, DictionaryUtils.NAACCR_DICTIONARY_FORMAT_CSV);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to get standard dictionary!", e);
        }
    }

    public static Patient createPatientFromLine(String line, RuntimeNaaccrDictionary dictionary) throws IOException {
        Patient patient = new Patient();
        Tumor tumor = new Tumor();
        patient.getTumors().add(tumor);

        for (RuntimeNaaccrDictionaryItem itemDef : dictionary.getItems()) {
            if (itemDef.getParentXmlElement() != null) {

                // check where the item needs to go in the patient structure
                List<Item> targetList;
                if (NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentXmlElement()))
                    targetList = patient.getItems();
                else if (NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentXmlElement()))
                    targetList = tumor.getItems();
                else
                    throw new IOException("Unsupported parent element: " + itemDef.getParentXmlElement());

                int start = itemDef.getStartColumn();
                int end = start + itemDef.getLength() - 1;

                if (end <= line.length()) {
                    String value = line.substring(start, end);
                    String trimmedValue = value.trim();

                    // never trim a group field unless it's completely empty (or we would lose the info of which child value is which)
                    if (itemDef.getSubItems().isEmpty() || trimmedValue.isEmpty())
                        value = trimmedValue;

                    if (!value.isEmpty()) {
                        Item item = new Item();
                        item.setId(itemDef.getId());
                        item.setNum(itemDef.getNumber());
                        item.setValue(value);
                        targetList.add(item);

                        // handle the sub-items if any
                        if (!itemDef.getSubItems().isEmpty()) {
                            for (RuntimeNaaccrDictionaryItem subItemDef : itemDef.getSubItems()) {
                                start = subItemDef.getStartColumn();
                                end = start + subItemDef.getLength() - 1;

                                value = line.substring(start - 1, end).trim();
                                if (!value.isEmpty()) {
                                    Item subItem = new Item();
                                    subItem.setId(itemDef.getId());
                                    subItem.setNum(itemDef.getNumber());
                                    subItem.setValue(value);
                                    targetList.add(subItem);
                                }
                            }
                        }
                    }
                }
            }
        }

        return patient;
    }

    public static String createLineFromPatient(Patient patient, RuntimeNaaccrDictionary dictionary) throws IOException {
        StringBuilder line = new StringBuilder();

        int currentIndex = 1;
        for (RuntimeNaaccrDictionaryItem itemDef : dictionary.getItems()) {
            if (itemDef.getParentXmlElement() != null && itemDef.getStartColumn() != null && itemDef.getLength() != null) {
                int start = itemDef.getStartColumn();
                int length = itemDef.getLength();
                int end = start + length - 1;

                System.out.println("Start: " + start + "; end: " + end + "; length: " + length);
                
                // adjust for the "leading" gap
                if (start > currentIndex)
                    for (int i = 0; i < start - currentIndex; i++)
                        line.append(' ');
                currentIndex = start;

                // get value; if the item defines sub-items, always use the sub-items
                if (!itemDef.getSubItems().isEmpty()) {
                    for (RuntimeNaaccrDictionaryItem subItemDef : itemDef.getSubItems()) {
                        int subStart = subItemDef.getStartColumn();
                        int subLength = subItemDef.getLength();
                        int subEnd = start + length - 1;

                        // adjust for the "leading" gap within the sub-items
                        if (subStart > currentIndex)
                            for (int i = 0; i < subStart - currentIndex; i++)
                                line.append(' ');
                        currentIndex = subStart;

                        if (subEnd <= end) { // do not write the current sub-item out if it can potentially go out of the space
                            String value = getValueForItem(subItemDef, patient, patient.getTumors().get(0));
                            if (value == null)
                                value = "";
                            if (value.length() > subLength)
                                value = value.substring(0, subLength);
                            line.append(value);
                            currentIndex = subStart + value.length();
                        }
                    }

                    // adjust for the "trailing" gap within the sub-items
                    if (currentIndex <= end)
                        for (int i = 0; i < end - currentIndex + 1; i++)
                            line.append(' ');
                    currentIndex = end + 1;
                }
                else {
                    String value = getValueForItem(itemDef, patient, patient.getTumors().get(0)); // TODO don't just use the first tumor, create several lines instead...
                    if (value != null) {
                        if (value.length() > length)
                            value = value.substring(0, length);
                        // TODO add more validation here
                        line.append(value);
                        currentIndex = start + value.length();
                    }

                    // adjust for the "trailing" gap
                    if (currentIndex <= end)
                        for (int i = 0; i < end - currentIndex + 1; i++)
                            line.append(' ');
                    currentIndex = end + 1;
                }
            }
        }
        return line.toString();
    }

    private static String getValueForItem(RuntimeNaaccrDictionaryItem itemDef, Patient patient, Tumor tumor) throws IOException {
        Item item;
        if (NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentXmlElement()))
            item = patient.getItem(itemDef.getId(), itemDef.getNumber());
        else if (NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentXmlElement()))
            item = tumor.getItem(itemDef.getId(), itemDef.getNumber()); // TODO don't use just the first tumor, create one line per
        else
            throw new IOException("Unsupported parent element: " + itemDef.getParentXmlElement());

        return item == null ? null : item.getValue();
    }

    private static InputStream createInputStream(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        if (file.getName().endsWith(".gz"))
            is = new GZIPInputStream(is);
        else if (file.getName().endsWith(".xz"))
            is = new XZInputStream(is);

        return is;
    }

    private static OutputStream createOutputStream(File file) throws IOException {
        OutputStream os = new FileOutputStream(file);

        if (file.getName().endsWith(".gz"))
            os = new GZIPOutputStream(os);
        else if (file.getName().endsWith(".xz")) {
            LZMA2Options options = new LZMA2Options();
            options.setPreset(0); // this makes a huge difference in terms of time vs size, I think it should be exposed to the user...
            os = new XZOutputStream(os, options);
        }

        return os;
    }

    public static XStream getXStream() {
        XStream xstream = new XStream(new StaxDriver());
        xstream.alias("Patient", Patient.class);
        xstream.alias("Tumor", Tumor.class);
        xstream.alias("Item", Item.class);
        xstream.addImplicitCollection(Patient.class, "items", Item.class);
        xstream.addImplicitCollection(Patient.class, "tumors", Tumor.class);
        xstream.addImplicitCollection(Tumor.class, "items", Item.class);
        xstream.registerConverter(new ItemConverter());
        return xstream;
    }

    public static class ItemConverter implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return type.equals(Item.class);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            Item item = (Item)source;
            if (item.getNum() != null)
                writer.addAttribute("num", item.getNum().toString());
            else if (item.getId() != null)
                writer.addAttribute("id", item.getId());
            else
                throw new RuntimeException("ID or Number is required for any item.");
            if (item.getValue() != null)
                writer.setValue(item.getValue());
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Item item = new Item();
            String num = reader.getAttribute("num");
            if (num != null)
                item.setNum(Integer.valueOf(num));
            item.setId(reader.getAttribute("id"));
            item.setValue(reader.getValue());
            return item;
        }
    }

    // testing method, will be removed eventually...
    public static void main(String[] args) throws Exception {
        /**
        File inputFile = new File(System.getProperty("user.dir") + "/src/main/resources/data/fake-naaccr14inc-10000-rec.txt.gz");
        File outputFile = new File(System.getProperty("user.dir") + "/build/test.xml.gz");
        long start = System.currentTimeMillis();
        flatToXml(inputFile, outputFile, NAACCR_FILE_FORMAT_14_INCIDENCE);
        System.out.println("Done flat to XML in " + (System.currentTimeMillis() - start) + "ms");
         */

         File inputFile = new File(System.getProperty("user.dir") + "/build/test.xml.gz");
         File outputFile = new File(System.getProperty("user.dir") + "/build/test.txt.gz");
         long start = System.currentTimeMillis();
         xmlToFlat(inputFile, outputFile, NAACCR_FILE_FORMAT_14_INCIDENCE);
         System.out.println("Done XML to flat in " + (System.currentTimeMillis() - start) + "ms");
    }
}
