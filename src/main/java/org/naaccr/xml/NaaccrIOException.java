/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;

public class NaaccrIOException extends IOException {

    protected Integer _lineNumber;
    
    protected String _path;
    
    public NaaccrIOException(String message) {
        super(message);
    }

    public NaaccrIOException(String message, Integer lineNumber) {
        super(message);
        _lineNumber = lineNumber;
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
