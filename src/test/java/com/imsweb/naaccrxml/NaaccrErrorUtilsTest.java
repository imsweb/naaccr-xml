/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import org.junit.Assert;
import org.junit.Test;

public class NaaccrErrorUtilsTest {

    @Test
    public void testGetValidationError() {
        Assert.assertEquals("unknown NAACCR ID: ${0}", NaaccrErrorUtils.getValidationError(NaaccrErrorUtils.CODE_BAD_NAACCR_ID));
        Assert.assertEquals("unknown NAACCR ID: {blank}", NaaccrErrorUtils.getValidationError(NaaccrErrorUtils.CODE_BAD_NAACCR_ID, (String)null));
        Assert.assertEquals("unknown NAACCR ID: ", NaaccrErrorUtils.getValidationError(NaaccrErrorUtils.CODE_BAD_NAACCR_ID, ""));
        Assert.assertEquals("unknown NAACCR ID: xyz", NaaccrErrorUtils.getValidationError(NaaccrErrorUtils.CODE_BAD_NAACCR_ID, "xyz"));
    }
    
}
