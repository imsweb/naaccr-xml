/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;
import com.thoughtworks.xstream.converters.basic.ByteConverter;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.converters.basic.DoubleConverter;
import com.thoughtworks.xstream.converters.basic.FloatConverter;
import com.thoughtworks.xstream.converters.basic.IntConverter;
import com.thoughtworks.xstream.converters.basic.LongConverter;
import com.thoughtworks.xstream.converters.basic.NullConverter;
import com.thoughtworks.xstream.converters.basic.ShortConverter;
import com.thoughtworks.xstream.converters.basic.StringConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.WildcardTypePermission;

import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;

public class NaaccrStreamConfiguration {

    // the parser used for reading operations
    protected XmlPullParser _parser;

    // the XStream driver used for reading and writing operations
    protected HierarchicalStreamDriver _driver;

    // the patient converter responsible for defining how to read and write patient objects
    protected NaaccrPatientConverter _patientConverter;

    // the instance of XStream to use for reading and writing operations
    protected XStream _xstream;

    // the registered namespace (in addition to the default one)
    protected Map<String, String> _namespaces;

    // the registered tags for each registered namespaces
    protected Map<String, Set<String>> _tags;

    // the tags defined in the default NAACCR namespace
    protected Set<String> _defaultTags;

    // cached runtime dictionary
    protected RuntimeNaaccrDictionary _cachedDictionary;

    /**
     * Convenience method to make the code look nicer, but it really just calls the default constructor!
     * @return an instance of the configuration with all default values.
     */
    public static NaaccrStreamConfiguration getDefault() {
        return new NaaccrStreamConfiguration();
    }

