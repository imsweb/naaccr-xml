/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

public class SpecificationVersion {

    /**
     * Constants for specification version 1.0
     */
    public static final String SPEC_1_0 = "1.0";

    /**
     * Constants for specification version 1.1
     */
    public static final String SPEC_1_1 = "1.1";

    /**
     * Returns true if the provided spec is supported by this library, false otherwise.
     */
    public static boolean isSpecificationSupported(String spec) {
        return SPEC_1_0.equals(spec) || SPEC_1_1.equals(spec);
    }
}
