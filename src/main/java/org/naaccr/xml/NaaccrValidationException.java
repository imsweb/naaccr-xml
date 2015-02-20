/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

public class NaaccrValidationException extends Exception {

    protected Integer _lineNumber;
    
    protected String _path;
    
    public NaaccrValidationException(String message) {
        super(message);
    }

    public Integer getLineNumber() {
        return _lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        _lineNumber = lineNumber;
    }

    public void setPath(String path) {
        _path = path;
    }
    
    public String getPath() {
        return _path;
    }
    
}
