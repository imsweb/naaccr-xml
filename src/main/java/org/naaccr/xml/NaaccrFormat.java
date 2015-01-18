/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * A format encapsulates a NAACCR version and a record type.
 */
public class NaaccrFormat {

    // format constants
    public static String NAACCR_FORMAT_14_ABSTRACT = "naaccr-14-abstract";
    public static String NAACCR_FORMAT_14_MODIFIED = "naaccr-14-modified";
    public static String NAACCR_FORMAT_14_CONFIDENTIAL = "naaccr-14-confidential";
    public static String NAACCR_FORMAT_14_INCIDENCE = "naaccr-14-incidence";

    // list of supported formats
    private static final List<String> _SUPPORTED_FORMATS = new ArrayList<>();

    static {
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_14_ABSTRACT);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_14_MODIFIED);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_14_CONFIDENTIAL);
        _SUPPORTED_FORMATS.add(NAACCR_FORMAT_14_INCIDENCE);
    }

    public static boolean isFormatSupported(String format) {
        return _SUPPORTED_FORMATS.contains(format);
    }

    private String _naaccrVersion;

    private String _recordType;
    
    private int _lineLength;

    public NaaccrFormat(String format) {
        if (!isFormatSupported(format))
            throw new RuntimeException("Unsupported format: " + format);

        String[] parts = format.split("\\-");
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
        return "NAACCR " + _naaccrVersion + " " + formattedType;
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
