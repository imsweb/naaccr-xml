/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Dates are tricky to deal with so I am adding an extra test just for them...
 */
public class DateConversionTest {

    @Test
    public void testDateFormat() {
        // following examples are from http://books.xmlschemata.org/relaxng/ch19-77049.html

        assertValidDateValue("2001-10-26T21:32:52");
        assertValidDateValue("2001-10-26T21:32:52+02:00");
        assertValidDateValue("2001-10-26T19:32:52Z");
        assertValidDateValue("2001-10-26T19:32:52+00:00");
        assertValidDateValue("-2001-10-26T21:32:52");
        assertValidDateValue("2001-10-26T21:32:52.12679");

        assertInvalidDateValue("2001-10-26");
        assertInvalidDateValue("2001-10-26T21:32");
        assertInvalidDateValue("2001-10-26T25:32:52+02:00");
        assertInvalidDateValue("01-10-26T21:32");
        assertInvalidDateValue("2001-10-26T21:32:52+2:00");
    }

    private void assertValidDateValue(String dateValue) {
        try {
            ZonedDateTime.parse(dateValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        catch (RuntimeException e1) {
            try {
                LocalDateTime.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            catch (RuntimeException e2) {
                Assert.fail("Value should be valid, but isn't: " + dateValue);
            }
        }
    }

    private void assertInvalidDateValue(String dateValue) {
        try {
            ZonedDateTime.parse(dateValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            LocalDateTime.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        catch (RuntimeException e) {
            return;
        }
        Assert.fail("Value should be invalid, but isn't: " + dateValue);
    }
}
