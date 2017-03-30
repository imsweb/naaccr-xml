/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import org.junit.Assert;
import org.junit.Test;

public class NaaccrPatientConverterTest {

    @Test
    public void testCarriageReturnPattern() {
        Assert.assertEquals("", NaaccrPatientConverter._CARRIAGE_RETURN_PATTERN.matcher("").replaceAll("?"));
        Assert.assertEquals("?", NaaccrPatientConverter._CARRIAGE_RETURN_PATTERN.matcher("\n").replaceAll("?"));
        Assert.assertEquals("?", NaaccrPatientConverter._CARRIAGE_RETURN_PATTERN.matcher("\r").replaceAll("?"));
        Assert.assertEquals("?", NaaccrPatientConverter._CARRIAGE_RETURN_PATTERN.matcher("\r\n").replaceAll("?"));
        Assert.assertEquals("????", NaaccrPatientConverter._CARRIAGE_RETURN_PATTERN.matcher("\r\n\r\r\r\n").replaceAll("?"));
        Assert.assertEquals("???", NaaccrPatientConverter._CARRIAGE_RETURN_PATTERN.matcher("\n\r\r").replaceAll("?"));
    }

    @Test
    public void testControlCharactersPattern() {
        Assert.assertEquals("", NaaccrPatientConverter._CONTROL_CHARACTERS_PATTERN.matcher("").replaceAll("?"));
        Assert.assertEquals("abc123", NaaccrPatientConverter._CONTROL_CHARACTERS_PATTERN.matcher("abc123").replaceAll("?"));
        Assert.assertEquals("??", NaaccrPatientConverter._CONTROL_CHARACTERS_PATTERN.matcher("\u0000\u001F").replaceAll("?"));
        Assert.assertEquals("\t", NaaccrPatientConverter._CONTROL_CHARACTERS_PATTERN.matcher("\u0009").replaceAll("?"));
    }
}
