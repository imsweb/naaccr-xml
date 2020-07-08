/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * A format encapsulates a NAACCR version and a record type. It also makes the flat-file line length available based on those two fields, for versions that support it.
 */
public final class NaaccrFormat {

    // version constants
    public static final String NAACCR_VERSION_210 = "210";
    public static final String NAACCR_VERSION_180 = "180";
    public static final String NAACCR_VERSION_160 = "160";
    public static final String NAACCR_VERSION_150 = "150";
    public static final String NAACCR_VERSION_140 = "140";

    // list of supported versions
    private static final List<String> _SUPPORTED_VERSIONS = new ArrayList<>();

    static {
        _SUPPORTED_VERSIONS.add(NAACCR_VERSION_210);
        _SUPPORTED_VERSIONS.add(NAACCR_VERSION_180);
        _SUPPORTED_VERSIONS.add(NAACCR_VERSION_160);
        _SUPPORTED_VERSIONS.add(NAACCR_VERSION_150);
        _SUPPORTED_VERSIONS.add(NAACCR_VERSION_140);
    }

    // "latest" version
    public static final String NAACCR_VERSION_LATEST = NAACCR_VERSION_180;

    public static boolean isVersionSupported(String version) {
        return _SUPPORTED_VERSIONS.contains(version);
    }

    public static Set<String> getSupportedVersions() {
        return new HashSet<>(_SUPPORTED_VERSIONS);
    }

    // format constants
    public static final String NAACCR_FORMAT_21_ABSTRACT = "naaccr-210-abstract";
    public static final String NAACCR_FORMAT_21_MODIFIED = "naaccr-210-modified";
    public static final String NAACCR_FORMAT_21_CONFIDENTIAL = "naaccr-210-confidential";
    public static final String NAACCR_FORMAT_21_INCIDENCE = "naaccr-210-incidence";
    public static final String NAACCR_FORMAT_18_ABSTRACT = "naaccr-180-abstract";
    public static final String NAACCR_FORMAT_18_MODIFIED = "naaccr-180-modified";
    public static final String NAACCR_FORMAT_18_CONFIDENTIAL = "naaccr-180-confidential";
    public static final String NAACCR_FORMAT_18_INCIDENCE = "naaccr-180-incidence";
    public static final String NAACCR_FORMAT_16_ABSTRACT = "naaccr-160-abstract";
    public static final String NAACCR_FORMAT_16_MODIFIED = "naaccr-160-modified";
    public static final String NAACCR_FORMAT_16_CONFIDENTIAL = "naaccr-160-confidential";
    public static final String NAACCR_FORMAT_16_INCIDENCE = "naaccr-160-incidence";
    public static final String NAACCR_FORMAT_15_ABSTRACT = "naaccr-150-abstract";
    public static final String NAACCR_FORMAT_15_MODIFIED = "naaccr-150-modified";
    public static final String NAACCR_FORMAT_15_CONFIDENTIAL = "naaccr-150-confidential";
    public static final String NAACCR_FORMAT_15_INCIDENCE = "naaccr-150-incidence";
    public static final String NAACCR_FORMAT_14_ABSTRACT = "naaccr-140-abstract";
    public static final String NAACCR_FORMAT_14_MODIFIED = "naaccr-140-modified";
    public static final String NAACCR_FORMAT_14_CONFIDENTIAL = "naaccr-140-confidential";
    public static final String NAACCR_FORMAT_14_INCIDENCE = "naaccr-140-incidence";

    // list of supported formats
    private static final List<String> _SUPPORTED_FORMATS = new ArrayList<>();

