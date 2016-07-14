/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

/**
 * This utility class can be used to read/write dictionaries, whether they are internal to the library, or provided by the user...
 * <br/><br/>
 * Note that there is no method to get all the supported dictionaries; instead one needs to use the getSupportedVersions() available in
 * the NaaccrFormat class, and from there use the getBaseDictionaryByVersion() in this class.
 * <br/><br/>
 * There is no caching done in this class; dictionaries are loaded from XML as requested.
 */
public final class NaaccrXmlDictionaryUtils {

    // the different data types
    public static final String NAACCR_DATA_TYPE_ALPHA = "alpha"; // uppercase letters, A-Z, no spaces, full length needs to be filled in
    public static final String NAACCR_DATA_TYPE_DIGITS = "digits"; // digits, 0-9, no spaces, full length needs to be filled in
    public static final String NAACCR_DATA_TYPE_MIXED = "mixed"; // uppercase letters or digits, A-Z,0-9, no spaces, full length needs to be filled in
    public static final String NAACCR_DATA_TYPE_NUMERIC = "numeric"; // digits, 0-9 with optional period, no spaces but value can be smaller than the length
    public static final String NAACCR_DATA_TYPE_TEXT = "text"; // no checking on this value
    public static final String NAACCR_DATA_TYPE_DATE = "date"; // digits, YYYY or YYYYMM or YYYYMMDD

    // regular expression for each data type
    private static final Map<String, Pattern> _NAACCR_DATA_TYPES_REGEX = new HashMap<>();

