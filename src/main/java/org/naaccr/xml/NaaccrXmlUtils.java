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
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;

// TODO use a properties file with the exceptions so they can be shared with the DLL?
// TODO investigate using abstract base reader/writer that would be parametrized classes...
// TODO post something on XStream to expose the Path from the PartTracker
public class NaaccrXmlUtils {

    // structure tags in the XML
    public static final String NAACCR_XML_TAG_ROOT = "NaaccrData";
    public static final String NAACCR_XML_TAG_PATIENT = "Patient";
    public static final String NAACCR_XML_TAG_TUMOR = "Tumor";
    public static final String NAACCR_XML_TAG_ITEM = "Item";

    // root attributes
    public static final String NAACCR_XML_ROOT_ATT_BASE_DICT = "baseDictionaryUri";
    public static final String NAACCR_XML_ROOT_ATT_USER_DICT = "userDictionaryUri";
    public static final String NAACCR_XML_ROOT_ATT_REC_TYPE = "recordType";
    public static final String NAACCR_XML_ROOT_ATT_TIME_GENERATED = "timeGenerated";
    
    public static final String GENERATED_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"; // TODO verify the format...

    /**
     * Translates a flat data file into an XML data file.
     * @param flatFile source flat data file, must exists
     * @param xmlFile target XML data file, parent file must exists
     * @param format expected NAACCR format, required (but you can use the getFormatFromFlatFile() method to evaluate it from the file)
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @throws IOException if there is problem reading/writing the file
     * @throws NaaccrValidationException if there is a problem validating the data
     */
    public static void flatToXml(File flatFile, File xmlFile, String format, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws IOException, NaaccrValidationException {
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

        // create the reader and writer and let them do all the work!
        try (PatientXmlWriter writer = new PatientXmlWriter(createWriter(xmlFile), format)) {
            try (PatientFlatReader reader = new PatientFlatReader(createReader(flatFile), options, userDictionary)) {
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
     * @param flatFile target flat data file, parent file must exists
     * @param format expected NAACCR format, required (but you can use the getFormatFromXmlFile() method to evaluate it from the file)
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @throws IOException if there is problem reading/writing the file
     * @throws NaaccrValidationException if there is a problem validating the data
     */
    public static void xmlToFlat(File xmlFile, File flatFile, String format, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws IOException, NaaccrValidationException {
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

        // create the reader and writer and let them do all the work!
        try (PatientFlatWriter writer = new PatientFlatWriter(createWriter(flatFile), format, options, userDictionary)) {
            try (PatientXmlReader reader = new PatientXmlReader(createReader(xmlFile), options, userDictionary)) {
                Patient patient = reader.readPatient();
                while (patient != null) {
                    writer.writePatient(patient);
                    patient = reader.readPatient();
                }
            }
        }
    }

    /**
     * Reads an NAACCR XML data file and returns the corresponding data.
     * <br/>
     * ATTENTION: THIS METHOD WILL RETURN THE FULL CONTENT OF THE FILE AND IS NOT SUITABLE FOR LARGE FILE; CONSIDER USING A STREAM INSTEAD.
     * @param xmlFile source XML data file, must exists
     * @param format expected NAACCR format
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @throws IOException if there is problem reading/writing the file
     * @throws NaaccrValidationException if there is a problem validating the data
     */
    public static NaaccrData readXmlFile(File xmlFile, String format, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws IOException, NaaccrValidationException {
        if (xmlFile == null)
            throw new IOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new IOException("Source XML file must exist");
        if (format == null)
            throw new IOException("File format is required");
        if (!NaaccrFormat.isFormatSupported(format))
            throw new IOException("Invalid file format");
        
        try (PatientXmlReader reader = new PatientXmlReader(createReader(xmlFile), options, userDictionary)) {
            NaaccrData rootData = reader.getRootData();
            Patient patient = reader.readPatient();
            while (patient != null) {
                rootData.getPatients().add(patient);
                patient = reader.readPatient();
            }
            return rootData;
        }
    }

    /**
     * Writes the provided data to the requested XML file.
     * <br/>
     * ATTENTION: THIS METHOD REQUIRES THE ENTIRE DATA OBJECT TO BE IN MEMORY; CONSIDER USING A STREAM INSTEAD.
     * @param data a <code>NaaccrDataExchange</code> object, cannot be null
     * @param xmlFile target XML data file
     * @param format expected NAACCR format
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @throws IOException if there is problem reading/writing the file
     * @throws NaaccrValidationException if there is a problem validating the data
     */
    public static void writeXmlFile(NaaccrData data, File xmlFile, String format, NaaccrXmlOptions options, NaaccrDictionary nonStandardDictionary) throws IOException {
        if (data == null)
            throw new IOException("Data is required");
        if (format == null)
            throw new IOException("File format is required");
        if (!xmlFile.getParentFile().exists())
            throw new IOException("Target folder must exist");
        if (!NaaccrFormat.isFormatSupported(format))
            throw new IOException("Invalid file format");

        try (PatientXmlWriter writer = new PatientXmlWriter(createWriter(xmlFile), data)) {
            for (Patient patient : data.getPatients())
                writer.writePatient(patient);
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

        try (BufferedReader reader = new BufferedReader(createReader(flatFile))) {
            return getFormatFromFlatFile(reader.readLine());
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the NAACCR format of the given line in a flat file.
     * @param flatFile provided data line
     * @return the NAACCR format, null if it cannot be determined
     */
    public static String getFormatFromFlatFile(String line) {
        if (line == null || line.length() < 19)
            return null;
        
        String version = line.substring(16, 19).trim();
        String type = line.substring(0, 1).trim();

        if (NaaccrFormat.isVersionSupported(version) && NaaccrFormat.isRecordTypeSupported(type))
            return NaaccrFormat.getInstance(version, type).toString();

        return null;
    }

    /**
     * Returns the NAACCR format of the given XML file.
     * @param xmlFile provided data file
     * @return the NAACCR format, null if it cannot be determined
     */
    public static String getFormatFromXmlFile(File xmlFile) {
        if (xmlFile == null || !xmlFile.exists())
            return null;

        try {
            return getFormatFromXmlReader(createReader(xmlFile));
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the NAACCR format of the given XML reader.
     * @param xmlReader provided reader
     * @return the NAACCR format, null if it cannot be determined
     */
    public static String getFormatFromXmlReader(Reader xmlReader) {

        Pattern patternDictionary = Pattern.compile("baseDictionaryUri=\"(.+?)\"");
        Pattern patternType = Pattern.compile("recordType=\"(.+?)\"");

        String version = null, type = null;
        try (BufferedReader reader = new BufferedReader(xmlReader)) {
            // this isn't going to work if the file is big and doesn't contain any new lines, which is technically allowed in XML...
            String line = reader.readLine();
            while (line != null && (version == null || type == null)) {
                Matcher matcherDictionary = patternDictionary.matcher(line);
                if (matcherDictionary.find())
                    version = NaaccrDictionaryUtils.extractVersionFromUri(matcherDictionary.group(1));
                Matcher matcherType = patternType.matcher(line);
                if (matcherType.find())
                    type = matcherType.group(1);
                line = reader.readLine();
            }
        }
        catch (IOException e) {
            // ignore, the result will be null
        }

        if (version != null && NaaccrFormat.isVersionSupported(version) && type != null && NaaccrFormat.isRecordTypeSupported(type))
            return NaaccrFormat.getInstance(version, type).toString();
        
        return null;
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