    static {
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_21_ABSTRACT);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_21_MODIFIED);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_21_CONFIDENTIAL);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_21_INCIDENCE);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_18_ABSTRACT);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_18_MODIFIED);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_18_CONFIDENTIAL);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_18_INCIDENCE);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_16_ABSTRACT);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_16_MODIFIED);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_16_CONFIDENTIAL);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_16_INCIDENCE);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_15_ABSTRACT);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_15_MODIFIED);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_15_CONFIDENTIAL);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_15_INCIDENCE);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_14_ABSTRACT);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_14_MODIFIED);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_14_CONFIDENTIAL);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_14_INCIDENCE);
    }

    public static boolean isFormatSupported(String format) {
        return _SUPPORTED_FORMATS.contains(format);
    }

    public static Set<String> getSupportedFormats() {
        return new HashSet<>(_SUPPORTED_FORMATS);
    }

    // record type constants
    public static final String NAACCR_REC_TYPE_ABSTRACT = "A";
    public static final String NAACCR_REC_TYPE_MODIFIED = "M";
    public static final String NAACCR_REC_TYPE_CONFIDENTIAL = "C";
    public static final String NAACCR_REC_TYPE_INCIDENCE = "I";

    // list of supported record types
    private static final List<String> _SUPPORTED_REC_TYPES = new ArrayList<>();

    static {
        _SUPPORTED_REC_TYPES.add(NAACCR_REC_TYPE_ABSTRACT);
        _SUPPORTED_REC_TYPES.add(NAACCR_REC_TYPE_MODIFIED);
        _SUPPORTED_REC_TYPES.add(NAACCR_REC_TYPE_CONFIDENTIAL);
        _SUPPORTED_REC_TYPES.add(NAACCR_REC_TYPE_INCIDENCE);
    }

    // default value if a record type is not provided
    public static final String ALL_RECORD_TYPES = "A,M,C,I";

    public static boolean isRecordTypeSupported(String recordType) {
        return _SUPPORTED_REC_TYPES.contains(recordType);
    }

    public static Set<String> getSupportedRecordTypes() {
        return new HashSet<>(_SUPPORTED_REC_TYPES);
    }

    public static NaaccrFormat getInstance(String format) {
        return new NaaccrFormat(format);
    }

    public static NaaccrFormat getInstance(String naaccrVersion, String recordType) {
        return new NaaccrFormat(getFormatFromVersionAndType(naaccrVersion, recordType));
    }

    private final String _naaccrVersion;

    private final String _recordType;

    private final int _lineLength;

    private NaaccrFormat(String format) {
        if (!isFormatSupported(format))
            throw new RuntimeException("Unsupported format: " + format);

        String[] parts = StringUtils.split(format, '-');
        if (!isVersionSupported(parts[1]))
            throw new RuntimeException("Unsupported version: " + parts[1]);
        _naaccrVersion = parts[1];

        // version 18 had a different line length than 16/15/14; post version 18 doesn't support length anymore...
        boolean isVersion18 = NAACCR_VERSION_180.equals(_naaccrVersion);
        boolean isPreVersion18 = NAACCR_VERSION_160.equals(_naaccrVersion) || NAACCR_VERSION_150.equals(_naaccrVersion) || NAACCR_VERSION_140.equals(_naaccrVersion);

        switch (parts[2]) {
            case "abstract":
                _recordType = "A";
                _lineLength = isPreVersion18 ? 22824 : isVersion18 ? 24194 : -1;
                break;
            case "modified":
                _recordType = "M";
                _lineLength = isPreVersion18 ? 22824 : isVersion18 ? 24194 : -1;
                break;
            case "confidential":
                _recordType = "C";
                _lineLength = isPreVersion18 ? 5564 : isVersion18 ? 6154 : -1;
                break;
            case "incidence":
                _recordType = "I";
                _lineLength = isPreVersion18 ? 3339 : isVersion18 ? 4048 : -1;
                break;
            default:
                throw new RuntimeException("Unsupported format: " + parts[2]);
        }
    }

    private static String getFormatFromVersionAndType(String version, String type) {
        String format;
        switch (type) {
            case "A":
                format = "naaccr-" + version + "-abstract";
                break;
            case "M":
                format = "naaccr-" + version + "-modified";
                break;
            case "C":
                format = "naaccr-" + version + "-confidential";
                break;
            case "I":
                format = "naaccr-" + version + "-incidence";
                break;
            default:
                format = null;
        }
        return format;
    }

    public String getNaaccrVersion() {
        return _naaccrVersion;
    }

    public String getRecordType() {
        return _recordType;
    }

    public int getLineLength() {
        return _lineLength;
    }

    @Override
    public String toString() {
        return getFormatFromVersionAndType(_naaccrVersion, _recordType);
    }

    public String getDisplayName() {
        String formattedType;
        switch (_recordType) {
            case "A":
                formattedType = "Abstract";
                break;
            case "M":
                formattedType = "Modified";
                break;
            case "C":
                formattedType = "Confidential";
                break;
            case "I":
                formattedType = "Incidence";
                break;
            default:
                formattedType = "?";
        }

        if ("0".equals(_naaccrVersion.substring(2)))
            return "NAACCR " + _naaccrVersion.substring(0, 2) + " " + formattedType;
        else
            return "NAACCR " + _naaccrVersion.substring(0, 2) + "." + _naaccrVersion.substring(2) + " " + formattedType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NaaccrFormat))
            return false;

        NaaccrFormat that = (NaaccrFormat)o;

        if (!_naaccrVersion.equals(that._naaccrVersion))
            return false;
        return _recordType.equals(that._recordType);

    }

    @Override
    public int hashCode() {
        int result = _naaccrVersion.hashCode();
        result = 31 * result + _recordType.hashCode();
        return result;
    }
}
