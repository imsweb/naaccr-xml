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
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;

import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;

public class NaaccrStreamConfiguration {

    protected XmlPullParser _parser;

    protected HierarchicalStreamDriver _driver;

    protected NaaccrPatientConverter _patientConverter;

    protected XStream _xstream;

    protected Map<String, String> _namespaces;

    private Map<String, Set<String>> _tags, _attributes;

    private static Set<String> _DEFAULT_TAGS = new HashSet<>();

    static {
        _DEFAULT_TAGS.add(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT);
        _DEFAULT_TAGS.add(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        _DEFAULT_TAGS.add(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        _DEFAULT_TAGS.add(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM);
    }

    public NaaccrStreamConfiguration() {
        _parser = createParser();
        _driver = createDriver(_parser);
        _patientConverter = createPatientConverter();
        _xstream = createXStream(_driver, _patientConverter);
        _namespaces = new HashMap<>();
        _tags = new HashMap<>();
        _attributes = new HashMap<>();
    }

    protected XmlPullParser createParser() {
        try {
            return XmlPullParserFactory.newInstance().newPullParser();
        }
        catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    protected HierarchicalStreamDriver createDriver(XmlPullParser parser) {
        return new XppDriver() {
            @Override
            protected synchronized XmlPullParser createParser() throws XmlPullParserException {
                return parser;
            }
        };
    }

    protected NaaccrPatientConverter createPatientConverter() {
        return new NaaccrPatientConverter();
    }

    protected XStream createXStream(HierarchicalStreamDriver driver, NaaccrPatientConverter patientConverter) {
        XStream xstream = new XStream(driver);

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

    public XmlPullParser getParser() {
        return _parser;
    }

    public HierarchicalStreamDriver getDriver() {
        return _driver;
    }

    public NaaccrPatientConverter getPatientConverter() {
        return _patientConverter;
    }

    public XStream getXstream() {
        return _xstream;
    }

    public void registerNamespace(String namespacePrefix, String namespaceUri) {
        if (_namespaces.containsKey(namespacePrefix))
            throw new RuntimeException("Namespace prefix '" + namespacePrefix + "' has already been registered");
        _namespaces.put(namespacePrefix, namespaceUri);
    }

    public Map<String, String> getRegisterNamespaces() {
        return Collections.unmodifiableMap(_namespaces);
    }

    public void registerTag(String namespacePrefix, String tagName, Class<?> clazz) {
        if (!_namespaces.containsKey(namespacePrefix))
            throw new RuntimeException("Namespace prefix '" + namespacePrefix + "' has not been registered yet");
        _xstream.alias(namespacePrefix + ":" + tagName, clazz);
        _tags.computeIfAbsent(namespacePrefix, k -> new HashSet<>()).add(tagName);
    }

    public void registerTag(String namespacePrefix, String tagName, Class<?> clazz, String fieldName) {
        if (!_namespaces.containsKey(namespacePrefix))
            throw new RuntimeException("Namespace prefix '" + namespacePrefix + "' has not been registered yet");
        _xstream.aliasField(namespacePrefix + ":" + tagName, clazz, fieldName);
        _tags.computeIfAbsent(namespacePrefix, k -> new HashSet<>()).add(tagName);
    }

    public void registerAttribute(String namespacePrefix, String attributeName, Class<?> clazz, String fieldName) {
        if (!_namespaces.containsKey(namespacePrefix))
            throw new RuntimeException("Namespace prefix '" + namespacePrefix + "' has not been registered yet");
        _xstream.aliasAttribute(clazz, fieldName, namespacePrefix + ":" + attributeName);
        _attributes.computeIfAbsent(namespacePrefix, k -> new HashSet<>()).add(attributeName);
    }

    public void registerImplicitCollection(Class<?> clazz, String field, Class<?> fieldType) {
        _xstream.addImplicitCollection(clazz, field, fieldType);
    }

    public void registerConverter(Converter converter) {
        _xstream.registerConverter(converter);
    }

    public Set<String> getAllowedTagsForNamespacePrefix(String prefix) {
        if (prefix == null)
            return _DEFAULT_TAGS;
        return _tags.get(prefix);
    }
}
