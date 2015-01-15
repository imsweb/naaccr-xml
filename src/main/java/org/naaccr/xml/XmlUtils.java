/*
 * Copyright (C) 2014 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

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

        // build the list of items for each XML level
        Map<String, List<NaaccrDictionaryItem>> items = buildItemLists(format);
        
        // TODO FPD change this to use the PatientWriter

        // convert the input stream into the outputstream...
        try (Writer writer = new OutputStreamWriter(createOutputStream(xmlFile))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n\r\n");
            PrettyPrintWriter xmlWriter = new PrettyPrintWriter(writer); // same as SaxWriter, but handles the indentation and new lines...
            xmlWriter.startNode("NaaccrDataExchange");
            try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(createInputStream(flatFile)))) {
                String line = reader.readLine();
                boolean wroteRoot = false; // root is actually based on the first line...
                while (line != null) {
                    if (!wroteRoot) {
                        writeXmlBlock(items.get(NAACCR_XML_TAG_ROOT), line, xmlWriter);
                        wroteRoot = true;
                    }
                    xmlWriter.startNode("Patient");
                    writeXmlBlock(items.get(NAACCR_XML_TAG_PATIENT), line, xmlWriter);
                    xmlWriter.startNode("Tumor");
                    writeXmlBlock(items.get(NAACCR_XML_TAG_TUMOR), line, xmlWriter);
                    xmlWriter.endNode();
                    xmlWriter.endNode();
                    line = reader.readLine();
                }
            }
            xmlWriter.endNode();
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

        // build the list of items for each XML level
        Map<String, List<NaaccrDictionaryItem>> items = buildItemLists(format);

        // TODO FPD change this to use the PatientReader
        
        // convert the input stream into the outputstream...
        try (Writer writer = new OutputStreamWriter(createOutputStream(flatFile))) {
            try (Reader reader = new InputStreamReader(createInputStream(xmlFile))) {

                Map<Integer, String> values = new HashMap<>();
                XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(reader);
                while (xmlReader.hasNext()) {
                    xmlReader.next();
                    if (xmlReader.getEventType() == XMLStreamReader.START_ELEMENT) {
                        //if (NAACCR_XML_TAG_ITEM.equals(xmlReader.getLocalName())) {
                        //    NAACCR_XML_TAG_ITEM
                        //}
                    }
                }
            }
            catch (XMLStreamException e) {
                throw new IOException("Unable to create XML reader", e);
            }
        }
    }

    private static String getRecordTypeFromFormat(String format) throws IOException {
        String recordType;
        if (format.endsWith("-abstract"))
            recordType = "A";
        else if (format.endsWith("-modified"))
            recordType = "M";
        else if (format.endsWith("-confidential"))
            recordType = "C";
        else if (format.endsWith("-incidence"))
            recordType = "I";
        else
            throw new IOException("Invalid file format: " + format);
        return recordType;
    }

    private static Map<String, List<NaaccrDictionaryItem>> buildItemLists(String format) throws IOException {

        // get the dictionary 
        URL standardDiciontaryUrl = Thread.currentThread().getContextClassLoader().getResource("fabian/naaccr-dictionary-v14.csv");
        NaaccrDictionary dictionary = DictionaryUtils.readDictionary(standardDiciontaryUrl, DictionaryUtils.NAACCR_DICTIONARY_FORMAT_CSV);

        // split the items by XML level
        List<NaaccrDictionaryItem> rootItems = new ArrayList<>(), patItems = new ArrayList<>(), tumorItems = new ArrayList<>();
        for (NaaccrDictionaryItem item : dictionary.getItems().values()) {
            if (item.getRecordType().contains(getRecordTypeFromFormat(format))) {
                if (NAACCR_XML_TAG_ROOT.equals(item.getParentElement()))
                    rootItems.add(item);
                else if (NAACCR_XML_TAG_PATIENT.equals(item.getParentElement()))
                    patItems.add(item);
                else if (NAACCR_XML_TAG_TUMOR.equals(item.getParentElement()))
                    tumorItems.add(item);
                else
                    throw new IOException("Unsupported parent element: " + item.getParentElement());
            }
        }

        // sort the list of items by their starting columns
        Comparator<NaaccrDictionaryItem> comparator = new Comparator<NaaccrDictionaryItem>() {
            @Override
            public int compare(NaaccrDictionaryItem o1, NaaccrDictionaryItem o2) {
                return o1.getStartColumn().compareTo(o2.getStartColumn());
            }
        };
        Collections.sort(rootItems, comparator);
        Collections.sort(patItems, comparator);
        Collections.sort(tumorItems, comparator);

        // crate final data structure
        Map<String, List<NaaccrDictionaryItem>> result = new HashMap<>();
        result.put(NAACCR_XML_TAG_ROOT, rootItems);
        result.put(NAACCR_XML_TAG_PATIENT, patItems);
        result.put(NAACCR_XML_TAG_TUMOR, tumorItems);

        return result;
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
            options.setPreset(1); // this makes a huge difference in terms of time vs size, I think it should be exposed to the user...
            os = new XZOutputStream(os, options);
        }

        return os;
    }

    // TODO FPD don't think this will actually be needed...
    private static void writeXmlBlock(List<NaaccrDictionaryItem> items, String line, PrettyPrintWriter writer) {
        for (NaaccrDictionaryItem item : items) {
            int start = item.getStartColumn();
            int end = start + item.getLength() - 1;
            if (end <= line.length()) {
                String value = line.substring(start, end).trim();
                if (!value.isEmpty()) {
                    writer.startNode("Item");
                    writer.addAttribute("num", item.getNumber().toString());
                    writer.setValue(value);
                    writer.endNode();
                }
            }
        }
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
            if (item.getValue() != null)
                writer.setValue(item.getValue());
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Item item = new Item();
            String num = reader.getAttribute("num");
            if (num != null)
                item.setNum(Integer.valueOf(num));
            item.setValue(reader.getValue());
            return item;
        }
    }

    // testing method, will be removed eventually...
    public static void main(String[] args) throws Exception {
        File inputFile = new File(System.getProperty("user.dir") + "/src/main/resources/data/fake-naaccr14inc-10000-rec.txt.gz");
        File outputFile = new File(System.getProperty("user.dir") + "/build/test.xml.xz");
        long start = System.currentTimeMillis();
        flatToXml(inputFile, outputFile, NAACCR_FILE_FORMAT_14_INCIDENCE);
        System.out.println("Done in " + (System.currentTimeMillis() - start) + "ms");
    }
}
