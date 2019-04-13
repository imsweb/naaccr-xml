/*
 * Copyright (C) 2019 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import org.junit.Assert;
import org.junit.Test;

public class NaaccrFormatTest {

    @Test
    public void testFormats() {
        Assert.assertEquals(24194, NaaccrFormat.getInstance("180", "A").getLineLength());
        Assert.assertEquals(24194, NaaccrFormat.getInstance("180", "M").getLineLength());
        Assert.assertEquals(6154, NaaccrFormat.getInstance("180", "C").getLineLength());
        Assert.assertEquals(4048, NaaccrFormat.getInstance("180", "I").getLineLength());

        Assert.assertEquals(22824, NaaccrFormat.getInstance("160", "A").getLineLength());
        Assert.assertEquals(22824, NaaccrFormat.getInstance("160", "M").getLineLength());
        Assert.assertEquals(5564, NaaccrFormat.getInstance("160", "C").getLineLength());
        Assert.assertEquals(3339, NaaccrFormat.getInstance("160", "I").getLineLength());
    }
}