    /**
     * Constructor.
     */
    public NaaccrStreamConfiguration() {
        _parser = createParser();
        _driver = createDriver(_parser);
        _patientConverter = createPatientConverter();
        _xstream = createXStream(_driver, _patientConverter);
        _namespaces = new HashMap<>();
        _tags = new HashMap<>();

        Set<String> defaultTags = new HashSet<>();
        defaultTags.add(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT);
        defaultTags.add(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        defaultTags.add(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        defaultTags.add(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM);
        _defaultTags = Collections.unmodifiableSet(defaultTags);
    }

    /**
     * Creates a parser to use for all reading operations.
     * @return a parser, never null
     */
    protected XmlPullParser createParser() {
        try {
            return XmlPullParserFactory.newInstance().newPullParser();
        }
        catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an XStream driver to use for all reading and writing operations
     * @param parser parser (see createParser())
     * @return an XStream driver, never null
     */
    protected HierarchicalStreamDriver createDriver(XmlPullParser parser) {
        return new XppDriver() {
            @Override
            protected synchronized XmlPullParser createParser() {
                return parser;
            }
        };
    }

    /**
     * Creates the patient converter to use for reading and writing patient objects.
     * @return a patient converter, never null
     */
    protected NaaccrPatientConverter createPatientConverter() {
        return new NaaccrPatientConverter();
    }

    /**
     * Creates the instance of XStream to us for all reading and writing operations
     * @param driver an XStream driver (see createDriver())
     * @param patientConverter a patient converter (see createPatientConverter())
     * @return an instance of XStream, never null
     */
    protected XStream createXStream(HierarchicalStreamDriver driver, NaaccrPatientConverter patientConverter) {
        XStream xstream = new XStream(driver) {
            @Override
            protected void setupConverters() {
                registerConverter(new NullConverter(), PRIORITY_VERY_HIGH);
                registerConverter(new IntConverter(), PRIORITY_NORMAL);
                registerConverter(new FloatConverter(), PRIORITY_NORMAL);
                registerConverter(new DoubleConverter(), PRIORITY_NORMAL);
                registerConverter(new LongConverter(), PRIORITY_NORMAL);
                registerConverter(new ShortConverter(), PRIORITY_NORMAL);
                registerConverter(new BooleanConverter(), PRIORITY_NORMAL);
                registerConverter(new ByteConverter(), PRIORITY_NORMAL);
                registerConverter(new StringConverter(), PRIORITY_NORMAL);
                registerConverter(new DateConverter(), PRIORITY_NORMAL);
                registerConverter(new CollectionConverter(getMapper()), PRIORITY_NORMAL);
                registerConverter(new ReflectionConverter(getMapper(), getReflectionProvider()), PRIORITY_VERY_LOW);
            }
        };

        // setup proper security environment
        xstream.addPermission(NoTypePermission.NONE);
        xstream.addPermission(new WildcardTypePermission(new String[] {"com.imsweb.naaccrxml.**"}));

        // tell XStream how to read/write our main entities
        xstream.alias(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT, NaaccrData.class);
        xstream.alias(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM, Item.class);
        xstream.alias(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT, Patient.class);

        // add some attributes
        xstream.aliasAttribute(NaaccrData.class, "_baseDictionaryUri", NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT);
        xstream.aliasAttribute(NaaccrData.class, "_userDictionaryUri", NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT);
        xstream.aliasAttribute(NaaccrData.class, "_recordType", NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE);
        xstream.aliasAttribute(NaaccrData.class, "_timeGenerated", NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED);
        xstream.aliasAttribute(NaaccrData.class, "_specificationVersion", NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_SPEC_VERSION);

        // all collections should be wrap into collection tags, but it's nicer to omit them in the XML; we have to tell XStream though
        xstream.addImplicitCollection(NaaccrData.class, "_items", Item.class);
        xstream.addImplicitCollection(NaaccrData.class, "_patients", Patient.class);

        // handle patients
        xstream.registerConverter(patientConverter);

        return xstream;
    }

    /**
     * Returns the parser that is used for all reading operations.
     * @return a parser, never null
     */
    public XmlPullParser getParser() {
        return _parser;
    }

    /**
     * Returns the XStream driver used for all reading and writing operations.
     * @return an XStream driver, never null
     */
    public HierarchicalStreamDriver getDriver() {
        return _driver;
    }

    /**
     * Returns the patient converter to use for reading and writing patient objects.
     * @return a patient converter, never null
     */
    public NaaccrPatientConverter getPatientConverter() {
        return _patientConverter;
    }

    /**
     * Returns the instance of XStream to use for all reading and writing operations.
     * @return an instance of XStream, never null
     */
    public XStream getXstream() {
        return _xstream;
    }

    /**
     * Returns the runtime dictionary cached by this configuration, null if non is currently cached.
     * @return runtime dictionary, maybe null
     */
    public RuntimeNaaccrDictionary getCachedDictionary() {
        return _cachedDictionary;
    }

    /**
     * The library calls this method to cache the runtime dictionary among several stream instantiations.
     * <br/><br/>
     * This method can be used to set the cached dictionary to null between the stream instantiations (to disable the caching);
     * it shouldn't be used to set an actual runtime dictionary (only the library should do that).
     * @param cachedDictionary cached dictionary to set
     */
    public void setCachedDictionary(RuntimeNaaccrDictionary cachedDictionary) {
        _cachedDictionary = cachedDictionary;
    }

    /**
     * Registers a namespace for a given namespace prefix. This method must be called before registering any tags or attributes
     * for that namespace. Note that extensions require namespaces to work properly.
     * @param namespacePrefix the namespace prefix, cannot be null
     * @param namespaceUri the namespace URI, cannot be null
     */
    public void registerNamespace(String namespacePrefix, String namespaceUri) {
        if (_namespaces.containsKey(namespacePrefix))
            throw new RuntimeException("Namespace prefix '" + namespacePrefix + "' has already been registered");
        _namespaces.put(namespacePrefix, namespaceUri);
    }

    /**
     * Returns all the registered namespaces, keyed by their prefix.
     * @return a map of namespaces keyed by their prefix, maybe empty but never null
     */
    public Map<String, String> getRegisterNamespaces() {
        return Collections.unmodifiableMap(_namespaces);
    }

    /**
     * Registers a tag corresponding to a specific class, in the given namespace.
     * @param namespacePrefix namespace prefix, required
     * @param tagName tag name, required
     * @param clazz class corresponding to the tag name, required
     */
    public void registerTag(String namespacePrefix, String tagName, Class<?> clazz) {
        if (!_namespaces.containsKey(namespacePrefix))
            throw new RuntimeException("Namespace prefix '" + namespacePrefix + "' has not been registered yet");
        _xstream.alias(namespacePrefix + ":" + tagName, clazz);
        _xstream.addPermission(new WildcardTypePermission(new String[] {clazz.getName()}));
        _tags.computeIfAbsent(namespacePrefix, k -> new HashSet<>()).add(tagName);
    }

    /**
     * Registers a tag corresponding to a specific field of a specific class, in a the given namespace.
     * @param namespacePrefix namespace prefix, required
     * @param tagName tag name, required
     * @param clazz class containing the field, required
     * @param fieldName field name, required
     * @param fieldClass field type, required
     */
    public void registerTag(String namespacePrefix, String tagName, Class<?> clazz, String fieldName, Class<?> fieldClass) {
        if (!_namespaces.containsKey(namespacePrefix))
            throw new RuntimeException("Namespace prefix '" + namespacePrefix + "' has not been registered yet");
        _xstream.alias(namespacePrefix + ":" + tagName, fieldClass);
        _xstream.aliasField(namespacePrefix + ":" + tagName, clazz, fieldName);
        _tags.computeIfAbsent(namespacePrefix, k -> new HashSet<>()).add(tagName);
    }

    /**
     * Registers an attribute corresponding to a specific field of a specific class, in a given namespace.
     * @param namespacePrefix namespace prefix, required
     * @param attributeName attribute name, required
     * @param clazz class containing the field, required
     * @param fieldName field name, required
     * @param fieldClass field type, required
     */
    public void registerAttribute(String namespacePrefix, String attributeName, Class<?> clazz, String fieldName, Class<?> fieldClass) {
        if (!_namespaces.containsKey(namespacePrefix))
            throw new RuntimeException("Namespace prefix '" + namespacePrefix + "' has not been registered yet");
        _xstream.aliasAttribute(clazz, fieldName, namespacePrefix + ":" + attributeName);
        _xstream.useAttributeFor(fieldName, fieldClass);
    }

    /**
     * Register an implicit collection (a collection that shouldn't appear as a tag in the XML).
     * @param clazz class containing the field (the collection in this case), required
     * @param fieldName field name, required
     * @param fieldClass field type, required
     */
    public void registerImplicitCollection(Class<?> clazz, String fieldName, Class<?> fieldClass) {
        _xstream.addImplicitCollection(clazz, fieldName, fieldClass);
    }

    /**
     * Registers an XStream data converter.
     * @param converter converter to register, required
     */
    public void registerConverter(Converter converter) {
        _xstream.registerConverter(converter);
    }

    /**
     * Returns the tags allowed for a given namespace prefix (null means the default namespace).
     * @param namespacePrefix namespace prefix
     * @return set of allowed tags, maybe empty but never null
     */
    public Set<String> getAllowedTagsForNamespacePrefix(String namespacePrefix) {
        if (namespacePrefix == null)
            return _defaultTags;
        return _tags.get(namespacePrefix);
    }
}
