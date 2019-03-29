/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

import static org.junit.Assert.fail;

public class NaaccrXmlDictionaryUtilsTest {

    @Test
    public void testInternalDictionaries() throws IOException {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            List<NaaccrDictionaryItem> items = new ArrayList<>();

            // make sure internal base dictionaries are valid
            try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-dictionary-" + version + ".xml"))) {
                NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(reader);
                Assert.assertTrue(version, NaaccrXmlDictionaryUtils.validateBaseDictionary(dict).isEmpty());
                Assert.assertTrue(version, NaaccrXmlDictionaryUtils.BASE_DICTIONARY_URI_PATTERN.matcher(dict.getDictionaryUri()).matches());
                items.addAll(dict.getItems());
            }

            // make sure internal default user dictionaries are valid
            try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("user-defined-naaccr-dictionary-" + version + ".xml"))) {
                NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(reader);
                Assert.assertTrue(version, NaaccrXmlDictionaryUtils.validateUserDictionary(dict).isEmpty());
                Assert.assertTrue(version, NaaccrXmlDictionaryUtils.DEFAULT_USER_DICTIONARY_URI_PATTERN.matcher(dict.getDictionaryUri()).matches());
                items.addAll(dict.getItems());
            }

            // make sure the combination of fields doesn't leave any gaps
            items.sort(Comparator.comparing(NaaccrDictionaryItem::getStartColumn));
            for (int i = 0; i < items.size() - 1; i++)
                if (items.get(i).getStartColumn() + items.get(i).getLength() != items.get(i + 1).getStartColumn())
                    fail("Found a gap after item " + items.get(i).getNaaccrId());

            // make sure IDs are no longer than 50 characters (this will be enforced by the standard in a future version)
            for (NaaccrDictionaryItem item : items)
                if (item.getNaaccrId().length() > 50)
                    fail("Found item with ID too long: " + item.getNaaccrId());
        }

        // clear the caches, force other tests to reload them again
        NaaccrXmlDictionaryUtils.clearCachedDictionaries();
    }

    @Test
    public void testReadDictionary() throws IOException {

        // get a base dictionary
        NaaccrDictionary baseDictionary1 = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_140);
        NaaccrDictionary baseDictionary2 = NaaccrXmlDictionaryUtils.getBaseDictionaryByUri(baseDictionary1.getDictionaryUri());
        Assert.assertEquals(baseDictionary1.getDictionaryUri(), baseDictionary2.getDictionaryUri());
        Assert.assertEquals(baseDictionary1.getNaaccrVersion(), baseDictionary2.getNaaccrVersion());
        Assert.assertEquals(baseDictionary1.getSpecificationVersion(), baseDictionary2.getSpecificationVersion());
        Assert.assertEquals(baseDictionary1.getItems().size(), baseDictionary2.getItems().size());
        Assert.assertEquals(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION, baseDictionary1.getSpecificationVersion());

        // get a default user dictionary
        NaaccrDictionary defaultUserDictionary1 = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_140);
        NaaccrDictionary defaultUserDictionary2 = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByUri(baseDictionary1.getDictionaryUri());
        Assert.assertEquals(defaultUserDictionary1.getDictionaryUri(), defaultUserDictionary2.getDictionaryUri());
        Assert.assertEquals(defaultUserDictionary1.getNaaccrVersion(), defaultUserDictionary2.getNaaccrVersion());
        Assert.assertEquals(defaultUserDictionary1.getItems().size(), defaultUserDictionary2.getItems().size());
        Assert.assertEquals(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION, defaultUserDictionary1.getSpecificationVersion());

        // make sure N18 dictionary doesn't have any references to the long IDs
        for (String oldId : NaaccrXmlDictionaryUtils.getRenamedLongNaaccr18Ids().keySet())
            Assert.assertNull(NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_180).getItemByNaaccrId(oldId));

        // read a provided user dictionary
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140.xml"))) {
            NaaccrDictionary defaultUserDictionary = NaaccrXmlDictionaryUtils.readDictionary(reader);
            Assert.assertEquals(SpecificationVersion.SPEC_1_0, defaultUserDictionary.getSpecificationVersion());
            Assert.assertEquals(4, defaultUserDictionary.getItems().size());
        }

        // try to read a user dictionary with an error (bad start column)
        boolean exceptionAppend = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140-bad1.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exceptionAppend = true;
        }
        Assert.assertTrue(exceptionAppend);

        // try to read a user dictionary with another error (NPCR item definition redefines the NAACCR number)
        exceptionAppend = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140-bad2.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exceptionAppend = true;
        }
        Assert.assertTrue(exceptionAppend);

        // this one defines an item in a bad location, but it doesn't define a NAACCR version, so no exception, but if a NAACCR version is provided, the validation should fail
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140-bad3.xml"))) {
            NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(reader);
            Assert.assertTrue(NaaccrXmlDictionaryUtils.validateUserDictionary(dict).isEmpty());
            Assert.assertFalse(NaaccrXmlDictionaryUtils.validateUserDictionary(dict, "140").isEmpty());
            Assert.assertFalse(NaaccrXmlDictionaryUtils.validateUserDictionary(dict, "160").isEmpty());
        }

        // try to read a user dictionary with another error (missing dictionaryUri attribute)
        exceptionAppend = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140-bad4.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exceptionAppend = true;
        }
        Assert.assertTrue(exceptionAppend);
    }

    @Test
    public void testWriteDictionary() throws IOException {

        NaaccrDictionary dict = new NaaccrDictionary();
        dict.setNaaccrVersion("140");
        dict.setDictionaryUri("whatever");
        dict.setDescription("Another whatever");
        NaaccrDictionaryItem item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(10000);
        item.setRecordTypes("A,M,C,I");
        item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_NUMERIC);
        item.setLength(2);
        item.setStartColumn(2340);
        item.setNaaccrName("My Variable");
        item.setSourceOfStandard("ME");
        item.setPadding(NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_BLANK);
        item.setTrim(NaaccrXmlDictionaryUtils.NAACCR_TRIM_NONE);
        item.setRegexValidation("0[0-8]");
        dict.addItem(item);

        // write using a writer
        File file = TestingUtils.createFile("dict-write-test.xml", false);
        try (Writer writer = new FileWriter(file)) {
            NaaccrXmlDictionaryUtils.writeDictionary(dict, writer);
        }
        NaaccrDictionary newDict = NaaccrXmlDictionaryUtils.readDictionary(file);
        Assert.assertEquals("140", newDict.getNaaccrVersion());
        Assert.assertEquals("whatever", newDict.getDictionaryUri());
        Assert.assertEquals("Another whatever", newDict.getDescription());
        Assert.assertEquals(1, newDict.getItems().size());
        Assert.assertNotNull(newDict.getItemByNaaccrId("myVariable"));
        Assert.assertNotNull(newDict.getItemByNaaccrNum(10000));

        // write using a file
        NaaccrXmlDictionaryUtils.writeDictionary(dict, file);
        newDict = NaaccrXmlDictionaryUtils.readDictionary(file);
        Assert.assertEquals("140", newDict.getNaaccrVersion());
        Assert.assertEquals("whatever", newDict.getDictionaryUri());
        Assert.assertEquals("Another whatever", newDict.getDescription());
        Assert.assertEquals(1, newDict.getItems().size());
        Assert.assertNotNull(newDict.getItemByNaaccrId("myVariable"));
        Assert.assertNotNull(newDict.getItemByNaaccrNum(10000));
    }

    @Test
    public void testValidateUserDictionary() {

        NaaccrDictionary dict = new NaaccrDictionary();
        dict.setNaaccrVersion("160");
        dict.setDictionaryUri("whatever");
        dict.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);

        // validate good dictionary
        NaaccrDictionaryItem item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariable");
        item.setNaaccrName("My Variable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item.setNaaccrNum(10000);
        item.setLength(1);
        dict.setItems(Collections.singletonList(item));
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateUserDictionary(dict).isEmpty());

        // this one re-defines the NPCR field but with a different number, which is not allowed
        item = new NaaccrDictionaryItem();
        item.setNaaccrId("npcrSpecificField");
        item.setNaaccrName("NPCR Specific Field");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(10000);
        item.setLength(75);
        item.setRecordTypes("A,M,C,I");
        dict.setItems(Collections.singletonList(item));
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.validateUserDictionary(dict));

        // this one defines an item that has the same number as the base
        item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariable");
        item.setNaaccrName("My Variable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(240);
        item.setLength(1);
        dict.setItems(Collections.singletonList(item));
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.validateUserDictionary(dict));
        item.setNaaccrNum(999999);
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateUserDictionary(dict).isEmpty());

        // this one defines an item that is too long
        item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariableWithSomeVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongId!");
        item.setNaaccrName("My Variable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(10000);
        item.setLength(1);
        dict.setItems(Collections.singletonList(item));
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.validateUserDictionary(dict));
        item.setNaaccrId("myVariable");
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateUserDictionary(dict).isEmpty());
    }

    @Test
    public void testValidateDictionaries() {

        // base dictionary by itself is valid
        NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("160");
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, Collections.emptyList()).isEmpty());

        // add two valid user-defined dictionaries
        List<NaaccrDictionary> userDictionaries = new ArrayList<>();
        NaaccrDictionary userDictionary1 = new NaaccrDictionary();
        userDictionary1.setNaaccrVersion("160");
        userDictionary1.setDictionaryUri("whatever1");
        userDictionary1.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
        NaaccrDictionaryItem item1 = new NaaccrDictionaryItem();
        item1.setNaaccrId("myVariable1");
        item1.setNaaccrName("My Variable1");
        item1.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item1.setNaaccrNum(10000);
        item1.setLength(1);
        userDictionary1.setItems(Collections.singletonList(item1));
        userDictionaries.add(userDictionary1);
        NaaccrDictionary userDictionary2 = new NaaccrDictionary();
        userDictionary2.setDictionaryUri("whatever2");
        userDictionary2.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
        NaaccrDictionaryItem item2 = new NaaccrDictionaryItem();
        item2.setNaaccrId("myVariable2");
        item2.setNaaccrName("My Variable2");
        item2.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item2.setNaaccrNum(10001);
        item2.setLength(1);
        userDictionary2.setItems(Collections.singletonList(item2));
        userDictionaries.add(userDictionary2);
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());

        // NAACCR ID repeats a base one
        item2.setNaaccrId("vitalStatus");
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
        item2.setNaaccrId("myVariable2");

        // NAACCR ID repeats a user-defined ones
        item2.setNaaccrId("myVariable1");
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
        item2.setNaaccrId("myVariable2");

        // NAACCR number repeats a base one
        item2.setNaaccrNum(10);
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
        item2.setNaaccrNum(10001);

        // NAACCR number repeats a user-defined one
        item2.setNaaccrNum(10000);
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
        item2.setNaaccrNum(10001);

        // items overlap
        item1.setStartColumn(2340);
        item2.setStartColumn(2340);
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
    }

    @Test
    public void testValidationRegex() {

        //  "alpha": uppercase letters, A-Z, no spaces, full length needs to be filled in
        Pattern pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_ALPHA);
        Assert.assertTrue(pattern.matcher("A").matches());
        Assert.assertTrue(pattern.matcher("AVALUE").matches());
        Assert.assertFalse(pattern.matcher("A VALUE").matches());
        Assert.assertFalse(pattern.matcher(" A").matches());
        Assert.assertFalse(pattern.matcher("A ").matches());
        Assert.assertFalse(pattern.matcher("a").matches());
        Assert.assertFalse(pattern.matcher("a value").matches());
        Assert.assertFalse(pattern.matcher("123").matches());
        Assert.assertFalse(pattern.matcher("A123").matches());
        Assert.assertFalse(pattern.matcher("123A").matches());
        Assert.assertFalse(pattern.matcher("A!").matches());

        // "digits": digits, 0-9, no spaces, full length needs to be filled in
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
        Assert.assertTrue(pattern.matcher("1").matches());
        Assert.assertTrue(pattern.matcher("123").matches());
        Assert.assertFalse(pattern.matcher("12 3").matches());
        Assert.assertFalse(pattern.matcher(" 1").matches());
        Assert.assertFalse(pattern.matcher("1 ").matches());
        Assert.assertFalse(pattern.matcher("a value").matches());
        Assert.assertFalse(pattern.matcher("1A23").matches());
        Assert.assertFalse(pattern.matcher("A123").matches());
        Assert.assertFalse(pattern.matcher("123A").matches());
        Assert.assertFalse(pattern.matcher("1!").matches());

        // "mixed": uppercase letters or digits, A-Z,0-9, no spaces, full length needs to be filled in
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_MIXED);
        Assert.assertTrue(pattern.matcher("A").matches());
        Assert.assertTrue(pattern.matcher("AVALUE").matches());
        Assert.assertFalse(pattern.matcher("A VALUE").matches());
        Assert.assertFalse(pattern.matcher(" A").matches());
        Assert.assertFalse(pattern.matcher("A ").matches());
        Assert.assertFalse(pattern.matcher("a").matches());
        Assert.assertFalse(pattern.matcher("a value").matches());
        Assert.assertTrue(pattern.matcher("123").matches());
        Assert.assertTrue(pattern.matcher("A123").matches());
        Assert.assertTrue(pattern.matcher("123A").matches());
        Assert.assertFalse(pattern.matcher("A!").matches());

        // "numeric": digits, 0-9 with optional period, no spaces but value can be smaller than the length
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_NUMERIC);
        Assert.assertTrue(pattern.matcher("1").matches());
        Assert.assertTrue(pattern.matcher("123").matches());
        Assert.assertFalse(pattern.matcher("12 3").matches());
        Assert.assertFalse(pattern.matcher(" 1").matches());
        Assert.assertFalse(pattern.matcher("1 ").matches());
        Assert.assertFalse(pattern.matcher("a value").matches());
        Assert.assertFalse(pattern.matcher("1A23").matches());
        Assert.assertFalse(pattern.matcher("A123").matches());
        Assert.assertFalse(pattern.matcher("123A").matches());
        Assert.assertFalse(pattern.matcher("1!").matches());
        Assert.assertTrue(pattern.matcher("1.0").matches());
        Assert.assertTrue(pattern.matcher("0.123").matches());
        Assert.assertFalse(pattern.matcher(".123").matches());
        Assert.assertFalse(pattern.matcher("1.").matches());

        // "text": no checking on this value
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
        Assert.assertTrue(pattern.matcher("A").matches());
        Assert.assertTrue(pattern.matcher("AVALUE").matches());
        Assert.assertTrue(pattern.matcher("A VALUE").matches());
        Assert.assertTrue(pattern.matcher(" A").matches());
        Assert.assertTrue(pattern.matcher("A ").matches());
        Assert.assertTrue(pattern.matcher("a").matches());
        Assert.assertTrue(pattern.matcher("a value").matches());
        Assert.assertTrue(pattern.matcher("123").matches());
        Assert.assertTrue(pattern.matcher("A123").matches());
        Assert.assertTrue(pattern.matcher("123A").matches());
        Assert.assertTrue(pattern.matcher("A!").matches());

        // "date": digits, YYYY or YYYYMM or YYYYMMDD
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DATE);
        Assert.assertTrue(pattern.matcher("20100615").matches());
        Assert.assertTrue(pattern.matcher("201006").matches());
        Assert.assertTrue(pattern.matcher("2010").matches());
        Assert.assertFalse(pattern.matcher("201006  ").matches());
        Assert.assertFalse(pattern.matcher("2010  15").matches());
        Assert.assertFalse(pattern.matcher("    0615").matches());
        Assert.assertFalse(pattern.matcher("0615").matches());
        Assert.assertFalse(pattern.matcher("15").matches());
        Assert.assertFalse(pattern.matcher("A").matches());
        Assert.assertFalse(pattern.matcher("20100615!").matches());
        Assert.assertFalse(pattern.matcher("17000615").matches());
        Assert.assertFalse(pattern.matcher("20101315").matches());
        Assert.assertFalse(pattern.matcher("20100632").matches());
    }

    @Test
    public void testCreateNaaccrIdFromItemName() {
        Assert.assertEquals("", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(""));
        Assert.assertEquals("testTestTest", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("Test Test Test"));
        Assert.assertEquals("testSomeThingElse123", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("test: (ignored);   some_thing # else --123!!!"));

        Assert.assertEquals("phase1NumberOfFractions", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("Phase I Number of Fractions"));
        Assert.assertEquals("lnHeadAndNeckLevels6To7", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("LN Head and Neck Levels VI-VII"));
        Assert.assertEquals("headNeck", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("Head&Neck"));
    }

    @Test
    public void testGetMergedDictionaries() {
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.getMergedDictionaries(NaaccrFormat.NAACCR_VERSION_160));
    }

    @Test
    public void testStandardDictionaries() throws IOException {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            Path path1 = Paths.get(TestingUtils.getWorkingDirectory() + "/src/main/resources/naaccr-dictionary-" + version + ".xml");
            Path path2 = Paths.get("build/tmp-dictionary-" + version + ".xml");
            NaaccrXmlDictionaryUtils.writeDictionary(NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(version), path2.toFile());
            if (!TestingUtils.readFileAsOneString(path1.toFile()).replace("\r", "").equals(TestingUtils.readFileAsOneString(path2.toFile()).replace("\r", "")))
                Assert.fail("Dictionary for version " + version + " needs to be re-created, it contains differences from what would be created by the library!");

            path1 = Paths.get(TestingUtils.getWorkingDirectory() + "/src/main/resources/user-defined-naaccr-dictionary-" + version + ".xml");
            path2 = Paths.get("build/tmp-dictionary-" + version + ".xml");
            NaaccrXmlDictionaryUtils.writeDictionary(NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(version), path2.toFile());
            if (!TestingUtils.readFileAsOneString(path1.toFile()).replace("\r", "").equals(TestingUtils.readFileAsOneString(path2.toFile()).replace("\r", "")))
                Assert.fail("User dictionary for version " + version + " needs to be re-created, it contains differences from what would be created by the library!");
        }
    }
}