    static {
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_ALPHA, Pattern.compile("^[A-Z]+$"));
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_DIGITS, Pattern.compile("^[0-9]+$"));
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_MIXED, Pattern.compile("^[A-Z0-9]+$"));
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_NUMERIC, Pattern.compile("^[0-9]+(\\.[0-9]+)?$"));
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_TEXT, Pattern.compile("^.+$"));
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_DATE, Pattern.compile("^(18|19|20)[0-9][0-9]((0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])?)?$"));
    }

    // trimming rules (default is all)
    public static final String NAACCR_TRIM_ALL = "all";
    public static final String NAACCR_TRIM_NONE = "none";

    // padding rules (default is rightBlank)
    public static final String NAACCR_PADDING_RIGHT_BLANK = "rightBlank";
    public static final String NAACCR_PADDING_LEFT_BLANK = "leftBlank";
    public static final String NAACCR_PADDING_RIGHT_ZERO = "rightZero";
    public static final String NAACCR_PADDING_LEFT_ZERO = "leftZero";

    // the Patterns for the internal dictionaries URI
    public static final Pattern _PATTERN_DICTIONARY_URI = Pattern.compile("http://naaccr\\.org/naaccrxml/(user-defined-)?naaccr-dictionary-(.+?)\\.xml");

    /**
     * Private constructor, no instanciation...
     */
    private NaaccrXmlDictionaryUtils() {
    }

    /**
     * Returns the pattern for the provided data type, null if not found.
     * @param dataType requested data type
     * @return corresponding pattern, maybe null
     */
    public static Pattern getDataTypePattern(String dataType) {
        return _NAACCR_DATA_TYPES_REGEX.get(dataType);
    }

    /**
     * Returns whether values for a given data type need to have the same length as their definition
     * @param type given data type
     * @return true if the values of that type needs to be fully filled-in
     */
    public static boolean isFullLengthRequiredForType(String type) {
        boolean result = NAACCR_DATA_TYPE_ALPHA.equals(type);
        result |= NAACCR_DATA_TYPE_DIGITS.equals(type);
        result |= NAACCR_DATA_TYPE_MIXED.equals(type);
        return result;
    }

    /**
     * Extracts the NAACCR version from an internal dictionary URI.
     * @param uri internal dictionary URI
     * @return the corresponding NAACCR version, null if it can't be extracted
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
            return "http://naaccr.org/naaccrxml/user-defined-naaccr-dictionary-" + version + ".xml";
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
        String resName = "naaccr-dictionary-" + naaccrVersion + ".xml";
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResource(resName).openStream(), StandardCharsets.UTF_8)) {
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
        return getDefaultUserDictionaryByVersion(extractVersionFromUri(uri));
    }

    /**
     * Returns the default user dictionary for the requested NAACCR version.
     * @param naaccrVersion NAACCR version, required (see constants in NaaccrFormat)
     * @return the corresponding default user dictionary, throws a runtime exception if not found
     */
    @SuppressWarnings("ConstantConditions")
    public static NaaccrDictionary getDefaultUserDictionaryByVersion(String naaccrVersion) {
        if (naaccrVersion == null)
            throw new RuntimeException("Version is required for getting the default user dictionary.");
        if (!NaaccrFormat.isVersionSupported(naaccrVersion))
            throw new RuntimeException("Unsupported default user dictionary version: " + naaccrVersion);
        String resName = "user-defined-naaccr-dictionary-" + naaccrVersion + ".xml";
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResource(resName).openStream(), StandardCharsets.UTF_8)) {
            return readDictionary(reader);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to get base dictionary!", e);
        }
    }

    /**
     * Reads a dictionary from the provided file.
     * @param file file, cannot be null
     * @return the corresponding dictionary
     * @throws IOException if the dictionary could not be read
     */
    public static NaaccrDictionary readDictionary(File file) throws IOException {
        if (file == null)
            throw new IOException("File is required to load dictionary.");
        if (!file.exists())
            throw new IOException("File must exist to load dictionary.");
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return readDictionary(reader);
        }
    }

    /**
     * Reads a dictionary from the provided reader.
     * @param reader reader, cannot be null
     * @return the corresponding dictionary
     * @throws IOException if the dictionary could not be read
     */
    public static NaaccrDictionary readDictionary(Reader reader) throws IOException {
        NaaccrDictionary dictionary = (NaaccrDictionary)instanciateXStream().fromXML(reader);
        
        if (dictionary.getSpecificationVersion() == null)
            dictionary.setSpecificationVersion(SpecificationVersion.SPEC_1_0);

        String error;
        if (dictionary.getDictionaryUri() != null && _PATTERN_DICTIONARY_URI.matcher(dictionary.getDictionaryUri()).matches())
            error = validateBaseDictionary(dictionary);
        else
            error = validateUserDictionary(dictionary);

        if (error != null)
            throw new IOException(error);

        return dictionary;
    }

    /**
     * Writes the given dictionary to the provided file.
     * @param dictionary dictionary to write, cannot be null
     * @param file file, cannot be null
     * @throws IOException if the dictionary could not be written
     */
    public static void writeDictionary(NaaccrDictionary dictionary, File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
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
        instanciateXStream().marshal(dictionary, new NaaccrPrettyPrintWriter(dictionary, writer));
    }

    /**
     * Validates the provided base dictionary.
     * @param dictionary dictionary to validate, can't be null
     * @return null if the dictionary is valid, the error message otherwise
     */
    public static String validateBaseDictionary(NaaccrDictionary dictionary) {
        return validateDictionary(dictionary, true);
    }

    /**
     * Validates the provided user dictionary.
     * @param dictionary dictionary to validate, can't be null
     * @return null if the dictionary is valid, the error message otherwise
     */
    public static String validateUserDictionary(NaaccrDictionary dictionary) {
        return validateDictionary(dictionary, false);
    }

    /**
     * Validates the provided dictionary
     * @param dictionary dictionary to validate, can't be null
     * @param isBaseDictionary true if the dictionary is a base dictionary, false otherwise
     * @return null if the dictionary is valid, the error message otherwise
     */
    private static String validateDictionary(NaaccrDictionary dictionary, boolean isBaseDictionary) {

        if (dictionary.getDictionaryUri() == null || dictionary.getDictionaryUri().trim().isEmpty())
            return "'dictionaryUri' attribute is required";
        if (dictionary.getNaaccrVersion() == null || dictionary.getNaaccrVersion().trim().isEmpty())
            return "'naaccrVersion' attribute is required";
        if (dictionary.getItems().isEmpty())
            return "a dictionary must contain at least one item definition";

        Pattern idPattern = Pattern.compile("^[a-z][a-zA-Z0-9]+$");
        for (NaaccrDictionaryItem item : dictionary.getItems()) {
            if (item.getNaaccrId() == null || item.getNaaccrId().trim().isEmpty())
                return "'naaccrId' attribute is required";
            if (!idPattern.matcher(item.getNaaccrId()).matches())
                return "'naaccrId' attribute has a bad format (needs to start with a lower case letter, followed by letters and digits): " + item.getNaaccrId();
            if (item.getNaaccrNum() == null)
                return "'naaccrNum' attribute is required";
            if (item.getLength() == null)
                return "'length' attribute is required";
            if (item.getStartColumn() == null)
                return "'startColumn' attribute is required";
            if (item.getParentXmlElement() == null || item.getParentXmlElement().trim().isEmpty())
                return "'parentXmlElement' attribute is required";
            if (!NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(item.getParentXmlElement()) && !NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(item.getParentXmlElement())
                    && !NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(item.getParentXmlElement()))
                return "invalid value for 'parentXmlElement' attribute: " + item.getParentXmlElement();
            if (item.getDataType() != null && (!NAACCR_DATA_TYPE_ALPHA.equals(item.getDataType()) && !NAACCR_DATA_TYPE_DIGITS.equals(item.getDataType()) && !NAACCR_DATA_TYPE_MIXED.equals(
                    item.getDataType())) && !NAACCR_DATA_TYPE_NUMERIC.equals(item.getDataType()) && !NAACCR_DATA_TYPE_TEXT.equals(item.getDataType()) && !NAACCR_DATA_TYPE_DATE.equals(
                    item.getDataType()))
                return "invalid value for 'dataType' attribute: " + item.getDataType();
            if (item.getPadding() != null && (!NAACCR_PADDING_LEFT_BLANK.equals(item.getPadding()) && !NAACCR_PADDING_LEFT_ZERO.equals(item.getPadding()) && !NAACCR_PADDING_RIGHT_BLANK.equals(
                    item.getPadding()) && !NAACCR_PADDING_RIGHT_ZERO.equals(item.getPadding())))
                return "invalid value for 'padding' attribute: " + item.getPadding();
            if (item.getTrim() != null && (!NAACCR_TRIM_ALL.equals(item.getTrim()) && !NAACCR_TRIM_NONE.equals(item.getTrim())))
                return "invalid value for 'trim' attribute: " + item.getTrim();
            if (item.getRegexValidation() != null) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Pattern.compile(item.getRegexValidation());
                }
                catch (PatternSyntaxException e) {
                    return "invalid value for 'regexValidation' attribute: " + item.getRegexValidation();
                }
            }
        }

        // user dictionary specific validation
        if (!isBaseDictionary) {

            // we are going to need these...
            NaaccrDictionary baseDictionary = getBaseDictionaryByVersion(dictionary.getNaaccrVersion());
            NaaccrDictionary defaultUserDictionary = getDefaultUserDictionaryByVersion(dictionary.getNaaccrVersion());

            for (NaaccrDictionaryItem item : dictionary.getItems()) {

                // can't use an internal base ID, ever
                if (baseDictionary.getItemByNaaccrId(item.getNaaccrId()) != null)
                    return "invalid value for 'naaccrId' attribute: " + item.getNaaccrId() + "; this ID is used in the standard dictionary";

                // if an internal default user dictionary ID is used, then there are a bunch of attributes it can't re-defined.
                NaaccrDictionaryItem defaultUserItem = defaultUserDictionary.getItemByNaaccrId(item.getNaaccrId());
                if (defaultUserItem != null) {
                    if (!Objects.equals(defaultUserItem.getNaaccrNum(), item.getNaaccrNum()))
                        return "invalid value for 'naaccrNum' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getNaaccrNum();
                    if (!Objects.equals(defaultUserItem.getNaaccrName(), item.getNaaccrName()))
                        return "invalid value for 'naaccrName' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getNaaccrName();
                    if (!Objects.equals(defaultUserItem.getStartColumn(), item.getStartColumn()))
                        return "invalid value for 'startColumn' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getStartColumn();
                    if (!Objects.equals(defaultUserItem.getLength(), item.getLength()))
                        return "invalid value for 'length' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getLength();
                    if (!Objects.equals(defaultUserItem.getRecordTypes(), item.getRecordTypes()))
                        return "invalid value for 'recordTypes' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getRecordTypes();
                    if (!Objects.equals(defaultUserItem.getParentXmlElement(), item.getParentXmlElement()))
                        return "invalid value for 'parentXmlElement' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getParentXmlElement();
                    // I really hate that the defaults are not loaded right away in the Java bean; I think that was a mistake!
                    String defaultUserItemType = defaultUserItem.getDataType() == null ? NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT : defaultUserItem.getDataType();
                    String itemType = item.getDataType() == null ? NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT : item.getDataType();
                    if (!Objects.equals(defaultUserItemType, itemType))
                        return "invalid value for 'dataType' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItemType;
                }
                else {
                    // range must be very specific for a user dictionary...
                    if (item.getNaaccrNum() < 9500 || item.getNaaccrNum() > 99999)
                        return "invalid value for 'naaccrNum' attribute: " + item.getNaaccrNum() + "; allowed range is 9500-99999";

                    // this is tricky, but an item must fall into the columns of one of the items defined in the corresponding items defined in the default user dictionary
                    boolean fallInAllowedRange = false;
                    for (NaaccrDictionaryItem defaultItem : defaultUserDictionary.getItems()) {
                        if (item.getStartColumn() >= defaultItem.getStartColumn() && item.getStartColumn() + item.getLength() <= defaultItem.getStartColumn() + defaultItem.getLength()) {
                            fallInAllowedRange = true;
                            break;
                        }
                    }
                    if (!fallInAllowedRange)
                        return "invalid value for 'startColumn' and/or 'length' attributes; user-defined items can only override state requestor item, NPCR item, or reserved gaps";
                }
            }
        }

        return null;
    }

    /**
     * Utility method to create a NAACCR ID from a display name:
     * <ol>
     * <li>Spaces, dashes, slashes periods and underscores are considered as word separators and replaced by a single space</li>
     * <li>Anything in parenthesis is removed (along with the parenthesis)</li>
     * <li>Any non-digit and non-letter character is removed</li>
     * <li>The result is split by spaces</li>
     * <li>The first part is un-capitalized, the other parts are capitalized</li>
     * <li>All the parts are concatenated together</li>
     * </ol>
     * @param name display name
     * @return NAACCR ID (which can be used as a property name)
     */
    public static String createNaaccrIdFromItemName(String name) {
        if (name == null || name.isEmpty())
            return "";

        String[] parts = StringUtils.split(name.replaceAll("\\s+|-+|/|_|\\.", " ").replaceAll("\\(.+\\)|[\\W&&[^\\s]]", ""), ' ');

        StringBuilder buf = new StringBuilder();
        buf.append(StringUtils.uncapitalize(parts[0].toLowerCase()));
        for (int i = 1; i < parts.length; i++)
            buf.append(StringUtils.capitalize(parts[i].toLowerCase()));

        return buf.toString();
    }

    /**
     * Helper method to create the XStream object used to read and write XML.
     * @return a configured <code>XStream</code> object
     */
    private static XStream instanciateXStream() {
        XStream xstream = new XStream();

        xstream.alias("NaaccrDictionary", NaaccrDictionary.class);
        xstream.alias("ItemDef", NaaccrDictionaryItem.class);

        xstream.aliasAttribute(NaaccrDictionary.class, "_dictionaryUri", "dictionaryUri");
        xstream.aliasAttribute(NaaccrDictionary.class, "_naaccrVersion", "naaccrVersion");
        xstream.aliasAttribute(NaaccrDictionary.class, "_specificationVersion", "specificationVersion");
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

    /**
     * This class is used to properly format dictionaries when they are written (mainly the indentation and the line feeds).
     */
    private static class NaaccrPrettyPrintWriter extends PrettyPrintWriter {

        // why isn't the internal writer protected instead of private??? I hate when people do that!
        private QuickWriter _internalWriter;

        private NaaccrDictionary _dictionary;

        private String _currentItemId;

        public NaaccrPrettyPrintWriter(NaaccrDictionary dictionary, Writer writer) {
            super(writer, new char[] {' ', ' ', ' ', ' '});
            _dictionary = dictionary;
            try {
                writer.write("<?xml version=\"1.0\"?>\r\n\r\n");
            }
            catch (IOException e) {
                // ignore this one, the exception will happen again anyway...
            }
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
            if (!isLastAttribute(key))
                _internalWriter.write("\r\n           ");
            // this isn't going to work well for user-define dictionaries bc desc is optional...
            if ("description".equals(key))
                super.addAttribute("xmlns", NaaccrXmlUtils.NAACCR_XML_NAMESPACE);
        }

        /**
         * This complex logic is needed because we wanted a non-standard formatting for the XML (in terms of indentations)...
         * @param attribute attribute to consider
         * @return true if the provided attribute is the last one on the line, false otherwise.
         */
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
            return item.getParentXmlElement() != null && "parentXmlElement".equals(attribute);
        }
    }
}
