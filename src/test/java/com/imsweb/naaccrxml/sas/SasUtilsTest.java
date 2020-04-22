/*
 * Copyright (C) 2020 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.TestingUtils;

public class SasUtilsTest {

    @Test
    public void testComputeCsvPathFromXmlPath() {
        Assert.assertNull(SasUtils.computeCsvPathFromXmlPath(null));
        Assert.assertNull(SasUtils.computeCsvPathFromXmlPath(""));
        Assert.assertNull(SasUtils.computeCsvPathFromXmlPath(" "));

        Assert.assertEquals("test.csv", SasUtils.computeCsvPathFromXmlPath("test.xml"));
        Assert.assertEquals("test.csv", SasUtils.computeCsvPathFromXmlPath("test.xml.gz"));
        Assert.assertEquals("test.something.csv", SasUtils.computeCsvPathFromXmlPath("test.something.gz"));
        Assert.assertEquals("test.something.csv", SasUtils.computeCsvPathFromXmlPath("test.something"));
        Assert.assertEquals("test.xml.test.csv", SasUtils.computeCsvPathFromXmlPath("test.xml.test.xml"));

        Assert.assertEquals("TEST.csv", SasUtils.computeCsvPathFromXmlPath("TEST.XML"));
        Assert.assertEquals("TEST.csv", SasUtils.computeCsvPathFromXmlPath("TEST.XML.GZ"));
        Assert.assertEquals("TEST.SOMETHING.csv", SasUtils.computeCsvPathFromXmlPath("TEST.SOMETHING.GZ"));
        Assert.assertEquals("TEST.SOMETHING.csv", SasUtils.computeCsvPathFromXmlPath("TEST.SOMETHING"));
        Assert.assertEquals("TEST.XML.TEST.csv", SasUtils.computeCsvPathFromXmlPath("TEST.XML.TEST.XML"));
    }

    @Test
    public void testValidateCsvDictionary() throws IOException {
        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-140.csv"));
        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-150.csv"));
        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-160.csv"));
        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-180.csv"));

        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/sas/epath/path-text-dictionary.csv"));
    }

}
