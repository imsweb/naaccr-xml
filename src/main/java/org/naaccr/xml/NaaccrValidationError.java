/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

public class NaaccrValidationError {

    protected String _message;

    protected Integer _lineNumber;

    protected String _path;
    
    protected String _naaccrId;
    
    protected Integer _naaccrNum;
    
    protected String _value;

    public String getMessage() {
        return _message;
    }

    public void setMessage(String message) {
        _message = message;
    }

    public Integer getLineNumber() {
        return _lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        _lineNumber = lineNumber;
    }

    public String getPath() {
        return _path;
    }

    public void setPath(String path) {
        _path = path;
    }

    public String getNaaccrId() {
        return _naaccrId;
    }

    public void setNaaccrId(String naaccrId) {
        _naaccrId = naaccrId;
    }

    public Integer getNaaccrNum() {
        return _naaccrNum;
    }

    public void setNaaccrNum(Integer naaccrNum) {
        _naaccrNum = naaccrNum;
    }

    public String getValue() {
        return _value;
    }

    public void setValue(String value) {
        _value = value;
    }
}
