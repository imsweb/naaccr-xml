/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class NaaccrDictionaryUtilsTest {

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
