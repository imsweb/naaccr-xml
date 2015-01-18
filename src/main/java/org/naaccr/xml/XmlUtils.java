/*
 * Copyright (C) 2014 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;

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
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @throws IOException
     */
    public static void flatToXml(File flatFile, File xmlFile, String format, NaaccrDictionary nonStandardDictionary) throws IOException {
        if (flatFile == null)
            throw new IOException("Source flat file is required");
        if (!flatFile.exists())
            throw new IOException("Source flat file must exist");
        if (format == null)
            throw new IOException("File format is required");
        if (!xmlFile.getParentFile().exists())
            throw new IOException("Target folder must exist");

        // create the reader and writer and let them do all the work!
        try (PatientXmlWriter writer = new PatientXmlWriter(new OutputStreamWriter(createOutputStream(xmlFile), StandardCharsets.UTF_8), format, nonStandardDictionary)) {
            try (PatientFlatReader reader = new PatientFlatReader(new InputStreamReader(createInputStream(flatFile), StandardCharsets.UTF_8), format, nonStandardDictionary)) {
                Patient patient = reader.readPatient();
                while (patient != null) {
                    writer.writePatient(patient);
                    patient = reader.readPatient();
                }
            }
        }
    }

    /**
     * Translates an XML data file into a flat data file.
     * @param xmlFile source XML data file, must exists
     * @param flatFile target flat data file
     * @param format expected NAACCR format
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @throws IOException
     */
    public static void xmlToFlat(File xmlFile, File flatFile, String format, NaaccrDictionary nonStandardDictionary) throws IOException {
        if (xmlFile == null)
            throw new IOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new IOException("Source XML file must exist");
        if (format == null)
            throw new IOException("File format is required");
        if (!flatFile.getParentFile().exists())
            throw new IOException("Target folder must exist");

        // create the reader and writer and let them do all the work!
        try (PatientFlatWriter writer = new PatientFlatWriter(new OutputStreamWriter(createOutputStream(flatFile), StandardCharsets.UTF_8), format, nonStandardDictionary)) {
            try (PatientXmlReader reader = new PatientXmlReader(new InputStreamReader(createInputStream(xmlFile), StandardCharsets.UTF_8), format, nonStandardDictionary)) {
                Patient patient = reader.readPatient();
                while (patient != null) {
                    writer.writePatient(patient);
                    patient = reader.readPatient();
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

    private static InputStream createInputStream(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        if (file.getName().endsWith(".gz"))
            is = new GZIPInputStream(is);

        return is;
    }

    private static OutputStream createOutputStream(File file) throws IOException {
        OutputStream os = new FileOutputStream(file);

        if (file.getName().endsWith(".gz"))
            os = new GZIPOutputStream(os);

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

        URL myDictionaryUrl = Thread.currentThread().getContextClassLoader().getResource("fabian/fab-dictionary.csv");
        NaaccrDictionary myDictionary = DictionaryUtils.readDictionary(myDictionaryUrl, DictionaryUtils.NAACCR_DICTIONARY_FORMAT_CSV);

        File inputFile = new File(System.getProperty("user.dir") + "/src/main/resources/data/fake-naaccr14inc-10000-rec.txt.gz");
        File outputFile = new File(System.getProperty("user.dir") + "/build/test.xml.gz");
        long start = System.currentTimeMillis();
        flatToXml(inputFile, outputFile, NAACCR_FILE_FORMAT_14_INCIDENCE, null);
        System.out.println("Done translating flat-file to XML (10,000 records) using standard dictionary in " + (System.currentTimeMillis() - start) + "ms - see 'test.xml.gz'");

        File inputFile2 = new File(System.getProperty("user.dir") + "/build/test.xml.gz");
        File outputFile2 = new File(System.getProperty("user.dir") + "/build/test.txt.gz");
        long start2 = System.currentTimeMillis();
        xmlToFlat(inputFile2, outputFile2, NAACCR_FILE_FORMAT_14_INCIDENCE, null);
        System.out.println("Done translation XML back to flat-file (10,000 records) using standard dictionary in " + (System.currentTimeMillis() - start2) + "ms - see 'test.txt.gz'");

        Patient patient = new Patient();
        patient.getItems().add(new Item("nameLast", "DEPRY"));
        patient.getItems().add(new Item("nameFirst", "FABIAN"));
        patient.getTumors().add(new Tumor());
        patient.getTumors().get(0).getItems().add(new Item("primarySite", "C619"));
        patient.getTumors().get(0).getItems().add(new Item("hosptialAbstractorId", "FDEPRY"));
        File outputFile3 = new File(System.getProperty("user.dir") + "/build/user-dictionary-test-names.xml");
        PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(outputFile3), NAACCR_FILE_FORMAT_14_ABSTRACT, myDictionary);
        writer.writePatient(patient);
        writer.close();
        System.out.println("Done creating XML file using non-standard items (item referenced by ID) - see 'user-dictionary-test-names.xml'");

        patient = new Patient();
        patient.getItems().add(new Item(2230, "DEPRY"));
        patient.getItems().add(new Item(2240, "FABIAN"));
        patient.getTumors().add(new Tumor());
        patient.getTumors().get(0).getItems().add(new Item(400, "C619"));
        patient.getTumors().get(0).getItems().add(new Item(10001, "FDEPRY"));
        outputFile3 = new File(System.getProperty("user.dir") + "/build/user-dictionary-test-numbers.xml");
        writer = new PatientXmlWriter(new FileWriter(outputFile3), NAACCR_FILE_FORMAT_14_ABSTRACT, myDictionary);
        writer.writePatient(patient);
        writer.close();
        System.out.println("Done creating XML file using non-standard items (item referenced by number) - see 'user-dictionary-test-numbers.xml'");

        patient.getItems().add(new Item("recordType", "A"));
        patient.getItems().add(new Item("naaccrRecordVersion", "140"));
        outputFile3 = new File(System.getProperty("user.dir") + "/build/user-dictionary-test.txt");
        PatientFlatWriter writer3 = new PatientFlatWriter(new FileWriter(outputFile3), NAACCR_FILE_FORMAT_14_ABSTRACT, myDictionary);
        writer3.writePatient(patient);
        writer3.close();
        System.out.println("Done creating flat-file using non-standard items in State Requestor Items - see 'user-dictionary-test.txt'");
    }
}
