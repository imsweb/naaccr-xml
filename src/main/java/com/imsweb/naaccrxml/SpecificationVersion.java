/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import org.apache.commons.lang3.StringUtils;

public class SpecificationVersion {

    public static final String SPEC_1_6 = "1.6";
    public static final String SPEC_1_5 = "1.5";
    public static final String SPEC_1_4 = "1.4";
    public static final String SPEC_1_3 = "1.3";
    public static final String SPEC_1_2 = "1.2";
    public static final String SPEC_1_1 = "1.1";
    public static final String SPEC_1_0 = "1.0";

    /**
     * Constructor.
     */
    private SpecificationVersion() {
        // no creation of objects for this class...
    }

    /**
     * Returns true if the provided spec is supported by this library, false otherwise.
     */
    public static boolean isSpecificationSupported(String spec) {
        return SPEC_1_0.equals(spec) || SPEC_1_1.equals(spec) || SPEC_1_2.equals(spec) || SPEC_1_3.equals(spec) || SPEC_1_4.equals(spec) || SPEC_1_5.equals(spec) || SPEC_1_6.equals(spec);
    }

    /**
     * Compare the two provided specifications. Result is undefined if any of them is not supported.
     * @param spec1 first version to compare
     * @param spec2 second version to compare
     * @return negative integer if first version is smaller, 0 if they are the same, positive integer otherwise
     */
    public static int compareSpecifications(String spec1, String spec2) {
        String[] parts1 = StringUtils.split(spec1, '.');
        Integer major1 = Integer.valueOf(parts1[0]);
        Integer minor1 = Integer.valueOf(parts1[1]);

        String[] parts2 = StringUtils.split(spec2, '.');
        Integer major2 = Integer.valueOf(parts2[0]);
        Integer minor2 = Integer.valueOf(parts2[1]);

        if (major1.equals(major2))
            return minor1.compareTo(minor2);
        return major1.compareTo(major2);
    }
}
