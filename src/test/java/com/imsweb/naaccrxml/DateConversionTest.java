/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.text.ParseException;

import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Dates are tricky to deal with so I am adding an extra test just for them...
 */
public class DateConversionTest {

    @Test
    public void testDateFormat() throws ParseException {
        // following examples are from http://books.xmlschemata.org/relaxng/ch19-77049.html

        assertValidDateValue("2001-10-26T21:32:52");
        assertValidDateValue("2001-10-26T21:32:52+02:00");
        assertValidDateValue("2001-10-26T19:32:52Z");
        assertValidDateValue("2001-10-26T19:32:52+00:00");
        assertValidDateValue("-2001-10-26T21:32:52");
        assertValidDateValue("2001-10-26T21:32:52.12679");

        // weird, the link says that the following is invalid, but the Java framework seems to accept it...
        assertValidDateValue("2001-10-26");

        assertInvalidDateValue("2001-10-26T21:32");
        assertInvalidDateValue("2001-10-26T25:32:52+02:00");
        assertInvalidDateValue("01-10-26T21:32");
    }

    private void assertValidDateValue(String dateValue) {
        try {
            DatatypeConverter.parseDateTime(dateValue);
        }
        catch (IllegalArgumentException e) {
            Assert.fail("Value should be valid, but isn't: " + dateValue);
        }
    }

    private void assertInvalidDateValue(String dateValue) {
        try {
            DatatypeConverter.parseDateTime(dateValue);
        }
        catch (IllegalArgumentException e) {
            return;
        }
        Assert.fail("Value should be invalid, but isn't: " + dateValue);
    }
}
