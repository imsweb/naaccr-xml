/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

public class NaaccrValidationError {
    
    protected String _code;

    protected String _message;

    protected Integer _lineNumber;

    protected String _path;
    
    protected String _naaccrId;
    
    protected Integer _naaccrNum;
    
    protected String _value;
    
    public NaaccrValidationError(String code, Object... msgValues) {
        _code = code;
        _message = NaaccrErrorUtils.getValidationError(code, msgValues);
    }

    public String getCode() {
        return _code;
    }

    public void setCode(String code) {
        _code = code;
    }

    public String getMessage() {
        return _message;
    }

    public void setMessage(String message) {
        throw new RuntimeException("Forbidden method, use the constructor along with the NaaccrValidationErrorUtils class!");
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
