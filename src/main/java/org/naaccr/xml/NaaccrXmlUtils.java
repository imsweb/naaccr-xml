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

    // item attributes
    public static final String NAACCR_XML_ITEM_ATT_ID = "naaccrId";
    public static final String NAACCR_XML_ITEM_ATT_NUM = "naaccrNum";

    public static final String GENERATED_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"; // TODO verify the format...

    // item to use by default to group the tumors together
    public static final String DEFAULT_TUMOR_GROUPING_ITEM = "patientIdNumber";

    // items used to determine the format of a flat file line
    public static final String FLAT_FIILE_FORMAT_ITEM_REC_TYPE = "recordType";
    public static final String FLAT_FIILE_FORMAT_ITEM_NAACCR_VERSION = "naaccrRecordVersion";

    /**
     * Translates a flat data file into an XML data file.
     * @param flatFile source flat data file, must exists
     * @param xmlFile target XML data file, parent file must exists
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void flatToXml(File flatFile, File xmlFile, NaaccrXmlOptions options, NaaccrDictionary userDictionary, NaaccrStreamObserver observer) throws NaaccrIOException {
        if (flatFile == null)
            throw new NaaccrIOException("Source flat file is required");
        if (!flatFile.exists())
            throw new NaaccrIOException("Source flat file must exist");
        if (!xmlFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        // create the reader and writer and let them do all the work!
        try (PatientFlatReader reader = new PatientFlatReader(createReader(flatFile), options, userDictionary)) {
            try (PatientXmlWriter writer = new PatientXmlWriter(createWriter(xmlFile), reader.getRootData())) {
                Patient patient = reader.readPatient();
                while (patient != null) {
                    if (observer != null)
                        observer.patientRead(patient);
                    writer.writePatient(patient);
                    if (observer != null)
                        observer.patientWritten(patient);
                    patient = reader.readPatient();
                }
            }
        }
    }

    /**
     * Translates an XML data file into a flat data file.
     * @param xmlFile source XML data file, must exists
     * @param flatFile target flat data file, parent file must exists
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void xmlToFlat(File xmlFile, File flatFile, NaaccrXmlOptions options, NaaccrDictionary userDictionary, NaaccrStreamObserver observer) throws NaaccrIOException {
        if (xmlFile == null)
            throw new NaaccrIOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new NaaccrIOException("Source XML file must exist");
        if (!flatFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        // create the reader and writer and let them do all the work!
        try (PatientXmlReader reader = new PatientXmlReader(createReader(xmlFile), options, userDictionary)) {
            try (PatientFlatWriter writer = new PatientFlatWriter(createWriter(flatFile), reader.getRootData(), options, userDictionary)) {
                Patient patient = reader.readPatient();
                while (patient != null) {
                    if (observer != null)
                        observer.patientRead(patient);
                    writer.writePatient(patient);
                    if (observer != null)
                        observer.patientWritten(patient);
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
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static NaaccrData readXmlFile(File xmlFile, NaaccrXmlOptions options, NaaccrDictionary userDictionary, NaaccrStreamObserver observer) throws NaaccrIOException {
        if (xmlFile == null)
            throw new NaaccrIOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new NaaccrIOException("Source XML file must exist");

        try (PatientXmlReader reader = new PatientXmlReader(createReader(xmlFile), options, userDictionary)) {
            NaaccrData rootData = reader.getRootData();
            Patient patient = reader.readPatient();
            while (patient != null) {
                if (observer != null)
                    observer.patientRead(patient);
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
     * @param data a <code>NaaccrData</code> object, cannot be null
     * @param xmlFile target XML data file
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void writeXmlFile(NaaccrData data, File xmlFile, NaaccrXmlOptions options, NaaccrDictionary userDictionary, NaaccrStreamObserver observer) throws NaaccrIOException {
        if (data == null)
            throw new NaaccrIOException("Data is required");
        if (!xmlFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        try (PatientXmlWriter writer = new PatientXmlWriter(createWriter(xmlFile), data, options, userDictionary)) {
            for (Patient patient : data.getPatients()) {
                writer.writePatient(patient);
                if (observer != null)
                    observer.patientWritten(patient);
            }
        }
    }

    /**
     * Reads an NAACCR flat file data file and returns the corresponding data.
     * <br/>
     * ATTENTION: THIS METHOD WILL RETURN THE FULL CONTENT OF THE FILE AND IS NOT SUITABLE FOR LARGE FILE; CONSIDER USING A STREAM INSTEAD.
     * @param xmlFile source XML data file, must exists
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static NaaccrData readFlatFile(File flatFile, NaaccrXmlOptions options, NaaccrDictionary userDictionary, NaaccrStreamObserver observer) throws NaaccrIOException {
        if (flatFile == null)
            throw new NaaccrIOException("Source flat file is required");
        if (!flatFile.exists())
            throw new NaaccrIOException("Source flat file must exist");

        try (PatientFlatReader reader = new PatientFlatReader(createReader(flatFile), options, userDictionary)) {
            NaaccrData data = reader.getRootData();
            Patient patient = reader.readPatient();
            while (patient != null) {
                if (observer != null)
                    observer.patientRead(patient);
                data.getPatients().add(patient);
                patient = reader.readPatient();
            }
            return data;
        }
    }

    /**
     * Writes the provided data to the requested flat file.
     * <br/>
     * ATTENTION: THIS METHOD REQUIRES THE ENTIRE DATA OBJECT TO BE IN MEMORY; CONSIDER USING A STREAM INSTEAD.
     * @param data a <code>NaaccrData</code> object, cannot be null
     * @param flatFile target flat data file
     * @param format expected NAACCR format
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void writeFlatFile(NaaccrData data, File flatFile, NaaccrXmlOptions options, NaaccrDictionary userDictionary, NaaccrStreamObserver observer) throws NaaccrIOException {
        if (data == null)
            throw new NaaccrIOException("Data is required");
        if (!flatFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        try (PatientFlatWriter writer = new PatientFlatWriter(createWriter(flatFile), data, options, userDictionary)) {
            for (Patient patient : data.getPatients()) {
                writer.writePatient(patient);
                if (observer != null)
                    observer.patientWritten(patient);
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

        try (BufferedReader reader = new BufferedReader(createReader(flatFile))) {
            return getFormatFromFlatFileLine(reader.readLine());
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
    public static String getFormatFromFlatFileLine(String line) {
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
        catch (NaaccrIOException e) {
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
    private static Reader createReader(File file) throws NaaccrIOException {
        try {
            InputStream is = new FileInputStream(file);

            if (file.getName().endsWith(".gz"))
                is = new GZIPInputStream(is);

            return new InputStreamReader(is, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }
    }

    // takes care of the file encoding and compression...
    private static Writer createWriter(File file) throws NaaccrIOException {
        try {
            OutputStream os = new FileOutputStream(file);

            if (file.getName().endsWith(".gz"))
                os = new GZIPOutputStream(os);

            return new OutputStreamWriter(os, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }
    }
}
