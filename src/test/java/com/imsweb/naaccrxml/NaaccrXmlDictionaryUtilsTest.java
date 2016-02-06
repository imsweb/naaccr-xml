/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Pattern;

public class NaaccrXmlDictionaryUtilsTest {

    @Test
    public void testReadDictionary() throws IOException {

        // get a base dictionary
        NaaccrDictionary baseDictionary1 = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_140);
        NaaccrDictionary baseDictionary2 = NaaccrXmlDictionaryUtils.getBaseDictionaryByUri(baseDictionary1.getDictionaryUri());
        Assert.assertEquals(baseDictionary1.getDictionaryUri(), baseDictionary2.getDictionaryUri());
        Assert.assertEquals(baseDictionary1.getNaaccrVersion(), baseDictionary2.getNaaccrVersion());
        Assert.assertEquals(baseDictionary1.getItems().size(), baseDictionary2.getItems().size());

        // get a default user dictionary
        NaaccrDictionary defaultUserDictionary1 = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_140);
        NaaccrDictionary defaultUserDictionary2 = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByUri(baseDictionary1.getDictionaryUri());
        Assert.assertEquals(defaultUserDictionary1.getDictionaryUri(), defaultUserDictionary2.getDictionaryUri());
        Assert.assertEquals(defaultUserDictionary1.getNaaccrVersion(), defaultUserDictionary2.getNaaccrVersion());
        Assert.assertEquals(defaultUserDictionary1.getItems().size(), defaultUserDictionary2.getItems().size());

        // read a provided user dictionary
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/testing-user-dictionary-140.xml"))) {
            NaaccrDictionary defaultUserDictionary = NaaccrXmlDictionaryUtils.readDictionary(reader);
            Assert.assertEquals(3, defaultUserDictionary.getItems().size());
        }

        // try to read a user dictionary with an error (bad start column)
        boolean exceptionHappend = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/testing-user-dictionary-140-bad1.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exceptionHappend = true;
        }
        Assert.assertTrue(exceptionHappend);
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
    }

    @Test
    public void testXsdAgainstLibrary() {

        // I can't validate the dictionary files against the XSD because it requires them to define a namespace, which they don't know right now...

        assertValidXmlFileForLibrary("naaccr-dictionary-140.xml");
        assertValidXmlFileForLibrary("user-defined-naaccr-dictionary-140.xml");
        //assertValidXmlFileForXsd("naaccr-dictionary-140.xml");
        //assertValidXmlFileForXsd("user-defined-naaccr-dictionary-140.xml");

        assertValidXmlFileForLibrary("naaccr-dictionary-150.xml");
        assertValidXmlFileForLibrary("user-defined-naaccr-dictionary-150.xml");
        //assertValidXmlFileForXsd("naaccr-dictionary-150.xml");
        //assertValidXmlFileForXsd("user-defined-naaccr-dictionary-150.xml");
    }

    /**
    private void assertValidXmlFileForXsd(String xmlFile) {
        try {
            URL schemaXsd = Thread.currentThread().getContextClassLoader().getResource("naaccr_dictionary.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaXsd);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(xmlFile)));
        }
        catch (Exception e) {
            Assert.fail("Was expected a valid file, but it was invalid: " + e.getMessage());
        }
    }
     */

    private void assertValidXmlFileForLibrary(String xmlFile) {
        File file = new File(System.getProperty("user.dir") + "/src/main/resources/" + xmlFile);
        try {
            NaaccrXmlDictionaryUtils.readDictionary(file);
        }
        catch (IOException e) {
            Assert.fail("Was expected a valid file, but it was invalid: " + e.getMessage());
        }
    }
}
