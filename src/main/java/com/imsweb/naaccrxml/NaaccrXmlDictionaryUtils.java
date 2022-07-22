/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.WildcardTypePermission;

import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryGroupedItem;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;
import com.imsweb.naaccrxml.runtime.NaaccrDictionaryConverter;

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
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_DIGITS, Pattern.compile("^\\d+$"));
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_MIXED, Pattern.compile("^[A-Z\\d]+$"));
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_NUMERIC, Pattern.compile("^\\d+(\\.\\d+)?$"));
        _NAACCR_DATA_TYPES_REGEX.put(NAACCR_DATA_TYPE_DATE, Pattern.compile("^(18|19|20)\\d\\d((0[1-9]|1[012])(0[1-9]|[12]\\d|3[01])?)?$"));
    }

    // trimming rules (default is all)
    public static final String NAACCR_TRIM_ALL = "all";
    public static final String NAACCR_TRIM_NONE = "none";

    // padding rules (default is rightBlank)
    public static final String NAACCR_PADDING_RIGHT_BLANK = "rightBlank";
    public static final String NAACCR_PADDING_LEFT_BLANK = "leftBlank";
    public static final String NAACCR_PADDING_RIGHT_ZERO = "rightZero";
    public static final String NAACCR_PADDING_LEFT_ZERO = "leftZero";
    public static final String NAACCR_PADDING_NONE = "none";

    // the Patterns for the internal dictionaries URI
    public static final Pattern BASE_DICTIONARY_URI_PATTERN = Pattern.compile("http://naaccr\\.org/naaccrxml/naaccr-dictionary-(.+?)\\.xml");
    public static final Pattern DEFAULT_USER_DICTIONARY_URI_PATTERN = Pattern.compile("http://naaccr\\.org/naaccrxml/user-defined-naaccr-dictionary-(.+?)\\.xml");

    // cached internal dictionaries
    private static final Map<String, NaaccrDictionary> _INTERNAL_DICTIONARIES = new ConcurrentHashMap<>();

    // NAACCR 18 IDs that got renamed as part of specifications 1.4 (those should be removed eventually)
    private static final Map<String, String> _RENAMED_LONG_NAACCR_18_IDS = new HashMap<>();

    static {
        _RENAMED_LONG_NAACCR_18_IDS.put("dateRegionalLymphNodeDissection", "dateRegionalLNDissection");
        _RENAMED_LONG_NAACCR_18_IDS.put("dateRegionalLymphNodeDissectionFlag", "dateRegionalLNDissectionFlag");
        _RENAMED_LONG_NAACCR_18_IDS.put("phase1RadiationExternalBeamPlanningTech", "phase1RadiationExternalBeamTech");
        _RENAMED_LONG_NAACCR_18_IDS.put("phase1RadiationPrimaryTreatmentVolume", "phase1RadiationPrimaryTxVolume");
        _RENAMED_LONG_NAACCR_18_IDS.put("phase1RadiationToDrainingLymphNodes", "phase1RadiationToDrainingLN");
        _RENAMED_LONG_NAACCR_18_IDS.put("phase2RadiationExternalBeamPlanningTech", "phase2RadiationExternalBeamTech");
        _RENAMED_LONG_NAACCR_18_IDS.put("phase2RadiationPrimaryTreatmentVolume", "phase2RadiationPrimaryTxVolume");
        _RENAMED_LONG_NAACCR_18_IDS.put("phase2RadiationToDrainingLymphNodes", "phase2RadiationToDrainingLN");
        _RENAMED_LONG_NAACCR_18_IDS.put("phase3RadiationExternalBeamPlanningTech", "phase3RadiationExternalBeamTech");
        _RENAMED_LONG_NAACCR_18_IDS.put("phase3RadiationPrimaryTreatmentVolume", "phase3RadiationPrimaryTxVolume");
        _RENAMED_LONG_NAACCR_18_IDS.put("phase3RadiationToDrainingLymphNodes", "phase3RadiationToDrainingLN");
        _RENAMED_LONG_NAACCR_18_IDS.put("radiationTreatmentDiscontinuedEarly", "radiationTxDiscontinuedEarly");
        _RENAMED_LONG_NAACCR_18_IDS.put("numberOfPhasesOfRadTreatmentToThisVolume", "numberPhasesOfRadTxToVolume");
        _RENAMED_LONG_NAACCR_18_IDS.put("npcrDerivedAjcc8TnmPostTherapyStgGrp", "npcrDerivedAjcc8TnmPostStgGrp");
        _RENAMED_LONG_NAACCR_18_IDS.put("chromosome1pLossOfHeterozygosity", "chromosome1pLossHeterozygosity");
        _RENAMED_LONG_NAACCR_18_IDS.put("chromosome19qLossOfHeterozygosity", "chromosome19qLossHeterozygosity");
        _RENAMED_LONG_NAACCR_18_IDS.put("bilirubinPretreatmentTotalLabValue", "bilirubinPretxTotalLabValue");
        _RENAMED_LONG_NAACCR_18_IDS.put("bilirubinPretreatmentUnitOfMeasure", "bilirubinPretxUnitOfMeasure");
        _RENAMED_LONG_NAACCR_18_IDS.put("creatininePretreatmentUnitOfMeasure", "creatininePretxUnitOfMeasure");
        _RENAMED_LONG_NAACCR_18_IDS.put("estrogenReceptorPercentPositiveOrRange", "estrogenReceptorPercntPosOrRange");
        _RENAMED_LONG_NAACCR_18_IDS.put("extranodalExtensionHeadAndNeckClinical", "extranodalExtensionHeadNeckClin");
        _RENAMED_LONG_NAACCR_18_IDS.put("extranodalExtensionHeadAndNeckPathological", "extranodalExtensionHeadNeckPath");
        _RENAMED_LONG_NAACCR_18_IDS.put("gestationalTrophoblasticPrognosticScoringIndex", "gestationalTrophoblasticPxIndex");
        _RENAMED_LONG_NAACCR_18_IDS.put("internationalNormalizedRatioForProthrombinTime", "iNRProthrombinTime");
        _RENAMED_LONG_NAACCR_18_IDS.put("ipsilateralAdrenalGlandInvolvement", "ipsilateralAdrenalGlandInvolve");
        _RENAMED_LONG_NAACCR_18_IDS.put("lnAssessmentMethodFemoralInguinal", "lnAssessMethodFemoralInguinal");
        _RENAMED_LONG_NAACCR_18_IDS.put("lnAssessmentMethodParaAortic", "lnAssessMethodParaaortic");
        _RENAMED_LONG_NAACCR_18_IDS.put("lnAssessmentMethodPelvic", "lnAssessMethodPelvic");
        _RENAMED_LONG_NAACCR_18_IDS.put("lnDistantAssessmentMethod", "lnDistantAssessMethod");
        _RENAMED_LONG_NAACCR_18_IDS.put("lnStatusFemoralInguinalParaAorticPelvic", "lnStatusFemorInguinParaaortPelv");
        _RENAMED_LONG_NAACCR_18_IDS.put("methylationOfO6MethylguanineMethyltransferase", "methylationOfO6MGMT");
        _RENAMED_LONG_NAACCR_18_IDS.put("oncotypeDxRecurrenceScoreInvasive", "oncotypeDxRecurrenceScoreInvasiv");
        _RENAMED_LONG_NAACCR_18_IDS.put("progesteroneReceptorPercentPositiveOrRange", "progesteroneRecepPrcntPosOrRange");
        _RENAMED_LONG_NAACCR_18_IDS.put("progesteroneReceptorSummary", "progesteroneRecepSummary");
        _RENAMED_LONG_NAACCR_18_IDS.put("progesteroneReceptorTotalAllredScore", "progesteroneRecepTotalAllredScor");
        _RENAMED_LONG_NAACCR_18_IDS.put("residualTumorVolumePostCytoreduction", "residualTumVolPostCytoreduction");
        _RENAMED_LONG_NAACCR_18_IDS.put("serumBeta2MicroglobulinPretreatmentLevel", "serumBeta2MicroglobulinPretxLvl");
        _RENAMED_LONG_NAACCR_18_IDS.put("visceralAndParietalPleuralInvasion", "visceralParietalPleuralInvasion");
        _RENAMED_LONG_NAACCR_18_IDS.put("dateOfSentinelLymphNodeBiopsy", "dateSentinelLymphNodeBiopsy");
    }

    // a couple of items got moved from Patient to Tumor level in N18 and N21
    private static final List<String> _PAT_TO_TUM_CHANGED_18_21_IDS = Arrays.asList("dateOfLastCancerStatus", "dateOfLastCancerStatusFlag");

    /**
     * Private constructor, no instanciation...
     */
    private NaaccrXmlDictionaryUtils() {
    }

    /**
     * Returns the N18 IDs that got renamed as part of the new v1.4 specifications, keys are the old (long) IDs, values are the new (short) IDs.
     */
    public static Map<String, String> getRenamedLongNaaccr18Ids() {
        return Collections.unmodifiableMap(_RENAMED_LONG_NAACCR_18_IDS);
    }

    /**
     * Returns the N18 and N21 items that got changed from Patient level to Tumor level.
     */
    public static List<String> getPatToTumorChangedNaaccr18And21Ids() {
        return Collections.unmodifiableList(_PAT_TO_TUM_CHANGED_18_21_IDS);
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
        Matcher matcher = BASE_DICTIONARY_URI_PATTERN.matcher(uri);
        if (matcher.matches())
            return matcher.group(1);
        else {
            matcher = DEFAULT_USER_DICTIONARY_URI_PATTERN.matcher(uri);
            if (matcher.matches())
                return matcher.group(1);
        }
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
            throw new IllegalStateException("URI is required for getting the base dictionary.");
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
            throw new IllegalStateException("Version is required for getting the base dictionary.");
        if (!NaaccrFormat.isVersionSupported(naaccrVersion))
            throw new IllegalStateException("Unsupported base dictionary version: " + naaccrVersion);
        NaaccrDictionary result = _INTERNAL_DICTIONARIES.get("base_" + naaccrVersion);
        if (result == null) {
            String resName = "naaccr-dictionary-" + naaccrVersion + ".xml";
            try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResource(resName).openStream(), StandardCharsets.UTF_8)) {
                result = readDictionary(reader);
                _INTERNAL_DICTIONARIES.put("base_" + naaccrVersion, result);
            }
            catch (IOException e) {
                throw new IllegalStateException("Unable to load base dictionary for version " + naaccrVersion, e);
            }
        }
        return result;
    }

    /**
     * Returns the default user dictionary for the requested URI.
     * @param uri URI, required
     * @return the corresponding default user dictionary, throws a runtime exception if not found
     */
    public static NaaccrDictionary getDefaultUserDictionaryByUri(String uri) {
        if (uri == null)
            throw new IllegalStateException("URI is required for getting the default user dictionary.");
        return getDefaultUserDictionaryByVersion(extractVersionFromUri(uri));
    }

    /**
     * Returns the default user dictionary for the requested NAACCR version, null if that version doesn't support a default user dictionary.
     * @param naaccrVersion NAACCR version, required (see constants in NaaccrFormat)
     * @return the corresponding default user dictionary, throws a runtime exception if not found
     */
    @SuppressWarnings("ConstantConditions")
    public static NaaccrDictionary getDefaultUserDictionaryByVersion(String naaccrVersion) {
        if (naaccrVersion == null)
            throw new IllegalStateException("Version is required for getting the default user dictionary.");
        if (!NaaccrFormat.isVersionSupported(naaccrVersion))
            throw new IllegalStateException("Unsupported default user dictionary version: " + naaccrVersion);

        // no more default user dictionary for version 22 and later!
        if (Integer.parseInt(naaccrVersion) >= 220)
            return null;

        NaaccrDictionary result = _INTERNAL_DICTIONARIES.get("user_" + naaccrVersion);
        if (result == null) {
            String resName = "user-defined-naaccr-dictionary-" + naaccrVersion + ".xml";
            try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResource(resName).openStream(), StandardCharsets.UTF_8)) {
                result = readDictionary(reader);
                _INTERNAL_DICTIONARIES.put("user_" + naaccrVersion, result);
            }
            catch (IOException e) {
                throw new IllegalStateException("Unable to get base dictionary for version " + naaccrVersion, e);
            }
        }
        return result;
    }

    /**
     * Returns true if the provided dictionary is a base dictionary, false otherwise (this is based on its URI).
     * @param dictionary dictionary, cannot be null
     * @return true if the dictionary is a base one, false otherwise
     */
    public static boolean isBaseDictionary(NaaccrDictionary dictionary) {
        return BASE_DICTIONARY_URI_PATTERN.matcher(dictionary.getDictionaryUri()).matches();
    }

    /**
     * Returns true if the provided dictionary is a default user dictionary, false otherwise (this is based on its URI).
     * @param dictionary dictionary, cannot be null
     * @return true if the dictionary is a default user one, false otherwise
     */
    public static boolean isDefaultUserDictionary(NaaccrDictionary dictionary) {
        String version = dictionary.getNaaccrVersion();
        return DEFAULT_USER_DICTIONARY_URI_PATTERN.matcher(dictionary.getDictionaryUri()).matches() && (version == null || version.compareTo(NaaccrFormat.NAACCR_VERSION_210) <= 0);
    }

    /**
     * Clears the cached internal dictionaries.
     */
    public static void clearCachedDictionaries() {
        _INTERNAL_DICTIONARIES.clear();
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
        try {
            NaaccrDictionary dictionary = (NaaccrDictionary)instanciateXStream().fromXML(reader);

            // default value for specifications
            if (dictionary.getSpecificationVersion() == null)
                dictionary.setSpecificationVersion(SpecificationVersion.SPEC_1_0);

            // apply default values
            if (dictionary.getItems() != null) {
                for (NaaccrDictionaryItem item : dictionary.getItems()) {
                    // record types (defaults to all types)
                    if (StringUtils.isBlank(item.getRecordTypes()))
                        item.setRecordTypes(NaaccrFormat.ALL_RECORD_TYPES);
                    // data type (defaults to text)
                    if (StringUtils.isBlank(item.getDataType()))
                        item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
                    // padding (defaults to right-blank)
                    if (StringUtils.isBlank(item.getPadding()))
                        item.setPadding(NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_BLANK);
                    // trimming (defaults to all)
                    if (StringUtils.isBlank(item.getTrim()))
                        item.setTrim(NaaccrXmlDictionaryUtils.NAACCR_TRIM_ALL);
                }
            }

            // let's not validate the internal dictionaries, we know they are valid
            String uri = dictionary.getDictionaryUri();
            if (uri == null || uri.trim().isEmpty())
                throw new IOException("'dictionaryUri' attribute is required");
            else if (!BASE_DICTIONARY_URI_PATTERN.matcher(uri).matches() && !DEFAULT_USER_DICTIONARY_URI_PATTERN.matcher(uri).matches()) {
                List<String> errors = validateUserDictionary(dictionary);
                if (!errors.isEmpty())
                    throw new IOException(errors.get(0));
            }

            return dictionary;
        }
        catch (XStreamException ex) {
            throw new IOException("Unable to read dictionary", ex);
        }
    }

    /**
     * Writes the given dictionary to the provided file.
     * @param dictionary dictionary to write, cannot be null
     * @param file file, cannot be null
     * @throws IOException if the dictionary could not be written
     */
    public static void writeDictionary(NaaccrDictionary dictionary, File file) throws IOException {
        writeDictionary(dictionary, file, null);
    }

    /**
     * Writes the given dictionary to the provided file.
     * @param dictionary dictionary to write, cannot be null
     * @param file file, cannot be null
     * @param comment an optional comment; if provided, every line of that comment will be included in a comment block on the top of the written file
     * @throws IOException if the dictionary could not be written
     */
    public static void writeDictionary(NaaccrDictionary dictionary, File file, List<String> comment) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writeDictionary(dictionary, writer, comment);
        }
    }

    /**
     * Writes the given dictionary to the provided writer.
     * @param dictionary dictionary to write, cannot be null
     * @param writer writer, cannot be null
     * @throws IOException if the dictionary could not be written
     */
    public static void writeDictionary(NaaccrDictionary dictionary, Writer writer) throws IOException {
        writeDictionary(dictionary, writer, null);
    }

    /**
     * Writes the given dictionary to the provided writer.
     * @param dictionary dictionary to write, cannot be null
     * @param writer writer, cannot be null
     * @param comment an optional comment; if provided, every line of that comment will be included in a comment block on the top of the written file
     * @throws IOException if the dictionary could not be written
     */
    public static void writeDictionary(NaaccrDictionary dictionary, Writer writer, List<String> comment) throws IOException {
        if (dictionary == null)
            throw new IllegalStateException("Provided dictionary cannot be null");

        // get the changelog if we are writing a base dictionary
        if (isBaseDictionary(dictionary)) {
            if (comment != null)
                throw new IllegalStateException("Comment cannot be provided when writing a base dictionary!");
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("changelog/changelog-naaccr-dictionary-" + dictionary.getNaaccrVersion() + ".txt")) {
                if (is != null) {
                    comment = new ArrayList<>();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line = reader.readLine();
                        while (line != null) {
                            comment.add(line);
                            line = reader.readLine();
                        }
                    }
                }
            }
        }
        else if (!isDefaultUserDictionary(dictionary)) {
            // update the date last modified
            dictionary.setDateLastModified(new Date());
        }

        // write the given dictionary
        try {
            instanciateXStream().marshal(dictionary, new NaaccrPrettyPrintWriter(dictionary, writer, comment));
        }
        catch (XStreamException ex) {
            throw new IOException("Unable to write dictionary", ex);
        }
    }

    /**
     * Validates the provided base dictionary.
     * @param dictionary dictionary to validate, can't be null
     * @return list of errors, empty if valid
     */
    public static List<String> validateBaseDictionary(NaaccrDictionary dictionary) {
        return validateDictionary(dictionary, true, null);
    }

    /**
     * Validates the provided user dictionary.
     * <br/><br/>
     * If the dictionary doesn't contain a NAACCR version, any validation that needs a specific version will be skipped.
     * @param dictionary dictionary to validate, can't be null
     * @return list of errors, empty if valid
     */
    public static List<String> validateUserDictionary(NaaccrDictionary dictionary) {
        return validateDictionary(dictionary, false, null);
    }

    /**
     * Validates the provided user dictionary.
     * @param dictionary dictionary to validate, can't be null
     * @param naaccrVersion naaccrVersion to assume if it's not provided on the dictionary (can be null)
     * @return list of errors, empty if valid
     */
    public static List<String> validateUserDictionary(NaaccrDictionary dictionary, String naaccrVersion) {
        return validateDictionary(dictionary, false, naaccrVersion);
    }

    /**
     * Validates the provided dictionary
     * @param dictionary dictionary to validate, can't be null
     * @param isBaseDictionary true if the dictionary is a base dictionary, false otherwise
     * @return list of errors, empty if valid
     */
    private static List<String> validateDictionary(NaaccrDictionary dictionary, boolean isBaseDictionary, String naaccrVersion) {
        List<String> errors = new ArrayList<>();

        // some of the validation is based on the specification version; assume 1.0 if it's not available (it's required as of 1.5)
        String specVersion = dictionary.getSpecificationVersion() == null ? SpecificationVersion.SPEC_1_0 : dictionary.getSpecificationVersion();
        if (!SpecificationVersion.isSpecificationSupported(specVersion))
            errors.add("'specificationVersion' attribute is not supported: " + specVersion);

        if (dictionary.getDictionaryUri() == null || dictionary.getDictionaryUri().trim().isEmpty())
            errors.add("'dictionaryUri' attribute is required");

        boolean allowBlankNaaccrVersion = !isBaseDictionary && SpecificationVersion.compareSpecifications(specVersion, SpecificationVersion.SPEC_1_1) >= 0;
        if (!allowBlankNaaccrVersion && (dictionary.getNaaccrVersion() == null || dictionary.getNaaccrVersion().trim().isEmpty()))
            errors.add("'naaccrVersion' attribute is required");
        if (dictionary.getNaaccrVersion() != null && !NaaccrFormat.isVersionSupported(dictionary.getNaaccrVersion()))
            errors.add("'naaccrVersion' attribute is not valid: " + dictionary.getNaaccrVersion());

        // some of the validation is based on the version (which can be null for user-defined dictionaries)
        String naaccrVersionToUse = dictionary.getNaaccrVersion() == null ? naaccrVersion : dictionary.getNaaccrVersion();
        boolean versionSupportsStartColumns = !NumberUtils.isDigits(naaccrVersionToUse) || Integer.parseInt(naaccrVersionToUse) <= 180;

        if (dictionary.getItems().isEmpty())
            errors.add("a dictionary must contain at least one item definition");

        Pattern idPattern = Pattern.compile("^[a-z][a-zA-Z0-9]+$");
        Set<String> naaccrIds = new HashSet<>();
        Set<Integer> naaccrNums = new HashSet<>();
        for (NaaccrDictionaryItem item : dictionary.getItems()) {
            // validate ID
            if (item.getNaaccrId() == null || item.getNaaccrId().trim().isEmpty())
                errors.add("'naaccrId' attribute is required");
            else if (!idPattern.matcher(item.getNaaccrId()).matches())
                errors.add("'naaccrId' attribute has a bad format (needs to start with a lower case letter, followed by letters and digits): " + item.getNaaccrId());
            else if (item.getNaaccrId().length() > 32)
                errors.add("'naaccrId' attribute can only be 32 characters long: " + item.getNaaccrId());
            else if (naaccrIds.contains(item.getNaaccrId()))
                errors.add("'naaccrId' attribute must be unique, already saw " + item.getNaaccrId());
            naaccrIds.add(item.getNaaccrId());

            // validate number
            if (item.getNaaccrNum() == null)
                errors.add("'naaccrNum' attribute is required");
            else if (naaccrNums.contains(item.getNaaccrNum()))
                errors.add("'naaccrNum' attribute must be unique, already saw " + item.getNaaccrNum());
            naaccrNums.add(item.getNaaccrNum());

            // validate name
            if (item.getNaaccrName() != null && item.getNaaccrName().length() > 50)
                errors.add("'naaccrName' attribute can only be 50 characters long: " + item.getNaaccrName());

            // validate length
            if (item.getLength() == null)
                errors.add("'length' attribute is required");

            // validate start column
            if (!versionSupportsStartColumns && item.getStartColumn() != null)
                errors.add("'startColumn' attribute is not allowed");
            if (isBaseDictionary && versionSupportsStartColumns && item.getStartColumn() == null)
                errors.add("'startColumn' attribute is required");
            if (!isBaseDictionary && SpecificationVersion.compareSpecifications(specVersion, SpecificationVersion.SPEC_1_1) < 0 && item.getStartColumn() == null)
                errors.add("'startColumn' attribute is required");

            // validate parent element
            if (item.getParentXmlElement() == null || item.getParentXmlElement().trim().isEmpty())
                errors.add("'parentXmlElement' attribute is required");
            else if (!NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(item.getParentXmlElement()) && !NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(item.getParentXmlElement())
                    && !NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(item.getParentXmlElement()))
                errors.add("invalid value for 'parentXmlElement' attribute: " + item.getParentXmlElement());

            // validate record type (null means all types, so that's OK)
            if (item.getRecordTypes() != null && !item.getRecordTypes().matches("[AMCI](,[AMCI])*"))
                errors.add("invalid value for 'recordTypes' attribute: " + item.getRecordTypes());

            // validate data type
            String type = item.getDataType();
            if (type != null && (!NAACCR_DATA_TYPE_ALPHA.equals(type) && !NAACCR_DATA_TYPE_DIGITS.equals(type) && !NAACCR_DATA_TYPE_MIXED.equals(type)) && !NAACCR_DATA_TYPE_NUMERIC.equals(type)
                    && !NAACCR_DATA_TYPE_TEXT.equals(type) && !NAACCR_DATA_TYPE_DATE.equals(type))
                errors.add("invalid value for 'dataType' attribute: " + item.getDataType());

            // validate unlimited text
            if (item.getAllowUnlimitedText() != null && SpecificationVersion.compareSpecifications(specVersion, SpecificationVersion.SPEC_1_6) >= 0)
                errors.add("invalid attribute 'allowUnlimitedText'");
            else if (Boolean.TRUE.equals(item.getAllowUnlimitedText()) && !NAACCR_DATA_TYPE_TEXT.equals(type))
                errors.add("allowUnlimitedText attribute can only be used with text data type");

            // validate padding
            if (item.getPadding() != null && (!NAACCR_PADDING_LEFT_BLANK.equals(item.getPadding()) && !NAACCR_PADDING_LEFT_ZERO.equals(item.getPadding()) && !NAACCR_PADDING_RIGHT_BLANK.equals(
                    item.getPadding()) && !NAACCR_PADDING_RIGHT_ZERO.equals(item.getPadding()) && !NAACCR_PADDING_NONE.equals(item.getPadding())))
                errors.add("invalid value for 'padding' attribute: " + item.getPadding());

            // validate trimming
            if (item.getTrim() != null && (!NAACCR_TRIM_ALL.equals(item.getTrim()) && !NAACCR_TRIM_NONE.equals(item.getTrim())))
                errors.add("invalid value for 'trim' attribute: " + item.getTrim());

            // validate regex
            if (item.getRegexValidation() != null) {
                if (SpecificationVersion.compareSpecifications(specVersion, SpecificationVersion.SPEC_1_2) >= 0)
                    errors.add("invalid attribute 'regexValidation'");
                else {
                    try {
                        //noinspection
                        Pattern.compile(item.getRegexValidation());
                    }
                    catch (PatternSyntaxException e) {
                        errors.add("invalid value for 'regexValidation' attribute: " + item.getRegexValidation());
                    }
                }
            }
        }

        // validate grouped items; since those can only appear in base dictionaries, the validation is going to be minimal
        if (isBaseDictionary) {
            if (!dictionary.getGroupedItems().isEmpty() && SpecificationVersion.compareSpecifications(specVersion, SpecificationVersion.SPEC_1_6) >= 0)
                errors.add("grouped items are not supported anymore");
            else {
                for (NaaccrDictionaryGroupedItem groupedItem : dictionary.getGroupedItems()) {
                    // validate ID
                    if (groupedItem.getNaaccrId() == null || groupedItem.getNaaccrId().trim().isEmpty())
                        errors.add("'naaccrId' attribute is required");
                    else if (!idPattern.matcher(groupedItem.getNaaccrId()).matches())
                        errors.add("'naaccrId' attribute has a bad format (needs to start with a lower case letter, followed by letters and digits): " + groupedItem.getNaaccrId());
                    else if (naaccrIds.contains(groupedItem.getNaaccrId()))
                        errors.add("'naaccrId' attribute for grouped item " + groupedItem.getNaaccrId() + " is not unique");
                    naaccrIds.add(groupedItem.getNaaccrId());

                    // validate number
                    if (groupedItem.getNaaccrNum() == null)
                        errors.add("'naaccrNum' attribute for grouped item " + groupedItem.getNaaccrId() + " is missing");
                    else if (naaccrNums.contains(groupedItem.getNaaccrNum()))
                        errors.add("'naaccrNum' attribute for grouped item " + groupedItem.getNaaccrId() + " must be unique, already saw " + groupedItem.getNaaccrNum());
                    naaccrNums.add(groupedItem.getNaaccrNum());

                    // validate start column
                    if (groupedItem.getStartColumn() != null) {
                        for (int idx = 0; idx < groupedItem.getContainedItemId().size(); idx++) {
                            NaaccrDictionaryItem containedItem = dictionary.getItemByNaaccrId(groupedItem.getContainedItemId().get(idx));
                            if (containedItem == null)
                                errors.add("grouped item " + groupedItem.getNaaccrId() + " references unknown item " + groupedItem.getContainedItemId().get(idx));
                            else if (idx == 0 && !groupedItem.getStartColumn().equals(containedItem.getStartColumn()))
                                errors.add("'startColumn' attribute for grouped item " + groupedItem.getNaaccrId() + " is not consistent with first contained item");
                        }
                    }
                }
            }
        }
        else if (!dictionary.getGroupedItems().isEmpty())
            errors.add("user-defined dictionaries cannot defined grouped items");

        // user dictionary specific validation (only if we know the version)
        if (!isBaseDictionary && naaccrVersionToUse != null) {
            NaaccrDictionary defaultUserDictionary = getDefaultUserDictionaryByVersion(naaccrVersionToUse);
            if (defaultUserDictionary != null) {
                NaaccrDictionary baseDictionary = getBaseDictionaryByVersion(naaccrVersionToUse);

                // we need the NAACCR numbers from the base dictionary...
                List<Integer> baseNumbers = baseDictionary.getItems().stream().map(NaaccrDictionaryItem::getNaaccrNum).collect(Collectors.toList());

                for (NaaccrDictionaryItem item : dictionary.getItems()) {

                    // can't use an internal base ID, ever
                    if (baseDictionary.getItemByNaaccrId(item.getNaaccrId()) != null)
                        errors.add("invalid value for 'naaccrId' attribute: " + item.getNaaccrId() + "; this ID is used in the standard dictionary");

                    // if an internal default user dictionary ID is used, then there are a bunch of attributes it can't re-defined.
                    NaaccrDictionaryItem defaultUserItem = defaultUserDictionary.getItemByNaaccrId(item.getNaaccrId());
                    if (defaultUserItem != null) {
                        if (!Objects.equals(defaultUserItem.getNaaccrNum(), item.getNaaccrNum()))
                            errors.add("invalid value for 'naaccrNum' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getNaaccrNum());
                        if (!Objects.equals(defaultUserItem.getNaaccrName(), item.getNaaccrName()))
                            errors.add("invalid value for 'naaccrName' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getNaaccrName());
                        if (item.getStartColumn() != null && defaultUserItem.getStartColumn() != null && !Objects.equals(defaultUserItem.getStartColumn(), item.getStartColumn()))
                            errors.add("invalid value for 'startColumn' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getStartColumn());
                        if (!Objects.equals(defaultUserItem.getLength(), item.getLength()))
                            errors.add("invalid value for 'length' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getLength());
                        if (!Objects.equals(defaultUserItem.getRecordTypes(), item.getRecordTypes()))
                            errors.add("invalid value for 'recordTypes' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getRecordTypes());
                        if (!Objects.equals(defaultUserItem.getParentXmlElement(), item.getParentXmlElement()))
                            errors.add("invalid value for 'parentXmlElement' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItem.getParentXmlElement());
                        // I really hate that the defaults are not loaded right away in the Java bean; I think that was a mistake!
                        String defaultUserItemType = defaultUserItem.getDataType() == null ? NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT : defaultUserItem.getDataType();
                        String itemType = item.getDataType() == null ? NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT : item.getDataType();
                        if (!Objects.equals(defaultUserItemType, itemType))
                            errors.add("invalid value for 'dataType' attribute of item '" + item.getNaaccrId() + "'; should be set to " + defaultUserItemType);
                    }
                    else {

                        // number cannot be one of the numbers from the base dictionary
                        if (baseNumbers.contains(item.getNaaccrNum()))
                            errors.add("invalid value for 'naaccrNum' attribute: " + item.getNaaccrNum() + "; number is already defined in corresponding base dictionary");

                        // range must be very specific for a user dictionary (deprecated)
                        if (SpecificationVersion.compareSpecifications(specVersion, SpecificationVersion.SPEC_1_3) < 0)
                            if (item.getNaaccrNum() < 9500 || item.getNaaccrNum() > 99999)
                                errors.add("invalid value for 'naaccrNum' attribute: " + item.getNaaccrNum() + "; allowed range is 9500-99999");

                        // this is tricky, but an item must fall into the columns of one of the items defined in the corresponding items defined in the default user dictionary
                        boolean preVersion21 = NumberUtils.isDigits(naaccrVersionToUse) && Integer.parseInt(naaccrVersionToUse) < 210;
                        if (preVersion21 && item.getStartColumn() != null) {
                            boolean fallInAllowedRange = false;
                            for (NaaccrDictionaryItem defaultItem : defaultUserDictionary.getItems()) {
                                if (item.getStartColumn() >= defaultItem.getStartColumn() && item.getStartColumn() + item.getLength() <= defaultItem.getStartColumn() + defaultItem.getLength()) {
                                    fallInAllowedRange = true;
                                    break;
                                }
                            }
                            if (!fallInAllowedRange)
                                errors.add("invalid value for 'startColumn' and/or 'length' attributes; user-defined items can only override state requestor item, NPCR item, or reserved gaps");
                        }
                    }
                }

                if (versionSupportsStartColumns) {
                    List<NaaccrDictionaryItem> sortedItems = dictionary.getItems().stream()
                            .filter(i -> i.getStartColumn() != null)
                            .sorted(Comparator.comparing(NaaccrDictionaryItem::getStartColumn))
                            .collect(Collectors.toList());
                    for (int i = 0; i < sortedItems.size() - 1; i++) {
                        NaaccrDictionaryItem item1 = sortedItems.get(i);
                        NaaccrDictionaryItem item2 = sortedItems.get(i + 1);
                        if (item1.getStartColumn() + item1.getLength() - 1 >= item2.getStartColumn())
                            errors.add("Item " + item1.getNaaccrId() + " and " + item2.getNaaccrId() + " are overlapping");
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Validates the given base dictionary and user-defined dictionaries; this method validate every dictionary individually but also runs some inter-dictionary validation.
     * @param baseDictionary base dictionary (required)
     * @param userDictionaries user-defined dictionaries, can be empty but not null
     * @return null if the combination of dictionaries is valid, the error message otherwise
     */
    public static List<String> validateDictionaries(NaaccrDictionary baseDictionary, Collection<NaaccrDictionary> userDictionaries) {

        // validate the base dictionary
        List<String> errors = new ArrayList<>(validateBaseDictionary(baseDictionary));

        // cache the user dictionaries by their ID for quick lookup (and check the the dictionaries are unique)
        Map<String, NaaccrDictionary> cachedUserDictionaries = new HashMap<>();
        for (NaaccrDictionary userDictionary : userDictionaries) {
            if (cachedUserDictionaries.containsKey(userDictionary.getDictionaryUri()))
                errors.add("user-defined dictionaries must be unique; found multiple URI '" + userDictionary.getDictionaryUri() + "'");
            cachedUserDictionaries.put(userDictionary.getDictionaryUri(), userDictionary);
        }

        // validate each user dictionary
        Map<String, String> idsDejaVu = new HashMap<>();
        Map<Integer, String> numbersDejaVue = new HashMap<>();
        for (NaaccrDictionary userDictionary : userDictionaries) {
            errors.addAll(validateUserDictionary(userDictionary, baseDictionary.getNaaccrVersion()));

            // make sure the provided version (if one is provided) agrees with the base version
            if (userDictionary.getNaaccrVersion() != null && !baseDictionary.getNaaccrVersion().equals(userDictionary.getNaaccrVersion()))
                errors.add("user-defined dictionary '" + userDictionary.getDictionaryUri() + "' doesn't define the same version as the base dictionary");

            // validate the items
            String dictId = userDictionary.getDictionaryUri();
            for (NaaccrDictionaryItem item : userDictionary.getItems()) {
                // NAACCR IDs defined in user dictionaries cannot be the same as the base NAACCR IDs
                if (baseDictionary.getItemByNaaccrId(item.getNaaccrId()) != null)
                    errors.add("user-defined dictionary '" + dictId + "' cannot use same NAACCR ID as a base item: " + item.getNaaccrId());

                // NAACCR Numbers defined in user dictionaries cannot be the same as the base NAACCR Numbers
                if (baseDictionary.getItemByNaaccrNum(item.getNaaccrNum()) != null)
                    errors.add("user-defined dictionary '" + dictId + "' cannot use same NAACCR Number as a base item: " + item.getNaaccrNum());

                // NAACCR IDs must be unique among all user dictionaries
                if (idsDejaVu.containsKey(item.getNaaccrId())) {
                    String existingItemDictId = idsDejaVu.get(item.getNaaccrId());

                    // it is OK to have several time the same ID as long as the other attributes are the same...
                    NaaccrDictionaryItem existingItem = cachedUserDictionaries.get(existingItemDictId).getItemByNaaccrId(item.getNaaccrId());

                    boolean sameAttributes = Objects.equals(item.getNaaccrName(), existingItem.getNaaccrName());
                    sameAttributes &= Objects.equals(item.getNaaccrNum(), existingItem.getNaaccrNum());
                    sameAttributes &= Objects.equals(item.getDataType(), existingItem.getDataType());
                    sameAttributes &= Objects.equals(item.getLength(), existingItem.getLength());
                    sameAttributes &= Objects.equals(item.getRecordTypes(), existingItem.getRecordTypes());
                    sameAttributes &= Objects.equals(item.getParentXmlElement(), existingItem.getParentXmlElement());
                    sameAttributes &= Objects.equals(item.getStartColumn(), existingItem.getStartColumn());
                    sameAttributes &= Objects.equals(item.getPadding(), existingItem.getPadding());
                    sameAttributes &= Objects.equals(item.getTrim(), existingItem.getTrim());

                    if (!sameAttributes)
                        errors.add("user-defined dictionary '" + dictId + "' and '" + existingItemDictId + "' both  define NAACCR ID '" + item.getNaaccrId() + "'");
                }
                else {
                    idsDejaVu.put(item.getNaaccrId(), dictId);

                    // NAACCR Numbers must be unique among all user dictionaries
                    if (numbersDejaVue.containsKey(item.getNaaccrNum()))
                        errors.add("user-defined dictionary '" + dictId + "' and '" + numbersDejaVue.get(item.getNaaccrNum()) + "' both  define NAACCR ID '" + item.getNaaccrNum() + "'");
                    numbersDejaVue.put(item.getNaaccrNum(), dictId);
                }
            }
        }

        // make sure there is no overlapping with the start columns (for the items that have them)
        List<NaaccrDictionaryItem> items = mergeDictionaries(baseDictionary, userDictionaries.toArray(new NaaccrDictionary[0])).getItems().stream()
                .filter(i -> i.getStartColumn() != null)
                .sorted(Comparator.comparing(NaaccrDictionaryItem::getStartColumn))
                .collect(Collectors.toList());
        NaaccrDictionaryItem currentItem = null;
        for (NaaccrDictionaryItem item : items) {
            if (currentItem != null && item.getStartColumn() <= currentItem.getStartColumn() + currentItem.getLength() - 1)
                errors.add("user-defined dictionaries define overlapping columns for items '" + currentItem.getNaaccrId() + "' and '" + item.getNaaccrId() + "'");
            currentItem = item;
        }

        return errors;
    }

    /**
     * Utility method to create a NAACCR ID from a display name:
     * <ol>
     * <li>Spaces, dashes, slashes, periods, underscores and ampersand are considered as word separators and replaced by a single space</li>
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

        // not including 10 (and after) because I feel like X might be a legit occurrence...
        Map<String, String> romanNumerals = new HashMap<>();
        romanNumerals.put("I", "1");
        romanNumerals.put("II", "2");
        romanNumerals.put("III", "3");
        romanNumerals.put("IV", "4");
        romanNumerals.put("V", "5");
        romanNumerals.put("VI", "6");
        romanNumerals.put("VII", "7");
        romanNumerals.put("VIII", "8");
        romanNumerals.put("IX", "9");

        String[] parts = StringUtils.split(name.replaceAll("\\s+|-+|/|_|\\.|&", " ").replaceAll("\\(.+\\)|[\\W&&[^\\s]]", ""), ' ');

        // special case, if the last two parts are both roman numeral, then put a "to" between them
        if (parts.length > 2 && romanNumerals.containsKey(parts[parts.length - 1]) && romanNumerals.containsKey(parts[parts.length - 2])) {
            String[] tmp = new String[parts.length + 1];
            System.arraycopy(parts, 0, tmp, 0, parts.length - 1);
            tmp[tmp.length - 2] = "to";
            tmp[tmp.length - 1] = parts[parts.length - 1];
            parts = tmp;
        }

        StringBuilder buf = new StringBuilder();
        buf.append(StringUtils.uncapitalize(parts[0].toLowerCase()));
        for (int i = 1; i < parts.length; i++) {
            if (romanNumerals.containsKey(parts[i]))
                buf.append(romanNumerals.get(parts[i]));
            else
                buf.append(StringUtils.capitalize(parts[i].toLowerCase()));
        }

        return buf.toString();
    }

    /**
     * Merges the base user dictionary for the given NAACCR version.
     * <br/><br/>
     * Sort order of the items is based on NAACCR XML ID.
     * @param naaccrVersion NAACCR version, required
     * @return a new merged dictionary containing the items of the requested dictionaries
     */
    public static NaaccrDictionary getMergedDictionaries(String naaccrVersion) {
        // this method makes sense for NAACCR versions that support a default user dictionary, but not so much for the more recent ones that don't;
        // to stay consistent this method still merges a base dictionary even if there is nothing to merge it with...
        NaaccrDictionary userDictionary = getDefaultUserDictionaryByVersion(naaccrVersion);
        return userDictionary == null ? mergeDictionaries(getBaseDictionaryByVersion(naaccrVersion)) : mergeDictionaries(getBaseDictionaryByVersion(naaccrVersion), userDictionary);
    }

    /**
     * Merges the given base dictionary and user dictionaries into one dictionary.
     * <br/><br/>
     * Sort order of the items is based on NAACCR XML ID.
     * @param baseDictionary base dictionary, required
     * @param userDictionaries user dictionaries, optional
     * @return a new merged dictionary containing the items of all the provided dictionaries.
     */
    public static NaaccrDictionary mergeDictionaries(NaaccrDictionary baseDictionary, NaaccrDictionary... userDictionaries) {
        if (baseDictionary == null)
            throw new IllegalStateException("Base dictionary is required");

        NaaccrDictionary result = new NaaccrDictionary();
        result.setNaaccrVersion(baseDictionary.getNaaccrVersion());
        result.setDictionaryUri(baseDictionary.getDictionaryUri() + "[merged]");
        result.setSpecificationVersion(baseDictionary.getSpecificationVersion());
        result.setDescription(baseDictionary.getDescription());

        List<NaaccrDictionaryItem> items = new ArrayList<>(baseDictionary.getItems());
        Set<String> processedIds = new HashSet<>();
        for (NaaccrDictionary userDictionary : userDictionaries) {
            for (NaaccrDictionaryItem item : userDictionary.getItems()) {
                if (!processedIds.contains(item.getNaaccrId())) {
                    items.add(item);
                    processedIds.add(item.getNaaccrId());
                }
            }
        }
        items.sort(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId));
        result.setItems(items);

        List<NaaccrDictionaryGroupedItem> groupedItems = new ArrayList<>(baseDictionary.getGroupedItems());
        for (NaaccrDictionary userDictionary : userDictionaries)
            groupedItems.addAll(userDictionary.getGroupedItems());
        groupedItems.sort(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId));
        result.setGroupedItems(groupedItems);

        return result;
    }

    /**
     * Write the given dictionary to the give target file using the CSV format.
     * <br/><br/>
     * Columns:
     * <ol>
     * <li>NAACCR ID</li>
     * <li>NAACCR number</li>
     * <li>Name</li>
     * <li>Start Column</li>
     * <li>Length</li>
     * <li>Record Types</li>
     * <li>Parent XML Element</li>
     * <li>Data Type</li>
     * </ol>
     * @param dictionary dictionary to write
     * @param file target CSV file
     */
    public static void writeDictionaryToCsv(NaaccrDictionary dictionary, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.US_ASCII))) {
            writeDictionaryToCsv(dictionary, writer);
        }
    }

    /**
     * Write the given dictionary to the give target writer using the CSV format.
     * <br/><br/>
     * Columns:
     * <ol>
     * <li>NAACCR ID</li>
     * <li>NAACCR number</li>
     * <li>Name</li>
     * <li>Start Column</li>
     * <li>Length</li>
     * <li>Record Types</li>
     * <li>Parent XML Element</li>
     * <li>Data Type</li>
     * </ol>
     * @param dictionary dictionary to write
     * @param writer target writer
     */
    public static void writeDictionaryToCsv(NaaccrDictionary dictionary, Writer writer) throws IOException {
        try {
            writer.write("NAACCR XML ID,NAACCR Number,Name,Start Column,Length,Record Types,Parent XML Element,Data Type");
            writer.write(System.lineSeparator());
            dictionary.getItems().stream()
                    .sorted(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId))
                    .forEach(item -> {
                        try {
                            writer.write(item.getNaaccrId());
                            writer.write(",");
                            writer.write(item.getNaaccrNum() == null ? "" : item.getNaaccrNum().toString());
                            writer.write(",\"");
                            writer.write(item.getNaaccrName() == null ? "" : item.getNaaccrName());
                            writer.write("\",");
                            writer.write(item.getStartColumn() == null ? "" : item.getStartColumn().toString());
                            writer.write(",");
                            writer.write(item.getLength().toString());
                            writer.write(",\"");
                            writer.write(item.getRecordTypes() == null ? "" : item.getRecordTypes());
                            writer.write("\",");
                            writer.write(item.getParentXmlElement() == null ? "" : item.getParentXmlElement());
                            writer.write(",");
                            writer.write(item.getDataType() == null ? "" : item.getDataType());
                            writer.write(System.lineSeparator());
                        }
                        catch (IOException | RuntimeException ex1) {
                            throw new IllegalStateException(ex1); // doing that to make sure the loop is broken...
                        }
                    });
        }
        catch (RuntimeException ex2) {
            throw new IOException(ex2);
        }
    }

    /**
     * Helper method to create the XStream object used to read and write XML.
     * @return a configured <code>XStream</code> object
     */
    private static XStream instanciateXStream() {
        XStream xstream = new XStream(new PureJavaReflectionProvider()) {
            @Override
            protected void setupConverters() {
                registerConverter(new NaaccrDictionaryConverter());
            }
        };

        // setup proper security environment
        xstream.addPermission(NoTypePermission.NONE);
        xstream.addPermission(new WildcardTypePermission(new String[] {"com.imsweb.naaccrxml.**"}));

        // setup entry point for reading full dictionary document
        xstream.alias("NaaccrDictionary", NaaccrDictionary.class);

        return xstream;
    }

    /**
     * This class is used to properly format dictionaries when they are written (mainly the indentation and the line feeds).
     */
    private static class NaaccrPrettyPrintWriter extends PrettyPrintWriter {

        // why isn't the internal writer protected instead of private??? I hate when people do that!
        private final NaaccrDictionary _dictionary;
        private QuickWriter _internalWriter;
        private String _currentItemId;

        public NaaccrPrettyPrintWriter(NaaccrDictionary dictionary, Writer writer, List<String> comment) {
            super(writer, new char[] {' ', ' ', ' ', ' '});
            _dictionary = dictionary;
            _currentItemId = null;

            try {
                writer.write("<?xml version=\"1.0\"?>\n\n");

                // write a disclaimer if we have to
                if (isBaseDictionary(dictionary) || isDefaultUserDictionary(dictionary)) {
                    writer.write("<!-- This dictionary is maintained and provided by the NAACCR organization; it should not be modified.\n");
                    writer.write("     If you need to define additional data items, please create your own user-defined dictionary.\n");
                    writer.write("     Visit https://www.naaccr.org/ for more information about the NAACCR XML Data Exchange Standard. -->\n\n");
                }

                // write a comment if we have to
                if (isBaseDictionary(dictionary) && comment != null && !comment.isEmpty()) {
                    for (String line : comment) {
                        writer.write(line);
                        writer.write("\n");
                    }
                    writer.write("\n");
                }

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
            if ("padding".equals(key) && (NAACCR_PADDING_RIGHT_BLANK.equals(value) || NAACCR_PADDING_NONE.equals(value)))
                return;
            if ("trim".equals(key) && NAACCR_TRIM_ALL.equals(value))
                return;
            super.addAttribute(key, value);
            if ("naaccrId".equals(key))
                _currentItemId = value;
            if (!isLastAttribute(key))
                _internalWriter.write("\n           ");
        }

        /**
         * This complex logic is needed because we wanted a non-standard formatting for the XML (in terms of indentations)...
         * @param attribute attribute to consider
         * @return true if the provided attribute is the last one on the line, false otherwise.
         */
        private boolean isLastAttribute(String attribute) {

            // for the root items, the converter always write the default namespace last
            if ("xmlns".equals(attribute))
                return true;

            NaaccrDictionaryItem item = _dictionary.getItemByNaaccrId(_currentItemId);
            if (item == null)
                item = _dictionary.getGroupedItemByNaaccrId(_currentItemId);
            if (item == null)
                return false;

            // for grouped items, the converter always write the "contains" last:
            if (item instanceof NaaccrDictionaryGroupedItem)
                return "contains".equals(attribute);

            // for items, the order is defined by the converter: we go in reverse order, we only have to check up to parentXmlElement which is required (so we know it's going to be there)
            if (item.getTrim() != null && !NAACCR_TRIM_ALL.equals(item.getTrim()))
                return "trim".equals(attribute);
            if (item.getPadding() != null && !NAACCR_PADDING_RIGHT_BLANK.equals(item.getPadding()) && !NAACCR_PADDING_NONE.equals(item.getPadding()))
                return "padding".equals(attribute);
            if (item.getRegexValidation() != null)
                return "regexValidation".equals(attribute);
            if (item.getDataType() != null && !NAACCR_DATA_TYPE_TEXT.equals(item.getDataType()))
                return "dataType".equals(attribute);
            return item.getParentXmlElement() != null && "parentXmlElement".equals(attribute);
        }
    }
}
