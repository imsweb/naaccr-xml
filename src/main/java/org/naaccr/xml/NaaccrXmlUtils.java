/*
 * Copyright (C) 2014 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.NaaccrDataExchange;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

// TODO use a properties file with the exceptions so they can be shared with the DLL?
// TODO investigate using abstract base reader/writer that would be parametrized classes...
// TODO post something on XStream to expose the Path frmo the PartTracker
public class NaaccrXmlUtils {

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
     * @param options validating options
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @return the number of written patients
     * @throws IOException if there is problem reading/writing the files
     * @throws NaaccrValidationException if there is a problem validating the data
     */
    public static int flatToXml(File flatFile, File xmlFile, String format, NaaccrXmlOptions options, NaaccrDictionary nonStandardDictionary) throws IOException, NaaccrValidationException {
        if (flatFile == null)
            throw new IOException("Source flat file is required");
        if (!flatFile.exists())
            throw new IOException("Source flat file must exist");
        if (format == null)
            throw new IOException("File format is required");
        if (!NaaccrFormat.isFormatSupported(format))
            throw new IOException("Invalid file format");
        if (!xmlFile.getParentFile().exists())
            throw new IOException("Target folder must exist");

        // create the runtime dictionary
        RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(format, getStandardDictionary(), nonStandardDictionary);

        // make sure we have some options
        if (options == null)
            options = new NaaccrXmlOptions();

        // create the reader and writer and let them do all the work!
        int processedCount = 0;
        try (PatientXmlWriter writer = new PatientXmlWriter(createWriter(xmlFile), getStandardXStream(dictionary, options), dictionary)) {
            try (PatientFlatReader reader = new PatientFlatReader(createReader(flatFile), dictionary)) {
                Patient patient = reader.readPatient();
                while (patient != null) {
                    writer.writePatient(patient);
                    processedCount++;
                    patient = reader.readPatient();
                }
            }
        }

        return processedCount;
    }

    /**
     * Translates an XML data file into a flat data file.
     * @param xmlFile source XML data file, must exists
     * @param flatFile target flat data file
     * @param format expected NAACCR format
     * @param options validating options
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @return the number of written records
     * @throws IOException if there is problem reading/writing the files
     * @throws NaaccrValidationException if there is a problem validating the data
     */
    public static int xmlToFlat(File xmlFile, File flatFile, String format, NaaccrXmlOptions options, NaaccrDictionary nonStandardDictionary) throws IOException, NaaccrValidationException {
        if (xmlFile == null)
            throw new IOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new IOException("Source XML file must exist");
        if (format == null)
            throw new IOException("File format is required");
        if (!NaaccrFormat.isFormatSupported(format))
            throw new IOException("Invalid file format");
        if (!flatFile.getParentFile().exists())
            throw new IOException("Target folder must exist");

        // create the runtime dictionary
        RuntimeNaaccrDictionary runtimeDictionary = new RuntimeNaaccrDictionary(format, getStandardDictionary(), nonStandardDictionary);

        // make sure we have some options
        if (options == null)
            options = new NaaccrXmlOptions();

        // create the reader and writer and let them do all the work!
        int processedCount = 0;
        try (PatientFlatWriter writer = new PatientFlatWriter(createWriter(flatFile), runtimeDictionary)) {
            try (PatientXmlReader reader = new PatientXmlReader(createReader(xmlFile), getStandardXStream(runtimeDictionary, options))) {
                Patient patient = reader.readPatient();
                while (patient != null) {
                    writer.writePatient(patient);
                    processedCount += patient.getTumors().size();
                    patient = reader.readPatient();
                }
            }
        }

        return processedCount;
    }

    // takes care of the file encoding and compression...
    private static Reader createReader(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        if (file.getName().endsWith(".gz"))
            is = new GZIPInputStream(is);

        return new InputStreamReader(is, StandardCharsets.UTF_8);
    }

    // takes care of the file encoding and compression...
    private static Writer createWriter(File file) throws IOException {
        OutputStream os = new FileOutputStream(file);

        if (file.getName().endsWith(".gz"))
            os = new GZIPOutputStream(os);

        return new OutputStreamWriter(os, StandardCharsets.UTF_8);
    }

    /**
     * Reads an NAACCR XML data file and returns the corresponding data.
     * <br/>
     * ATTENTION: THIS METHOD WILL RETURN THE FULL CONTENT OF THE FILE AND IS NOT SUITABLE FOR LARGE FILE; CONSIDER USING A STREAM INSTEAD.
     * @param xmlFile source XML data file, must exists
     * @param format expected NAACCR format
     * @param options reading and validating options
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @throws IOException
     * @returns a <code>NaaccrDataExchange</code> object, never null
     */
    public static NaaccrDataExchange readXmlFile(File xmlFile, String format, NaaccrXmlOptions options, NaaccrDictionary nonStandardDictionary) throws IOException {
        if (xmlFile == null)
            throw new IOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new IOException("Source XML file must exist");
        if (format == null)
            throw new IOException("File format is required");
        if (!NaaccrFormat.isFormatSupported(format))
            throw new IOException("Invalid file format");

        // create the runtime dictionary
        RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(format, getStandardDictionary(), nonStandardDictionary);

        // make sure we have some options
        if (options == null)
            options = new NaaccrXmlOptions();
        
        // let XStream read the data
        NaaccrDataExchange data;
        try (Reader reader = createReader(xmlFile)) {
            data = (NaaccrDataExchange)getStandardXStream(dictionary, options).fromXML(reader);
        }

        return data;
    }

    /**
     * Writes the provided data to the requested XML file.
     * <br/>
     * ATTENTION: THIS METHOD REQUIRES THE ENTIRE DATA OBJECT TO BE IN MEMORY; CONSIDER USING A STREAM INSTEAD.
     * @param data a <code>NaaccrDataExchange</code> object, cannot be null
     * @param xmlFile target XML data file
     * @param format expected NAACCR format
     * @param options writing and validating options
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @throws IOException
     */
    public static void writeXmlFile(NaaccrDataExchange data, File xmlFile, String format, NaaccrXmlOptions options, NaaccrDictionary nonStandardDictionary) throws IOException {
        if (data == null)
            throw new IOException("Data is required");
        if (format == null)
            throw new IOException("File format is required");
        if (!xmlFile.getParentFile().exists())
            throw new IOException("Target folder must exist");
        if (!NaaccrFormat.isFormatSupported(format))
            throw new IOException("Invalid file format");

        // create the runtime dictionary
        RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(format, getStandardDictionary(), nonStandardDictionary);

        // make sure we have some options
        if (options == null)
            options = new NaaccrXmlOptions();

        // let XStream writer the data
        try (Writer writer = createWriter(xmlFile)) {
            PrettyPrintWriter prettyWriter = new PrettyPrintWriter(writer);
            try {
                getStandardXStream(dictionary, options).marshal(data, prettyWriter); // can't use xstream.toXML because it doesn't use a pretty formatting...
            }
            finally {
                prettyWriter.flush();
            }
        }
    }

    /**
     * Returns the NAACCR format of the given flat file.
     * @param flatFile provided data file
     * @return the NAACCR format, null if it cannot be determined
     */
    public static String getFormatFromFlatFile(File flatFile) {
        if (flatFile == null || !flatFile.exists())
            return null;

        String result = null;
        try (BufferedReader reader = new BufferedReader(createReader(flatFile))) {
            String line = reader.readLine();
            if (line != null && line.length() > 19) {
                String naaccrVersion = line.substring(16, 19).trim();
                String recordType = line.substring(0, 1).trim();
                switch (recordType) {
                    case "A":
                        result = "naaccr-" + naaccrVersion + "-abstract";
                        break;
                    case "M":
                        result = "naaccr-" + naaccrVersion + "-modified";
                        break;
                    case "C":
                        result = "naaccr-" + naaccrVersion + "-confidential";
                        break;
                    case "I":
                        result = "naaccr-" + naaccrVersion + "-incidence";
                        break;
                    default:
                        // ignored
                }
            }
        }
        catch (IOException e) {
            result = null;
        }

        if (result != null && !NaaccrFormat.isFormatSupported(result))
            result = null;

        return result;
    }

    /**
     * Returns the NAACCR format of the given XML file.
     * @param xmlFile provided data file
     * @return the NAACCR format, null if it cannot be determined
     */
    public static String getFormatFromXmlFile(File xmlFile) {
        if (xmlFile == null || !xmlFile.exists())
            return null;

        Pattern patternVersion = Pattern.compile("naaccrVersion=\"(.+?)\"");
        Pattern patternType = Pattern.compile("recordType=\"(.+?)\"");
        
        String version = null, type = null;
        try (BufferedReader reader = new BufferedReader(createReader(xmlFile))) {
            String line = reader.readLine();
            while (line != null && (version == null || type == null)) {
                Matcher matcherVersion = patternVersion.matcher(line);
                if (matcherVersion.find())
                    version = matcherVersion.group(1);
                Matcher matcherType = patternType.matcher(line);
                if (matcherType.find())
                    type = matcherType.group(1);
                line = reader.readLine();
            }
        }
        catch (IOException e) {
            // ignore, the result will be null
        }
        
        String result = null;
        if (version != null && type != null) {
            switch (type) {
                case "A":
                    result = "naaccr-" + version + "-abstract";
                    break;
                case "M":
                    result = "naaccr-" + version + "-modified";
                    break;
                case "C":
                    result = "naaccr-" + version + "-confidential";
                    break;
                case "I":
                    result = "naaccr-" + version + "-incidence";
                    break;
                default:
                    result = null;
            }
        }

        return NaaccrFormat.isFormatSupported(result) ? result : null;
    }

    public static NaaccrDictionary getStandardDictionary() {
        try {
            // TODO finalize standard dictionary format and location...
            URL standardDiciontaryUrl = Thread.currentThread().getContextClassLoader().getResource("naaccr-dictionary-140.csv");
            return NaaccrDictionaryUtils.readDictionary(standardDiciontaryUrl, NaaccrDictionaryUtils.NAACCR_DICTIONARY_FORMAT_CSV);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to get standard dictionary!", e);
        }
    }

    public static XStream getStandardXStream(RuntimeNaaccrDictionary dictionary, NaaccrXmlOptions options) {
        XStream xstream = new XStream();

        // tell XStream how to read/write our main entities
        xstream.alias(NAACCR_XML_TAG_ROOT, NaaccrDataExchange.class);
        xstream.alias(NAACCR_XML_TAG_PATIENT, Patient.class);
        xstream.alias(NAACCR_XML_TAG_TUMOR, Tumor.class);
        xstream.alias(NAACCR_XML_TAG_ITEM, Item.class);

        // all collections should be wrap into collection tags, but it's nicer to omit them in the XML; we have to tell XStream though
        xstream.addImplicitCollection(NaaccrDataExchange.class, "patients", Patient.class);
        xstream.addImplicitCollection(Patient.class, "items", Item.class);
        xstream.addImplicitCollection(Patient.class, "tumors", Tumor.class);
        xstream.addImplicitCollection(Tumor.class, "items", Item.class);

        // the item object is a bit harder to read/write, so we have to use a specific converter
        xstream.registerConverter(new NaaccrItemConverter(dictionary, options));

        return xstream;
    }

    // testing method, will be removed eventually...
    /**
    public static void main(String[] args) throws Exception {

        URL myDictionaryUrl = Thread.currentThread().getContextClassLoader().getResource("fabian/fab-dictionary.csv");
        NaaccrDictionary myDictionary = NaaccrDictionaryUtils.readDictionary(myDictionaryUrl, NaaccrDictionaryUtils.NAACCR_DICTIONARY_FORMAT_CSV);

        File inputFile = new File(System.getProperty("user.dir") + "/src/main/resources/data/fake-naaccr14inc-10000-rec.txt.gz");
        File outputFile = new File(System.getProperty("user.dir") + "/build/test.xml.gz");
        long start = System.currentTimeMillis();
        flatToXml(inputFile, outputFile, NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, null);
        System.out.println("Done translating flat-file to XML (10,000 records) using standard dictionary in " + (System.currentTimeMillis() - start) + "ms - see 'test.xml.gz'");

        File inputFile2 = new File(System.getProperty("user.dir") + "/build/test.xml.gz");
        File outputFile2 = new File(System.getProperty("user.dir") + "/build/test.txt.gz");
        long start2 = System.currentTimeMillis();
        xmlToFlat(inputFile2, outputFile2, NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, null);
        System.out.println("Done translation XML back to flat-file (10,000 records) using standard dictionary in " + (System.currentTimeMillis() - start2) + "ms - see 'test.txt.gz'");

        Patient patient = new Patient();
        patient.getItems().add(new Item("nameLast", "DEPRY"));
        patient.getItems().add(new Item("nameFirst", "FABIAN"));
        patient.getTumors().add(new Tumor());
        patient.getTumors().get(0).getItems().add(new Item("primarySite", "C619"));
        patient.getTumors().get(0).getItems().add(new Item("hosptialAbstractorId", "FDEPRY"));
        File outputFile3 = new File(System.getProperty("user.dir") + "/build/user-dictionary-test-names.xml");
        PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(outputFile3), NaaccrFormat.NAACCR_FORMAT_14_ABSTRACT, myDictionary);
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
        writer = new PatientXmlWriter(new FileWriter(outputFile3), NaaccrFormat.NAACCR_FORMAT_14_ABSTRACT, myDictionary);
        writer.writePatient(patient);
        writer.close();
        System.out.println("Done creating XML file using non-standard items (item referenced by number) - see 'user-dictionary-test-numbers.xml'");

        patient.getItems().add(new Item("recordType", "A"));
        patient.getItems().add(new Item("naaccrRecordVersion", "140"));
        outputFile3 = new File(System.getProperty("user.dir") + "/build/user-dictionary-test.txt");
        PatientFlatWriter writer3 = new PatientFlatWriter(new FileWriter(outputFile3), NaaccrFormat.NAACCR_FORMAT_14_ABSTRACT, myDictionary);
        writer3.writePatient(patient);
        writer3.close();
        System.out.println("Done creating flat-file using non-standard items in State Requestor Items - see 'user-dictionary-test.txt'");

        File inputFile4 = new File(System.getProperty("user.dir") + "/build/user-dictionary-test-numbers.xml");
        NaaccrDataExchange data = readXmlFile(inputFile4, NaaccrFormat.NAACCR_FORMAT_14_ABSTRACT, null);
        System.out.println("Done reading 'user-dictionary-test-numbers.xml', got " + data.getPatients().size() + " patient(s)...");

        File outputFile5 = new File(System.getProperty("user.dir") + "/build/another-xml-test.xml");
        writeXmlFile(data, outputFile5, NaaccrFormat.NAACCR_FORMAT_14_ABSTRACT, null);
        System.out.println("Done writing 'another-xml-test.xml'");
    }
     */
}
