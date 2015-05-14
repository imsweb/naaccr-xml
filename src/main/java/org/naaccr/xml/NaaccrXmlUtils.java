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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

/**
 * This utility class provides static methods for reading, writing and translating to/from XML and flat file NAACCR files.
 */
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

    // item to use by default to group the tumors together (item #20)
    public static final String DEFAULT_TUMOR_GROUPING_ITEM = "patientIdNumber";

    // items used to determine the format of a flat file line (items #10 and #50)
    public static final String FLAT_FILE_FORMAT_ITEM_REC_TYPE = "recordType";
    public static final String FLAT_FILE_FORMAT_ITEM_NAACCR_VERSION = "naaccrRecordVersion";

    /**
     * Translates a flat data file into an XML data file.
     * @param flatFile source flat data file, must exists
     * @param xmlFile target XML data file, parent file must exists
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void flatToXml(File flatFile, File xmlFile, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrObserver observer) throws NaaccrIOException {
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
                while (patient != null && !Thread.currentThread().isInterrupted()) {
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
    public static void xmlToFlat(File xmlFile, File flatFile, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrObserver observer) throws NaaccrIOException {
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
                while (patient != null && !Thread.currentThread().isInterrupted()) {
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
    public static NaaccrData readXmlFile(File xmlFile, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrObserver observer) throws NaaccrIOException {
        if (xmlFile == null)
            throw new NaaccrIOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new NaaccrIOException("Source XML file must exist");

        try (PatientXmlReader reader = new PatientXmlReader(createReader(xmlFile), options, userDictionary)) {
            NaaccrData rootData = reader.getRootData();
            Patient patient = reader.readPatient();
            while (patient != null && !Thread.currentThread().isInterrupted()) {
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
    public static void writeXmlFile(NaaccrData data, File xmlFile, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrObserver observer) throws NaaccrIOException {
        if (data == null)
            throw new NaaccrIOException("Data is required");
        if (!xmlFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        try (PatientXmlWriter writer = new PatientXmlWriter(createWriter(xmlFile), data, options, userDictionary)) {
            for (Patient patient : data.getPatients()) {
                writer.writePatient(patient);
                if (observer != null)
                    observer.patientWritten(patient);
                if (Thread.currentThread().isInterrupted())
                    break;
            }
        }
    }

    /**
     * Reads an NAACCR flat file data file and returns the corresponding data.
     * <br/>
     * ATTENTION: THIS METHOD WILL RETURN THE FULL CONTENT OF THE FILE AND IS NOT SUITABLE FOR LARGE FILE; CONSIDER USING A STREAM INSTEAD.
     * @param flatFile source flat file, must exists
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static NaaccrData readFlatFile(File flatFile, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrObserver observer) throws NaaccrIOException {
        if (flatFile == null)
            throw new NaaccrIOException("Source flat file is required");
        if (!flatFile.exists())
            throw new NaaccrIOException("Source flat file must exist");

        try (PatientFlatReader reader = new PatientFlatReader(createReader(flatFile), options, userDictionary)) {
            NaaccrData data = reader.getRootData();
            Patient patient = reader.readPatient();
            while (patient != null && !Thread.currentThread().isInterrupted()) {
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
     * @param options optional validating options
     * @param userDictionary an optional user-defined dictionary (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void writeFlatFile(NaaccrData data, File flatFile, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrObserver observer) throws NaaccrIOException {
        if (data == null)
            throw new NaaccrIOException("Data is required");
        if (!flatFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        try (PatientFlatWriter writer = new PatientFlatWriter(createWriter(flatFile), data, options, userDictionary)) {
            for (Patient patient : data.getPatients()) {
                writer.writePatient(patient);
                if (observer != null)
                    observer.patientWritten(patient);
                if (Thread.currentThread().isInterrupted())
                    break;
            }
        }
    }

    /**
     * Returns the NAACCR format of the given flat file, based on it's first data line.
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
     * <br/>
     * This method assumes that the record type is available in column 1 (length 1) and 
     * the NAACCR version is available in column 17 (length 3)...
     * @param line provided data line
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
     * @param xmlReader provided reader, cannot be null
     * @return the NAACCR format, null if it cannot be determined
     */
    public static String getFormatFromXmlReader(Reader xmlReader) {

        try (PatientXmlReader reader = new PatientXmlReader(xmlReader, null, null)) {
            NaaccrData rootData = reader.getRootData();
            if (rootData.getBaseDictionaryUri() != null && rootData.getRecordType() != null) {
                String version = NaaccrXmlDictionaryUtils.extractVersionFromUri(rootData.getBaseDictionaryUri());
                if (NaaccrFormat.isVersionSupported(version) && NaaccrFormat.isRecordTypeSupported(rootData.getRecordType()))
                    return NaaccrFormat.getInstance(version, rootData.getRecordType()).toString();
            }
        }
        catch (NaaccrIOException e) {
            // ignored, the result will be null, which is appropriate...
        }

        return null;
    }

    /**
     * Returns a generic reader for the provided file, taking care of the optional GZ compression.
     * @param file file to create the reader from, cannot be null
     * @return a generic reader to the file, never null
     * @throws NaaccrIOException if the reader cannot be created
     */
    public static Reader createReader(File file) throws NaaccrIOException {
        try {
            InputStream is = new FileInputStream(file);

            if (file.getName().endsWith(".gz"))
                is = new GZIPInputStream(is);
            else if (file.getName().endsWith(".xz"))
                is = new XZInputStream(is);

            return new InputStreamReader(is, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }
    }

    /**
     * Returns a generic writer for the provided file, taking care of the optional GZ compression.
     * @param file file to create the writer from, cannot be null
     * @return a generic writer to the file, never null
     * @throws NaaccrIOException if the writer cannot be created
     */
    public static Writer createWriter(File file) throws NaaccrIOException {
        try {
            OutputStream os = new FileOutputStream(file);

            if (file.getName().endsWith(".gz"))
                os = new GZIPOutputStream(os);
            else if (file.getName().endsWith(".xz"))
                os = new XZOutputStream(os, new LZMA2Options(3));

            return new OutputStreamWriter(os, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }
    }
}
