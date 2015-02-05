/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

public class NaaccrValidationError {

    protected String _message;

    protected Integer _lineNumber;

    protected String _path;

    public NaaccrValidationError(String message, Integer lineNumber, String path) {
        _message = message;
        _lineNumber = lineNumber;
        _path = path;
    }
    
    public String getMessage() {
        return _message;
    }

    public Integer getLineNumber() {
        return _lineNumber;
    }

    public String getPath() {
        return _path;
    }
}
