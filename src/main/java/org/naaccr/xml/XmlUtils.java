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
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
import com.thoughtworks.xstream.io.xml.StaxDriver;

// TODO have to add support for "deprecated items"
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
        
        // get the standard dictionary 
        NaaccrDictionary dictionary = getStandardDictionary();
        
        // create the reader and writer and translates the incoming lines into patient objects before writting those...
        try (PatientXmlWriter writer = new PatientXmlWriter(new OutputStreamWriter(createOutputStream(xmlFile), StandardCharsets.UTF_8))) {
            try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(createInputStream(flatFile)))) {
                String line = reader.readLine();
                while (line != null) {
                    writer.writePatient(createPatientFromLine(line, format, dictionary));
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

        // get the standard dictionary
        NaaccrDictionary dictionary = getStandardDictionary();

        // create the reader and writer and translates the incoming lines into patient objects before writting those...
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(createOutputStream(flatFile)))) {
            try (PatientXmlReader reader = new PatientXmlReader(new InputStreamReader(createInputStream(xmlFile), StandardCharsets.UTF_8))) {
                Patient patient = reader.readPatient();
                while (patient != null) {
                    writer.write(createLineFromPatient(patient, format, dictionary));
                    writer.newLine();
                    patient = reader.readPatient();
                }
            }
        }
    }

    public static NaaccrDictionary getStandardDictionary(){
        try {
            URL standardDiciontaryUrl = Thread.currentThread().getContextClassLoader().getResource("fabian/naaccr-dictionary-v14.csv");
            NaaccrDictionary dictionary =  DictionaryUtils.readDictionary(standardDiciontaryUrl, DictionaryUtils.NAACCR_DICTIONARY_FORMAT_CSV);
            Collections.sort(dictionary.getItems(), new Comparator<NaaccrDictionaryItem>() {
                @Override
                public int compare(NaaccrDictionaryItem o1, NaaccrDictionaryItem o2) {
                    return o1.getStartColumn().compareTo(o2.getStartColumn());
                }
            });
            return dictionary;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to get standard dictionary!", e);
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
    
    public static Patient createPatientFromLine(String line, String format, NaaccrDictionary dictionary) throws IOException {
        Patient patient = new Patient();
        Tumor tumor = new Tumor();
        patient.getTumors().add(tumor);
        
        String recType = getRecordTypeFromFormat(format);
        
        for (NaaccrDictionaryItem itemDef : dictionary.getItems()) {
            if (itemDef.getRecordTypes().contains(recType)) {
                int start = itemDef.getStartColumn();
                int end = start + itemDef.getLength() - 1;
                if (end <= line.length()) {
                    String value = line.substring(start, end).trim();
                    if (!value.isEmpty()) {
                        Item item = new Item();
                        item.setNum(itemDef.getNumber());
                        item.setValue(value);
                        if (NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentElement()))
                            patient.getItems().add(item);
                        else if (NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentElement()))
                            tumor.getItems().add(item);
                        else
                            throw new IOException("Unsupported parent element: " + itemDef.getParentElement());
                    }
                }
            }
        }
        
        return patient;
    }

    public static String createLineFromPatient(Patient patient, String format, NaaccrDictionary dictionary) throws IOException {
        StringBuilder line = new StringBuilder();
        
        String recType = getRecordTypeFromFormat(format);

        int currentIndex = 1;
        for (NaaccrDictionaryItem itemDef : dictionary.getItems()) {
            if (itemDef.getStartColumn() != null && itemDef.getLength() != null && itemDef.getRecordTypes().contains(recType)) {
                int start = itemDef.getStartColumn();
                int length = itemDef.getLength();
                int end = start + length - 1;

                // adjust for the "leading" gap
                if (start > currentIndex)
                    for (int i = 0; i < start - currentIndex; i++)
                        line.append(' ');
                currentIndex = start;
                
                Item item;
                if (NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentElement()))
                    item = patient.getItemByNumber(itemDef.getNumber());
                else if (NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentElement()))
                    item = patient.getTumors().get(0).getItemByNumber(itemDef.getNumber());
                else
                    throw new IOException("Unsupported parent element: " + itemDef.getParentElement());
                
                if (item != null) {
                    String value = item.getValue();
                    if (value != null) {
                        if (value.length() > length)
                            value = value.substring(0, length);
                        // TODO add more validation here
                        line.append(value);
                        currentIndex = start + value.length();
                    }
                }

                // adjust for the "trailing" gap
                if (currentIndex <= end)
                    for (int i = 0; i < end - currentIndex + 1; i++)
                        line.append(' ');
                currentIndex = end + 1;
            }
        }
        return line.toString();
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
            options.setPreset(3); // this makes a huge difference in terms of time vs size, I think it should be exposed to the user...
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
        File outputFile = new File(System.getProperty("user.dir") + "/build/test.xml.gz");
        long start = System.currentTimeMillis();
        flatToXml(inputFile, outputFile, NAACCR_FILE_FORMAT_14_INCIDENCE);
        System.out.println("Done flat to XML in " + (System.currentTimeMillis() - start) + "ms");

        inputFile = new File(System.getProperty("user.dir") + "/build/test.xml.gz");
        outputFile = new File(System.getProperty("user.dir") + "/build/test.txt.gz");
        start = System.currentTimeMillis();
        xmlToFlat(inputFile, outputFile, NAACCR_FILE_FORMAT_14_INCIDENCE);
        System.out.println("Done XML to flat in " + (System.currentTimeMillis() - start) + "ms");        
    }
}
