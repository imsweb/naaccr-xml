/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import org.junit.Assert;
import org.junit.Test;

public class SpecificationVersionTest {

    @Test
    public void testIsSpecificationSupported() {
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.0"));
        Assert.assertTrue(SpecificationVersion.isSpecificationSupported("1.1"));

        Assert.assertFalse(SpecificationVersion.isSpecificationSupported("1.2"));
        Assert.assertFalse(SpecificationVersion.isSpecificationSupported("2.2"));

        Assert.assertFalse(SpecificationVersion.isSpecificationSupported(null));
        Assert.assertFalse(SpecificationVersion.isSpecificationSupported(""));
        Assert.assertFalse(SpecificationVersion.isSpecificationSupported(" "));
        Assert.assertFalse(SpecificationVersion.isSpecificationSupported("hum?"));
    }
}
