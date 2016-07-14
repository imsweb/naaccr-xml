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

    /**
     * Compare the two provided specifications. Result is undefined if any of them is not supported.
     * @param spec1 first version to compare
     * @param spec2 second version to compare
     * @return negative integer if first version is smaller, 0 if they are the same, positive integer otherwise
     */
    public static int compareVersions(String spec1, String spec2) {
        String[] version1 = spec1.split("\\.");
        Integer major1 = Integer.valueOf(version1[0]);
        Integer minor1 = Integer.valueOf(version1[1]);

        String[] version2 = spec2.split("\\.");
        Integer major2 = Integer.valueOf(version2[0]);
        Integer minor2 = Integer.valueOf(version2[1]);
        
        if (major1.equals(major2))
            return minor1.compareTo(minor2);
        return major1.compareTo(major2);
    }
}
