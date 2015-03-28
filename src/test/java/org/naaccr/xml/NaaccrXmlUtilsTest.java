/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class NaaccrXmlUtilsTest {

    @Test
    public void testGetFormatFromXmlFile() {
        
        // all the attributes on one line
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/validation-test-1.xml");
        Assert.assertEquals(NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, NaaccrXmlUtils.getFormatFromXmlFile(file));

        // all the attributes on several lines
        file = new File(System.getProperty("user.dir") + "/src/test/resources/validation-test-2.xml");
        Assert.assertEquals(NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, NaaccrXmlUtils.getFormatFromXmlFile(file));
    }
}
