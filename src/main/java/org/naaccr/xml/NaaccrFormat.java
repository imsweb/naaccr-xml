/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A format encapsulates a NAACCR version and a record type. It also makes the flat-file line length available based on those two fields.
 */
public class NaaccrFormat {

    // version constants
    public static String NAACCR_VERSION_150 = "150";
    public static String NAACCR_VERSION_140 = "140";

    // list of supported versions
    private static final List<String> _SUPPORTED_VERSIONS = new ArrayList<>();

    static {
        _SUPPORTED_VERSIONS.add(NAACCR_VERSION_150);
        _SUPPORTED_VERSIONS.add(NAACCR_VERSION_140);
    }

    public static boolean isVersionSupported(String version) {
        return _SUPPORTED_VERSIONS.contains(version);
    }

    public static Set<String> getSupportedVersions() {
        return new HashSet<>(_SUPPORTED_VERSIONS);
    }

    // format constants
    public static String NAACCR_FORMAT_15_ABSTRACT = "naaccr-150-abstract";
    public static String NAACCR_FORMAT_15_MODIFIED = "naaccr-150-modified";
    public static String NAACCR_FORMAT_15_CONFIDENTIAL = "naaccr-150-confidential";
    public static String NAACCR_FORMAT_15_INCIDENCE = "naaccr-150-incidence";
    public static String NAACCR_FORMAT_14_ABSTRACT = "naaccr-140-abstract";
    public static String NAACCR_FORMAT_14_MODIFIED = "naaccr-140-modified";
    public static String NAACCR_FORMAT_14_CONFIDENTIAL = "naaccr-140-confidential";
    public static String NAACCR_FORMAT_14_INCIDENCE = "naaccr-140-incidence";

    // list of supported formats
    private static final List<String> _SUPPORTED_FORMATS = new ArrayList<>();

    static {
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
    public static String NAACCR_REC_TYPE_ABSTRACT = "A";
    public static String NAACCR_REC_TYPE_MODIFIED = "M";
    public static String NAACCR_REC_TYPE_CONFIDENTIAL = "C";
    public static String NAACCR_REC_TYPE_INCIDENCE = "I";

    // list of supported record types
    private static final List<String> _SUPPORTED_REC_TYPES = new ArrayList<>();

    static {
        _SUPPORTED_REC_TYPES.add(NAACCR_REC_TYPE_ABSTRACT);
        _SUPPORTED_REC_TYPES.add(NAACCR_REC_TYPE_MODIFIED);
        _SUPPORTED_REC_TYPES.add(NAACCR_REC_TYPE_CONFIDENTIAL);
        _SUPPORTED_REC_TYPES.add(NAACCR_REC_TYPE_INCIDENCE);
    }

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

    private String _naaccrVersion;

    private String _recordType;

    private int _lineLength;

    private NaaccrFormat(String format) {
        if (!isFormatSupported(format))
            throw new RuntimeException("Unsupported format: " + format);

        String[] parts = format.split("\\-");
        if (!isVersionSupported(parts[1]))
            throw new RuntimeException("Unsupported version: " + parts[1]);
        _naaccrVersion = parts[1];

        switch (parts[2]) {
            case "abstract":
                _recordType = "A";
                _lineLength = 22824; // this will have to change when we start supporting more formats
                break;
            case "modified":
                _recordType = "M";
                _lineLength = 22824;
                break;
            case "confidential":
                _recordType = "C";
                _lineLength = 5564;
                break;
            case "incidence":
                _recordType = "I";
                _lineLength = 3339;
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
