/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

/**
 * This utillity class can be used to read/write dictionaries, whether they are internal to the library, or provided by the user...
 */
public final class NaaccrDictionaryUtils {

    // the different data types
    public static final String NAACCR_DATA_TYPE_ALPHA = "alpha"; // uppercase letters, A-Z, no spaces, full length needs to be filled in
    public static final String NAACCR_DATA_TYPE_DIGITS = "digits"; // digits, 0-9, no spaces, full length needs to be filled in
    public static final String NAACCR_DATA_TYPE_MIXED = "mixed"; // uppercase letters or digits, A-Z,0-9, no spaces, full length needs to be filled in
    public static final String NAACCR_DATA_TYPE_NUMERIC = "numeric"; // digits, 0-9 with optional period, no spaces but value can be smaller than the length
    public static final String NAACCR_DATA_TYPE_TEXT = "text"; // no checking on this value
    public static final String NAACCR_DATA_TYPE_DATE = "date"; // digits, YYYY or YYYYMM or YYYYMMDD

    // regular expression for each data type
    public static final Map<String, Pattern> NAACCR_DATA_TYPES_REGEX = new HashMap<>();

    static {
        NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_ALPHA, Pattern.compile("^[A-Z]+$"));
        NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_DIGITS, Pattern.compile("^[0-9]+$"));
        NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_MIXED, Pattern.compile("^[A-Z0-9]+$"));
        NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_NUMERIC, Pattern.compile("^([A-Za-z]|\\s)+$"));
        NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_TEXT, Pattern.compile("^.+$"));
        NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_DATE, Pattern.compile("^(18|19|20)[0-9][0-9]((0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])?)?$"));
    }

    // trimming rules
    public static final String NAACCR_TRIM_ALL = "all";
    public static final String NAACCR_TRIM_NONE = "none";

    // padding rules
    public static final String NAACCR_PADDING_RIGHT_BLANK = "rightBlank";
    public static final String NAACCR_PADDING_LEFT_BLANK = "leftBlank";
    public static final String NAACCR_PADDING_RIGHT_ZERO = "rightZero";
    public static final String NAACCR_PADDING_LEFT_ZERO = "leftZero";


    // the Patterns for the internal dictionaries URI
    public static final Pattern _PATTERN_DICTIONARY_URI = Pattern.compile("http://naaccr\\.org/naaccrxml/naaccr-dictionary(-gaps)?-(.+?)\\.xml");

    /**
     * Private constructor, no instanciation...
     */
    private NaaccrDictionaryUtils() {
    }

    /**
     * Extracts the NAACCR version from an internal dictionary URI.
     * @param uri internal dictionary URI
     * @return the corrsponding NAACCR version, null if it can't be extracted
     */
    public static String extractVersionFromUri(String uri) {
        if (uri == null)
            return null;
        Matcher matcher = _PATTERN_DICTIONARY_URI.matcher(uri);
        if (matcher.matches())
            return matcher.group(2);
        return null;
    }

    /**
     * Returns the internal dictionary URI for the given version.
     * @param version version, required
     * @param isBase if true, the base URI will be returned, otherwise the default user one
     * @return URI as a string
     */
    public static String createUriFromVersion(String version, boolean isBase) {
        if (isBase)
            return "http://naaccr.org/naaccrxml/naaccr-dictionary-" + version + ".xml";
        else
            return "http://naaccr.org/naaccrxml/naaccr-dictionary-gaps-" + version + ".xml";
    }
    
    /**
     * Returns the base dictionary for the requested URI.
     * @param uri URI, required
     * @return the corresponding base dictionary, throws a runtime exception if not found
     */
    public static NaaccrDictionary getBaseDictionaryByUri(String uri) {
        if (uri == null)
            throw new RuntimeException("URI is required for getting the base dictionary.");
        return getBaseDictionaryByVersion(extractVersionFromUri(uri));
    }

    /**
     * Returns the base dictionary for the requested NAACCR version.
     * @param naaccrVersion NAACCR version, required (see constants in NaaccrFormat)
     * @return the corresponding base dictionary, throws a runtime exception if not found
     */
    @SuppressWarnings("ConstantConditions")
    public static NaaccrDictionary getBaseDictionaryByVersion(String naaccrVersion) {
        if (naaccrVersion == null)
            throw new RuntimeException("Version is required for getting the base dictionary.");
        if (!NaaccrFormat.isVersionSupported(naaccrVersion))
            throw new RuntimeException("Unsupported base dictionary version: " + naaccrVersion);
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResource("naaccr-dictionary-" + naaccrVersion + ".xml").openStream())) {
            return readDictionary(reader);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to get base dictionary!", e);
        }
    }

    /**
     * Returns the default user dictionary for the requested URI.
     * @param uri URI, required
     * @return the corresponding default user dictionary, throws a runtime exception if not found
     */
    public static NaaccrDictionary getDefaultUserDictionaryByUri(String uri) {
        if (uri == null)
            throw new RuntimeException("URI is required for getting the default user dictionary.");
        return getDefaultUserDictionary(extractVersionFromUri(uri));
    }

    /**
     * Returns the default user dictionary for the requested NAACCR version.
     * @param naaccrVersion NAACCR version, required (see constants in NaaccrFormat)
     * @return the corresponding default user dictionary, throws a runtime exception if not found
     */
    @SuppressWarnings("ConstantConditions")
    public static NaaccrDictionary getDefaultUserDictionary(String naaccrVersion) {
        if (naaccrVersion == null)
            throw new RuntimeException("Version is required for getting the default user dictionary.");
        if (!NaaccrFormat.isVersionSupported(naaccrVersion))
            throw new RuntimeException("Unsupported default user dictionary version: " + naaccrVersion);
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResource("naaccr-dictionary-gaps-" + naaccrVersion + ".xml").openStream())) {
            return readDictionary(reader);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to get base dictionary!", e);
        }
    }

    /**
     * Reads a dictionary from the provided file.
     * @param file file, cannot be null
     * @return the corresonding dictionary
     * @throws IOException if the dictionary could not be read
     */
    public static NaaccrDictionary readDictionary(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return readDictionary(reader);
        }
    }
    
    /**
     * Reads a dictionary from the provided reader.
     * @param reader reader, cannot be null
     * @return the corresonding dictionary
     * @throws IOException if the dictionary could not be read
     */
    public static NaaccrDictionary readDictionary(Reader reader) throws IOException {
        return (NaaccrDictionary)instanciateXStream().fromXML(reader);
    }

    /**
     * Writes the given dictionary to the provided file.
     * @param dictionary dictionary to write, cannot be null
     * @param file file, cannot be null
     * @throws IOException if the dictionary could not be written
     */
    public static void writeDictionary(NaaccrDictionary dictionary, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writeDictionary(dictionary, writer);
        }
    }
    
    /**
     * Writes the given dictionary to the provided writer.
     * @param dictionary dictionary to write, cannot be null
     * @param writer writer, cannot be null
     * @throws IOException if the dictionary could not be written
     */
    public static void writeDictionary(NaaccrDictionary dictionary, Writer writer) throws IOException {
        // TODO do we really want this formatting? It's really not standard, and adds a lot of complexity to this class...
        instanciateXStream().marshal(dictionary, new NaaccrPrettyPrintWriter(dictionary, writer));
        //instanciateXStream().toXML(dictionary, writer);
    }

    private static XStream instanciateXStream() {
        XStream xstream = new XStream();

        xstream.alias("NaaccrDictionary", NaaccrDictionary.class);
        xstream.alias("ItemDef", NaaccrDictionaryItem.class);

        xstream.aliasAttribute(NaaccrDictionary.class, "_dictionaryUri", "dictionaryUri");
        xstream.aliasAttribute(NaaccrDictionary.class, "_naaccrVersion", "naaccrVersion");
        xstream.aliasAttribute(NaaccrDictionary.class, "_description", "description");
        xstream.aliasAttribute(NaaccrDictionary.class, "_items", "ItemDefs");

        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_naaccrId", "naaccrId");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_naaccrNum", "naaccrNum");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_naaccrName", "naaccrName");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_startColumn", "startColumn");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_length", "length");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_recordTypes", "recordTypes");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_sourceOfStandard", "sourceOfStandard");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_parentXmlElement", "parentXmlElement");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_dataType", "dataType");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_regexValidation", "regexValidation");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_padding", "padding");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "_trim", "trim");

        xstream.omitField(NaaccrDictionary.class, "_cachedById");
        xstream.omitField(NaaccrDictionary.class, "_cachedByNumber");

        return xstream;
    }

    private static class NaaccrPrettyPrintWriter extends PrettyPrintWriter {

        // why isn't the internal writer protected instead of private??? I hate when people do that!
        private QuickWriter _internalWriter;

        private NaaccrDictionary _dictionary;

        private String _currentItemId;

        public NaaccrPrettyPrintWriter(NaaccrDictionary dictionary, Writer writer) {
            super(writer, new char[] {' ', ' ', ' ', ' '});
            _dictionary = dictionary;
        }

        @Override
        protected void writeAttributeValue(QuickWriter writer, String text) {
            super.writeAttributeValue(writer, text);
            if (_internalWriter == null)
                _internalWriter = writer;
        }

        @Override
        public void startNode(String name) {
            super.startNode(name);
            _currentItemId = null;
        }

        @Override
        public void addAttribute(String key, String value) {
            if ("dataType".equals(key) && NAACCR_DATA_TYPE_TEXT.equals(value))
                return;
            if ("padding".equals(key) && NAACCR_PADDING_RIGHT_BLANK.equals(value))
                return;
            if ("trim".equals(key) && NAACCR_TRIM_ALL.equals(value))
                return;
            super.addAttribute(key, value);
            if ("naaccrId".equals(key))
                _currentItemId = value;
            if (!"description".equals(key) && !isLastAttribute(key))
                _internalWriter.write("\r\n           ");
        }

        private boolean isLastAttribute(String attribute) {
            NaaccrDictionaryItem item = _dictionary.getItemByNaaccrId(_currentItemId);
            if (item == null)
                return false;

            if (item.getTrim() != null && !NAACCR_TRIM_ALL.equals(item.getTrim()))
                return "trim".equals(attribute);
            if (item.getPadding() != null && !NAACCR_PADDING_RIGHT_BLANK.equals(item.getPadding()))
                return "padding".equals(attribute);
            if (item.getRegexValidation() != null)
                return "regexValidation".equals(attribute);
            if (item.getDataType() != null && !NAACCR_DATA_TYPE_TEXT.equals(item.getDataType()))
                return "dataType".equals(attribute);
            if (item.getParentXmlElement() != null)
                return "parentXmlElement".equals(attribute);
            return false;
        }
    }


    // TODO remove this testing method...
    public static void main(String[] args) throws IOException {
        NaaccrDictionary dict = getBaseDictionaryByVersion("140");
        System.out.println("Read " + dict.getItems().size() + " items from base dictionary...");
        System.out.println(dict.getItemByNaaccrId("vendorName").getTrim());
        //dict = getDefaultUserDictionary("140");
        //System.out.println("Read " + dict.getItems().size() + " items from default user dictionary...");
    }
}
