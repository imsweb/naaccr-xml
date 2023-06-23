/*
 * Copyright (C) 2014 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;

/**
 * This utility class provides static methods for reading, writing and translating to/from XML and flat file NAACCR files.
 */
public final class NaaccrXmlUtils {

    // specification version that this library implements (will be used when writing XML files)
    public static final String CURRENT_SPECIFICATION_VERSION = SpecificationVersion.SPEC_1_7;

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
    public static final String NAACCR_XML_ROOT_ATT_SPEC_VERSION = "specificationVersion";

    // item attributes
    public static final String NAACCR_XML_ITEM_ATT_ID = "naaccrId";
    public static final String NAACCR_XML_ITEM_ATT_NUM = "naaccrNum";

    // item to use by default to group the tumors together (item #20)
    public static final String DEFAULT_TUMOR_GROUPING_ITEM = "patientIdNumber";

    // items used to determine the format of a flat file line (items #10 and #50)
    public static final String FLAT_FILE_FORMAT_ITEM_REC_TYPE = "recordType";
    public static final String FLAT_FILE_FORMAT_ITEM_NAACCR_VERSION = "naaccrRecordVersion";

    // target namespace
    public static final String NAACCR_XML_NAMESPACE = "http://naaccr.org/naaccrxml";

    /**
     * Constructor.
     */
    private NaaccrXmlUtils() {
        // utility class...
    }

