/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.runtime;

import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;

public class NaaccrStreamConfiguration {
    
    protected XmlPullParser _parser;
    
    protected HierarchicalStreamDriver _driver;
    
    protected NaaccrPatientConverter _patientConverter;
    
    protected XStream _xstream;
    
    public NaaccrStreamConfiguration() {
        _parser = createParser();

        _driver = new XppDriver() {
            @Override
            protected synchronized XmlPullParser createParser() throws XmlPullParserException {
                return _parser;
            }
        };
        _patientConverter = createPatientConverter();
        _xstream = createXStream(_driver, _patientConverter);
    }
    
    protected XmlPullParser createParser() {
        try {
            return XmlPullParserFactory.newInstance().newPullParser();
        }
        catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }
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
}
