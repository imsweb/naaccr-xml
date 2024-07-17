/*
 * Copyright (C) 2019 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import org.junit.Assert;
import org.junit.Test;

import static com.imsweb.naaccrxml.NaaccrFormat.NAACCR_REC_TYPE_ABSTRACT;
import static com.imsweb.naaccrxml.NaaccrFormat.NAACCR_REC_TYPE_CONFIDENTIAL;
import static com.imsweb.naaccrxml.NaaccrFormat.NAACCR_REC_TYPE_INCIDENCE;
import static com.imsweb.naaccrxml.NaaccrFormat.NAACCR_REC_TYPE_MODIFIED;
import static com.imsweb.naaccrxml.NaaccrFormat.NAACCR_VERSION_160;
import static com.imsweb.naaccrxml.NaaccrFormat.NAACCR_VERSION_180;
import static com.imsweb.naaccrxml.NaaccrFormat.NAACCR_VERSION_210;
import static com.imsweb.naaccrxml.NaaccrFormat.NAACCR_VERSION_LATEST;

public class NaaccrFormatTest {

    @Test
    public void testFormats() {
        Assert.assertFalse(NaaccrFormat.getSupportedFormats().isEmpty());
        Assert.assertFalse(NaaccrFormat.getSupportedVersions().isEmpty());
        Assert.assertFalse(NaaccrFormat.getSupportedRecordTypes().isEmpty());
        Assert.assertTrue(NaaccrFormat.getSupportedVersions().contains(NaaccrFormat.NAACCR_VERSION_LATEST));
    }

    @Test
    public void testLineLength() {
        Assert.assertEquals(-1, NaaccrFormat.getInstance(NAACCR_VERSION_210, NAACCR_REC_TYPE_ABSTRACT).getLineLength());
        Assert.assertEquals(-1, NaaccrFormat.getInstance(NAACCR_VERSION_210, NAACCR_REC_TYPE_MODIFIED).getLineLength());
        Assert.assertEquals(-1, NaaccrFormat.getInstance(NAACCR_VERSION_210, NAACCR_REC_TYPE_CONFIDENTIAL).getLineLength());
        Assert.assertEquals(-1, NaaccrFormat.getInstance(NAACCR_VERSION_210, NAACCR_REC_TYPE_INCIDENCE).getLineLength());

        Assert.assertEquals(24194, NaaccrFormat.getInstance(NAACCR_VERSION_180, NAACCR_REC_TYPE_ABSTRACT).getLineLength());
        Assert.assertEquals(24194, NaaccrFormat.getInstance(NAACCR_VERSION_180, NAACCR_REC_TYPE_MODIFIED).getLineLength());
        Assert.assertEquals(6154, NaaccrFormat.getInstance(NAACCR_VERSION_180, NAACCR_REC_TYPE_CONFIDENTIAL).getLineLength());
        Assert.assertEquals(4048, NaaccrFormat.getInstance(NAACCR_VERSION_180, NAACCR_REC_TYPE_INCIDENCE).getLineLength());

        Assert.assertEquals(22824, NaaccrFormat.getInstance(NAACCR_VERSION_160, NAACCR_REC_TYPE_ABSTRACT).getLineLength());
        Assert.assertEquals(22824, NaaccrFormat.getInstance(NAACCR_VERSION_160, NAACCR_REC_TYPE_MODIFIED).getLineLength());
        Assert.assertEquals(5564, NaaccrFormat.getInstance(NAACCR_VERSION_160, NAACCR_REC_TYPE_CONFIDENTIAL).getLineLength());
        Assert.assertEquals(3339, NaaccrFormat.getInstance(NAACCR_VERSION_160, NAACCR_REC_TYPE_INCIDENCE).getLineLength());
    }
    
    @Test
    public void testGetDisplayName() {
        Assert.assertNotNull(NaaccrFormat.getInstance(NAACCR_VERSION_LATEST, NAACCR_REC_TYPE_ABSTRACT).getDisplayName());
    }
}
