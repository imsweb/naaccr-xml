/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;

public class NaaccrDictionaryUtilsTest {

    @Test
    public void testReadDictionary() throws IOException {

        // get a base dictionary
        NaaccrDictionary baseDictionary1 = NaaccrDictionaryUtils.getBaseDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_140);
        NaaccrDictionary baseDictionary2 = NaaccrDictionaryUtils.getBaseDictionaryByUri(baseDictionary1.getDictionaryUri());
        Assert.assertEquals(baseDictionary1.getDictionaryUri(), baseDictionary2.getDictionaryUri());
        Assert.assertEquals(baseDictionary1.getNaaccrVersion(), baseDictionary2.getNaaccrVersion());
        Assert.assertEquals(baseDictionary1.getItems().size(), baseDictionary2.getItems().size());

        // get a default user dictionary
        NaaccrDictionary defaultUserDictionary1 = NaaccrDictionaryUtils.getDefaultUserDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_140);
        NaaccrDictionary defaultUserDictionary2 = NaaccrDictionaryUtils.getDefaultUserDictionaryByUri(baseDictionary1.getDictionaryUri());
        Assert.assertEquals(defaultUserDictionary1.getDictionaryUri(), defaultUserDictionary2.getDictionaryUri());
        Assert.assertEquals(defaultUserDictionary1.getNaaccrVersion(), defaultUserDictionary2.getNaaccrVersion());
        Assert.assertEquals(defaultUserDictionary1.getItems().size(), defaultUserDictionary2.getItems().size());

        // read a provided user dictionary
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("testing-user-dictionary-140.xml"))) {
            NaaccrDictionary defaultUserDictionary = NaaccrDictionaryUtils.readDictionary(reader);
            Assert.assertEquals(3, defaultUserDictionary.getItems().size());
        }

        // try to read a user dictionary with an error (bad start column)
        boolean exceptionHappend = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("testing-user-dictionary-140-bad1.xml"))) {
            NaaccrDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exceptionHappend = true;
        }
        Assert.assertTrue(exceptionHappend);
    }

    @Test
    public void testValidationRegex() {

        //  "alpha": uppercase letters, A-Z, no spaces, full length needs to be filled in
        Pattern pattern = NaaccrDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(NaaccrDictionaryUtils.NAACCR_DATA_TYPE_ALPHA);
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
        pattern = NaaccrDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(NaaccrDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
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
        pattern = NaaccrDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(NaaccrDictionaryUtils.NAACCR_DATA_TYPE_MIXED);
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
        pattern = NaaccrDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(NaaccrDictionaryUtils.NAACCR_DATA_TYPE_NUMERIC);
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
        pattern = NaaccrDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(NaaccrDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
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
        pattern = NaaccrDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(NaaccrDictionaryUtils.NAACCR_DATA_TYPE_DATE);
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
    
}
