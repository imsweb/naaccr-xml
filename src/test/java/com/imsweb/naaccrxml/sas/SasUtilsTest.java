/*
 * Copyright (C) 2020 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        Assert.assertEquals("test.csv", SasUtils.computeCsvPathFromXmlPath("test.zip"));

        Assert.assertEquals("TEST.csv", SasUtils.computeCsvPathFromXmlPath("TEST.XML"));
        Assert.assertEquals("TEST.csv", SasUtils.computeCsvPathFromXmlPath("TEST.XML.GZ"));
        Assert.assertEquals("TEST.SOMETHING.csv", SasUtils.computeCsvPathFromXmlPath("TEST.SOMETHING.GZ"));
        Assert.assertEquals("TEST.SOMETHING.csv", SasUtils.computeCsvPathFromXmlPath("TEST.SOMETHING"));
        Assert.assertEquals("TEST.XML.TEST.csv", SasUtils.computeCsvPathFromXmlPath("TEST.XML.TEST.XML"));
        Assert.assertEquals("TEST.csv", SasUtils.computeCsvPathFromXmlPath("TEST.ZIP"));
    }

    @Test
    public void testValidateCsvDictionary() throws IOException {
        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-140.csv"));
        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-150.csv"));
        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-160.csv"));
        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-180.csv"));

        SasUtils.validateCsvDictionary(new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/sas/epath/path-text-dictionary.csv"));
    }

    @Test
    public void testCleanUpValueToWriteAsXml() {
        Assert.assertEquals("Evaluate", SasUtils.cleanUpValueToWriteAsXml("Evaluate"));
        Assert.assertEquals("Evaluate for non-Hodgkin&apos;s lymphoma", SasUtils.cleanUpValueToWriteAsXml("Evaluate for non-Hodgkin's lymphoma"));
        Assert.assertEquals("&apos;Evaluate for non-Hodgkin&apos;s lymphoma&apos;", SasUtils.cleanUpValueToWriteAsXml("'Evaluate for non-Hodgkin's lymphoma'"));
        Assert.assertEquals("Evaluate for non-Hodgkin&apos;s lymphoma &amp; something", SasUtils.cleanUpValueToWriteAsXml("Evaluate for non-Hodgkin's lymphoma & something"));
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testExtractRequestedFields() throws IOException {
        File file = new File(TestingUtils.getBuildDirectory(), "included-items.csv");

        List<SasFieldInfo> availableFields = new ArrayList<>();
        availableFields.add(new SasFieldInfo("field1", "field1", "Tumor", 1, 1, "Field 1", null));
        availableFields.add(new SasFieldInfo("field2", "field2", "Tumor", 1, 1, "Field 2", null));

        try {
            TestingUtils.writeFile(file, "HEADER\nfield1\nfield2");
            Set<String> result = SasUtils.extractRequestedFields(file.getPath(), availableFields);
            Assert.assertEquals(2, result.size());
            Assert.assertTrue(result.contains("field1"));
            Assert.assertTrue(result.contains("field2"));

            TestingUtils.writeFile(file, "\"HEADER\"\n\"field1\"\n\"field2\"");
            result = SasUtils.extractRequestedFields(file.getPath(), availableFields);
            Assert.assertEquals(2, result.size());
            Assert.assertTrue(result.contains("field1"));
            Assert.assertTrue(result.contains("field2"));

            result = SasUtils.extractRequestedFields("field1,field2", availableFields);
            Assert.assertEquals(2, result.size());
            Assert.assertTrue(result.contains("field1"));
            Assert.assertTrue(result.contains("field2"));

            result = SasUtils.extractRequestedFields("field1, field2", availableFields);
            Assert.assertEquals(2, result.size());
            Assert.assertTrue(result.contains("field1"));
            Assert.assertTrue(result.contains("field2"));

            result = SasUtils.extractRequestedFields("field1", availableFields);
            Assert.assertEquals(1, result.size());
            Assert.assertTrue(result.contains("field1"));

            result = SasUtils.extractRequestedFields("field1,unknown", availableFields);
            Assert.assertEquals(1, result.size());
            Assert.assertTrue(result.contains("field1"));
        }
        finally {
            file.delete();
        }
    }

    @Test
    public void testRightPadWithSpaces() {
        Assert.assertEquals("X  ", SasUtils.rightPadWithSpaces("X", 3));
        Assert.assertEquals("X", SasUtils.rightPadWithSpaces("XYZ", 1));
    }
}