    /**
     * Writes all the patients from the source file into the target file after applying a given "processing" to each of them.
     * @param xmlSource source XML data file, must exists
     * @param xmlTarget target XML data file, parent file must exists
     * @param processor defines the logic to apply to each patient, cannot be null
     * @param options optional validating options
     * @param userDictionaries optional user-defined dictionaries (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void xmlToXml(File xmlSource, File xmlTarget, NaaccrPatientProcessor processor, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrObserver observer) throws NaaccrIOException {
        if (xmlSource == null)
            throw new NaaccrIOException("Source XML file is required");
        if (!xmlSource.exists())
            throw new NaaccrIOException("Source XML file must exist");
        if (!xmlTarget.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");
        if (processor == null)
            throw new NaaccrIOException("A processor must be provided");

        // create the reader and writer and let them do all the work!
        try (PatientXmlReader reader = new PatientXmlReader(createReader(xmlSource), options, userDictionaries)) {
            try (PatientXmlWriter writer = new PatientXmlWriter(createWriter(xmlTarget), reader.getRootData(), options, userDictionaries)) {
                Patient patient = reader.readPatient();
                while (patient != null && !Thread.currentThread().isInterrupted()) {
                    if (observer != null)
                        observer.patientRead(patient);
                    processor.processPatient(patient);
                    writer.writePatient(patient);
                    if (observer != null)
                        observer.patientWritten(patient);
                    patient = reader.readPatient();
                }
            }
        }
    }

    /**
     * Translates a flat data file into an XML data file.
     * @param flatFile source flat data file, must exists
     * @param xmlFile target XML data file, parent file must exists
     * @param options optional validating options
     * @param userDictionaries optional user-defined dictionaries (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void flatToXml(File flatFile, File xmlFile, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrObserver observer) throws NaaccrIOException {
        if (flatFile == null)
            throw new NaaccrIOException("Source flat file is required");
        if (!flatFile.exists())
            throw new NaaccrIOException("Source flat file must exist");
        if (!xmlFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        // create the reader and writer and let them do all the work!
        try (PatientFlatReader reader = new PatientFlatReader(createReader(flatFile), options, userDictionaries)) {
            try (PatientXmlWriter writer = new PatientXmlWriter(createWriter(xmlFile), reader.getRootData(), options, userDictionaries)) {
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
     * @param userDictionaries optional user-defined dictionaries (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void xmlToFlat(File xmlFile, File flatFile, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrObserver observer) throws NaaccrIOException {
        if (xmlFile == null)
            throw new NaaccrIOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new NaaccrIOException("Source XML file must exist");
        if (!flatFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        // create the reader and writer and let them do all the work!
        try (PatientXmlReader reader = new PatientXmlReader(createReader(xmlFile), options, userDictionaries)) {
            try (PatientFlatWriter writer = new PatientFlatWriter(createWriter(flatFile), reader.getRootData(), options, userDictionaries)) {
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
     * @param userDictionaries optional user-defined dictionaries (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static NaaccrData readXmlFile(File xmlFile, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrObserver observer) throws NaaccrIOException {
        if (xmlFile == null)
            throw new NaaccrIOException("Source XML file is required");
        if (!xmlFile.exists())
            throw new NaaccrIOException("Source XML file must exist");

        try (PatientXmlReader reader = new PatientXmlReader(createReader(xmlFile), options, userDictionaries)) {
            NaaccrData rootData = reader.getRootData();
            Patient patient = reader.readPatient();
            while (patient != null && !Thread.currentThread().isInterrupted()) {
                if (observer != null)
                    observer.patientRead(patient);
                rootData.addPatient(patient);
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
     * @param userDictionaries optional user-defined dictionaries (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void writeXmlFile(NaaccrData data, File xmlFile, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrObserver observer) throws NaaccrIOException {
        if (data == null)
            throw new NaaccrIOException("Data is required");
        if (!xmlFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        try (PatientXmlWriter writer = new PatientXmlWriter(createWriter(xmlFile), data, options, userDictionaries)) {
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
     * @param userDictionaries optional user-defined dictionaries (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static NaaccrData readFlatFile(File flatFile, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrObserver observer) throws NaaccrIOException {
        if (flatFile == null)
            throw new NaaccrIOException("Source flat file is required");
        if (!flatFile.exists())
            throw new NaaccrIOException("Source flat file must exist");

        try (PatientFlatReader reader = new PatientFlatReader(createReader(flatFile), options, userDictionaries)) {
            NaaccrData data = reader.getRootData();
            Patient patient = reader.readPatient();
            while (patient != null && !Thread.currentThread().isInterrupted()) {
                if (observer != null)
                    observer.patientRead(patient);
                data.addPatient(patient);
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
     * @param userDictionaries optional user-defined dictionaries (will be merged with the base dictionary)
     * @param observer an optional observer, useful to keep track of the progress
     * @throws NaaccrIOException if there is problem reading/writing the file
     */
    public static void writeFlatFile(NaaccrData data, File flatFile, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrObserver observer) throws NaaccrIOException {
        if (data == null)
            throw new NaaccrIOException("Data is required");
        if (!flatFile.getParentFile().exists())
            throw new NaaccrIOException("Target folder must exist");

        try (PatientFlatWriter writer = new PatientFlatWriter(createWriter(flatFile), data, options, userDictionaries)) {
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
     * Translates a single line representing a flat file line into a patient object. The resulting patient will have 0 or 1 tumor.
     * <br/><br/>
     * Unlike the methods dealing with files, this method takes a context as a parameter. The reason for that difference is that this method uses a stream to convert
     * the line, and so the stream needs to be re-created every time the method is invoked on a given line. This is very inefficient and would be too slow if this
     * method was used in a loop (which is the common use-case). Having a shared context that is created once outside the loop avoids that inefficiency.
     * <br/><br/>
     * It is very important to not re-create the context when this method is called in a loop:
     * <br><br/>
     * This is NOT correct:
     * <code>
     * for (String line : lines)
     * NaaccrXmlUtils.lineToPatient(line, new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT));
     * </code>
     * This is correct:
     * <code>
     * NaaccrContext context = new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
     * for (String line : lines)
     * NaaccrXmlUtils.lineToPatient(line, context);
     * </code>
     * @param line the line to translate, required
     * @param context the context to use for the translation, required
     * @return the corresponding patient, never null
     * @throws NaaccrIOException if there is problem translating the line
     */
    public static Patient lineToPatient(String line, NaaccrContext context) throws NaaccrIOException {
        if (line == null)
            throw new NaaccrIOException("Line is required");
        if (context == null)
            throw new NaaccrIOException("Context is required");

        NaaccrFormat format = NaaccrFormat.getInstance(context.getFormat());
        if (line.length() != format.getLineLength())
            throw new NaaccrIOException("Expected line length to be " + format.getLineLength() + " but was " + line.length());

        boolean updateType = !format.getRecordType().equals(line.substring(0, 1).trim());
        boolean updateVersion = !format.getNaaccrVersion().equals(line.substring(16, 19).trim());
        if (updateType || updateVersion) {
            StringBuilder buf = new StringBuilder(line);
            buf.replace(0, 1, format.getRecordType());
            buf.replace(16, 19, format.getNaaccrVersion());
            line = buf.toString();
        }

        try (PatientFlatReader reader = new PatientFlatReader(new StringReader(line), context.getOptions(), context.getUserDictionaries(), context.getStreamConfiguration())) {
            return reader.readPatient();
        }
    }

    /**
     * Translates a single patient into a line representing a flat file line. This method expects a patient with 0 or 1 tumor. An exception will be raised if it has more.
     * <br/><br/>
     * Unlike the methods dealing with files, this method takes a context as a parameter. The reason for that difference is that this method uses a stream to convert
     * the patient, and so the stream needs to be re-created every time the method is invoked on a given patient. This is very inefficient and would be too slow if this
     * method was used in a loop (which is the common use-case). Having a shared context that is created once outside the loop avoids that inefficiency.
     * <br/><br/>
     * It is very important to not re-create the context when this method is called in a loop:
     * <br><br/>
     * This is NOT correct:
     * <code>
     * for (Patient patient : patients)
     * NaaccrXmlUtils.patientToLine(patient, new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT));
     * </code>
     * This is correct:
     * <code>
     * NaaccrContext context = new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
     * for (Patient patient : patients)
     * NaaccrXmlUtils.patientToLine(patient, context);
     * </code>
     * @param patient the patient to translate, required
     * @param context the context to use for the translation, required
     * @return the corresponding line, never null
     * @throws NaaccrIOException if there is problem translating the patient
     */
    public static String patientToLine(Patient patient, NaaccrContext context) throws NaaccrIOException {
        if (patient == null)
            throw new NaaccrIOException("Patient is required");
        if (context == null)
            throw new NaaccrIOException("Context is required");

        // it wouldn't be very hard to support more than one tumor, but will do it only if needed
        if (patient.getTumors().size() > 1)
            throw new NaaccrIOException("This method requires a patient with 0 or 1 tumor.");

        StringWriter buf = new StringWriter();
        try (PatientFlatWriter writer = new PatientFlatWriter(buf, new NaaccrData(context.getFormat()), context.getOptions(), context.getUserDictionaries(), context.getStreamConfiguration())) {
            writer.writePatient(patient);
            return buf.toString();
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

        try (Reader reader = createReader(xmlFile)) {
            return getFormatFromXmlReader(reader);
        }
        catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Returns the NAACCR format of the given XML reader.
     * @param xmlReader provided reader, cannot be null
     * @return the NAACCR format, null if it cannot be determined
     */
    public static String getFormatFromXmlReader(Reader xmlReader) {
        Map<String, String> attributes = getAttributesFromXmlReader(xmlReader);
        String baseDictUri = attributes.get(NAACCR_XML_ROOT_ATT_BASE_DICT);
        String recordType = attributes.get(NAACCR_XML_ROOT_ATT_REC_TYPE);
        if (baseDictUri != null && recordType != null) {
            String version = NaaccrXmlDictionaryUtils.extractVersionFromUri(baseDictUri);
            if (NaaccrFormat.isVersionSupported(version) && NaaccrFormat.isRecordTypeSupported(recordType))
                return NaaccrFormat.getInstance(version, recordType).toString();
        }

        return null;
    }

    /**
     * Returns all the available attributes from the given XML file.
     * @param xmlFile provided data file
     * @return the available attributes in a map, maybe empty but never null
     */
    public static Map<String, String> getAttributesFromXmlFile(File xmlFile) {
        if (xmlFile == null || !xmlFile.exists())
            return Collections.emptyMap();

        try (Reader reader = createReader(xmlFile)) {
            return getAttributesFromXmlReader(reader);
        }
        catch (IOException | RuntimeException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Returns all the available attributes from the given XML reader.
     * <br/><br/>
     * If peeking is supported by the provided reader, this method won't consume the reader.
     * <br/><br/>
     * The reader is not closed after calling this method.
     * @param xmlReader provided reader, cannot be null
     * @return the available attributes in a map, maybe empty but never null
     */
    public static Map<String, String> getAttributesFromXmlReader(Reader xmlReader) {
        Map<String, String> result = new HashMap<>();
        if (xmlReader == null)
            return result;

        try {
            if (xmlReader.markSupported())
                xmlReader.mark(8192); // this is the default buffer size for a BufferedReader; should be more than enough to get all available attributes...
            HierarchicalStreamReader xstreamReader = new NaaccrStreamConfiguration().getDriver().createReader(xmlReader);
            if (removeNameSpacePrefix(xstreamReader.getNodeName()).equals(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT))
                for (int i = 0; i < xstreamReader.getAttributeCount(); i++)
                    result.put(removeNameSpacePrefix(xstreamReader.getAttributeName(i)), xstreamReader.getAttribute(i));
            if (xmlReader.markSupported())
                xmlReader.reset();
        }
        catch (IOException | RuntimeException e) {
            // ignored, result will be empty
        }

        return result;
    }

    // helper
    private static String removeNameSpacePrefix(String tag) {
        int idx = tag.indexOf(':');
        if (idx != -1)
            return tag.substring(idx + 1);
        return tag;
    }

    /**
     * Returns a generic reader for the provided file, taking care of the optional GZ compression.
     * @param file file to create the reader from, cannot be null
     * @return a generic reader to the file, never null
     * @throws NaaccrIOException if the reader cannot be created
     */
    public static Reader createReader(File file) throws NaaccrIOException {
        InputStream is = null;
        try {
            is = new FileInputStream(file);

            if (file.getName().endsWith(".gz"))
                is = new GZIPInputStream(is);

            return new InputStreamReader(is, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e1) {
                    // give up
                }
            }
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
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);

            if (file.getName().endsWith(".gz"))
                os = new GZIPOutputStream(os);

            return new OutputStreamWriter(os, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            if (os != null) {
                try {
                    os.close();
                }
                catch (IOException e1) {
                    // give up
                }
            }
            throw new NaaccrIOException(e.getMessage());
        }
    }

    /**
     * Reads the provided ISO 8601 value into a Date object.
     * @param value value to parse
     * @return the parsed value as a Date, null if the incoming value is null or empty
     * @throws IOException if the value has a value that cannot be parsed
     */
    public static Date parseIso8601Date(String value) throws IOException {
        if (StringUtils.isBlank(value))
            return null;

        // looks like ISO 8601 allows with and without a time zone (offset) and Java doesn't have a single formatter to handle both (sucks!)
        try {
            return Date.from(ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
        }
        catch (RuntimeException e1) {
            try {
                return Date.from(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC));
            }
            catch (RuntimeException e2) {
                throw new IOException("Unable to parse value as ISO 8601 date: " + value);
            }
        }
    }

    /**
     * Format the provided date into an ISO 8601 String value.
     * @param date the date to format, cannot be null
     * @return the formatted date using the ISO 8601 format
     */
    public static String formatIso8601Date(Date date) {
        if (date == null)
            throw new NullPointerException("Date to write cannot be null!");

        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
