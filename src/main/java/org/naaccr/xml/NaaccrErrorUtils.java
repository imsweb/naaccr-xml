/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The purpose of this class is to centralize the error messages.
 */
public class NaaccrErrorUtils {

    // the possible validation errors
    public static final String CODE_BAD_NAACCR_ID = "BAD_NAACCR_ID";
    public static final String CODE_BAD_NAACCR_NUM = "BAD_NAACCR_NUM";
    public static final String CODE_VAL_TOO_LONG = "VALUE_TOO_LONG";
    public static final String CODE_VAL_TOO_SHORT = "VALUE_TOO_SHORT";
    public static final String CODE_VAL_DATA_TYPE = "VALUE_BAD_FOR_TYPE";
    public static final String CODE_VAL_REGEX = "VALUE_BAD_FOR_REGEX";
    public static final String CODE_VAL_PAT_VS_TUM = "PAT_VS_TUM_LEVEL";

    // data structure that holds the corresponding error messages (using a linked hash map to keep the original order)
    private static final Map<String, String> _MESSAGES = new LinkedHashMap<>();

    static {
        _MESSAGES.put(CODE_BAD_NAACCR_ID, "unknown NAACCR ID: ${0}");
        _MESSAGES.put(CODE_BAD_NAACCR_NUM, "NAACCR Number '${0}' does not correspond to NAACCR ID '${1}'");
        _MESSAGES.put(CODE_VAL_TOO_LONG, "value too long, expected at most ${0} character(s) but got ${1}");
        _MESSAGES.put(CODE_VAL_TOO_SHORT, "value too short, expected exactly ${0} character(s) but got ${1}");
        _MESSAGES.put(CODE_VAL_DATA_TYPE, "invalid value according to data type '${0}'");
        _MESSAGES.put(CODE_VAL_REGEX, "invalid value according to regular expression '${0}'");
        _MESSAGES.put(CODE_VAL_PAT_VS_TUM, "item '${0}' is a patient-level item but has different values for some of the tumors");
    }

    /**
     * Returns the error message for the given error code
     * @param code error code
     * @param msgValues optional values to plug into the message
     * @return the corresponding error message, never null (will throw an runtime exception if unknown code)
     */
    public static String getValidationError(String code, Object... msgValues) {
        if (!_MESSAGES.containsKey(code))
            throw new RuntimeException("Unknown code: " + code);
        return fillMessage(_MESSAGES.get(code), msgValues);
    }

    /**
     * Returns all the available error codes and messages.
     * @return a map of codes and messages, never null
     */
    public static Map<String, String> getAllValidationErrors() {
        return Collections.unmodifiableMap(_MESSAGES);
    }

    private static String fillMessage(String msg, Object... values) {
        String result = msg;
        if (values != null)
            for (int i = 0; i < values.length; i++)
                result = result.replace("${" + i + "}", values[i] == null ? "{blank}" : values[i].toString());
        return result;

    }
}
