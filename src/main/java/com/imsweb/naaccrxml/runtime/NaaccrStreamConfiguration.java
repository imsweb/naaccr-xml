/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;

public class NaaccrStreamConfiguration {

    protected XmlPullParser _parser;

    protected HierarchicalStreamDriver _driver;

    protected NaaccrPatientConverter _patientConverter;

    protected XStream _xstream;

    protected Map<String, Set<String>> _allowedTagsPerNamespacePrefix;

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
        _allowedTagsPerNamespacePrefix = new HashMap<>();
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
        XStream xstream = new XStream(driver) {
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public Class realClass(String elementName) {
                        // I really don't like this solution, but this makes sure that when XStream looks for a suitable class, it doesn't take the namespace into account
                        int idx = elementName.indexOf(':');
                        if (idx != -1)
                            return next.realClass(elementName.substring(idx + 1));
                        return next.realClass(elementName);
                    }
                };
            }
        };

        // tell XStream how to read/write our main entities
        xstream.alias(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT, NaaccrData.class);
        xstream.alias(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM, Item.class);
        xstream.alias(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT, Patient.class);

        // add some attributes
        xstream.aliasAttribute(NaaccrData.class, "_baseDictionaryUri", NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT);
        xstream.aliasAttribute(NaaccrData.class, "_userDictionaryUri", NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT);
        xstream.aliasAttribute(NaaccrData.class, "_recordType", NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE);
        xstream.aliasAttribute(NaaccrData.class, "_timeGenerated", NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED);

        // all collections should be wrap into collection tags, but it's nicer to omit them in the XML; we have to tell XStream though
        xstream.addImplicitCollection(NaaccrData.class, "_items", Item.class);
        xstream.addImplicitCollection(NaaccrData.class, "_patients", Patient.class);

        // handle patients
        xstream.registerConverter(patientConverter);

        return xstream;
    }

    public void setAllowedTagsForNamespacePrefix(String prefix, String... allowedTags) {
        _allowedTagsPerNamespacePrefix.computeIfAbsent(prefix, k -> new HashSet<>()).addAll(Arrays.asList(allowedTags));
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

    public Set<String> getAllowedTagsForNamespacePrefix(String prefix) {
        if (prefix == null)
            return _DEFAULT_TAGS;
        return _allowedTagsPerNamespacePrefix.get(prefix);
    }
}
