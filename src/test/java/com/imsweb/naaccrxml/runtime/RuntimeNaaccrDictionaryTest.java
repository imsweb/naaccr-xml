/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrIOException;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class RuntimeNaaccrDictionaryTest {

    @Test
    public void testValidation() {
        NaaccrDictionary baseDict = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_160);

        List<NaaccrDictionary> userDicts = new ArrayList<>();

        NaaccrDictionary dict1 = new NaaccrDictionary();
        dict1.setNaaccrVersion(NaaccrFormat.NAACCR_VERSION_160);
        dict1.setDictionaryUri("user-dictionary-1");
        dict1.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
        NaaccrDictionaryItem item1 = new NaaccrDictionaryItem();
        item1.setNaaccrId("myVariable1");
        item1.setNaaccrName("My Variable 1");
        item1.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item1.setNaaccrNum(10000);
        item1.setLength(1);
        dict1.addItem(item1);
        userDicts.add(dict1);

        NaaccrDictionary dict2 = new NaaccrDictionary();
        dict2.setNaaccrVersion(NaaccrFormat.NAACCR_VERSION_160);
        dict2.setDictionaryUri("user-dictionary-2");
        dict2.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
        NaaccrDictionaryItem item2 = new NaaccrDictionaryItem();
        item2.setNaaccrId("myVariable2");
        item2.setNaaccrName("My Variable 2");
        item2.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item2.setNaaccrNum(10001);
        item2.setLength(1);
        dict2.addItem(item2);
        userDicts.add(dict2);

        // regular case, everything is good
        assertValid("A", baseDict, userDicts);
        assertValid("I", baseDict, userDicts);

        // first dictionary uses same ID as base
        item1.setNaaccrId("primarySite");
        assertNotValid("A", baseDict, userDicts);
        item1.setNaaccrId("myVariable1");

        // second dictionary uses same number as base
        item2.setNaaccrNum(240);
        assertNotValid("A", baseDict, userDicts);
        item2.setNaaccrNum(10001);

        // both user dictionaries use same ID
        item2.setNaaccrId("myVariable1");
        assertNotValid("A", baseDict, userDicts);
        item2.setNaaccrId("myVariable2");

        // both user dictionaries use same number
        item2.setNaaccrNum(10000);
        assertNotValid("M", baseDict, userDicts);
        item2.setNaaccrNum(10001);

    }

    private void assertValid(String recordType, NaaccrDictionary baseDictionary, Collection<NaaccrDictionary> userDictionaries) {
        try {
            new RuntimeNaaccrDictionary(recordType, baseDictionary, userDictionaries);
        }
        catch (NaaccrIOException e) {
            throw new RuntimeException("Runtime dictionary was invalid!", e);
        }
    }

    private void assertNotValid(String recordType, NaaccrDictionary baseDictionary, Collection<NaaccrDictionary> userDictionaries) {
        try {
            new RuntimeNaaccrDictionary(recordType, baseDictionary, userDictionaries);
            throw new RuntimeException("Runtime dictionary was valid!");
        }
        catch (NaaccrIOException e) {
            // expected
        }
    }
}
