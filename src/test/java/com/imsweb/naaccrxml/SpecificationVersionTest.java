/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class SpecificationVersionTest {

    @Test
    public void testIsSpecificationSupported() {
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.0"));
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.1"));
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.2"));
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.3"));
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.4"));
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.5"));
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.6"));
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.7"));
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.8"));

        Assert.assertFalse(SpecificationVersion.isSpecificationSupported("1.9"));
        Assert.assertFalse(SpecificationVersion.isSpecificationSupported("2.2"));

        Assert.assertFalse(SpecificationVersion.isSpecificationSupported(null));
        Assert.assertFalse(SpecificationVersion.isSpecificationSupported(""));
        Assert.assertFalse(SpecificationVersion.isSpecificationSupported(" "));
        Assert.assertFalse(SpecificationVersion.isSpecificationSupported("hum?"));
    }

    @Test
    public void testCompareVersions() {
        Assert.assertEquals(0, SpecificationVersion.compareSpecifications("1.0", "1.0"));
        Assert.assertEquals(0, SpecificationVersion.compareSpecifications("2.0", "2.0"));
        Assert.assertEquals(-1, SpecificationVersion.compareSpecifications("1.0", "2.0"));
        Assert.assertEquals(1, SpecificationVersion.compareSpecifications("2.0", "1.0"));
        Assert.assertEquals(-1, SpecificationVersion.compareSpecifications("2.0", "2.1"));
        Assert.assertEquals(1, SpecificationVersion.compareSpecifications("2.1", "2.0"));
        Assert.assertEquals(-1, SpecificationVersion.compareSpecifications("1.0", "2.1"));
        Assert.assertEquals(1, SpecificationVersion.compareSpecifications("2.1", "1.0"));
        Assert.assertEquals(-1, SpecificationVersion.compareSpecifications("1.2", "2.1"));
        Assert.assertEquals(1, SpecificationVersion.compareSpecifications("2.1", "1.2"));

        Assert.assertEquals(2, SpecificationVersion.compareSpecifications("1.0", "1"));
        Assert.assertEquals(-2, SpecificationVersion.compareSpecifications("1", "1.0"));
        Assert.assertEquals(0, SpecificationVersion.compareSpecifications("1", "1"));
    }

    @Test
    public void testReadmeFile() throws IOException {
        String content = TestingUtils.readFileAsOneString(new File(TestingUtils.getWorkingDirectory(), "README.md"));
        if (!content.contains("It implements version " + NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION))
            Assert.fail("README.md references on old specification!");
    }
}
