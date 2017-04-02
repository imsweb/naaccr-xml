/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab.extension;

public class Summary {

    private Integer _numberRejected;
    private Integer _numberProcessed;
    private Integer _numberMatched;

    public Integer getNumberRejected() {
        return _numberRejected;
    }

    public void setNumberRejected(Integer numberRejected) {
        _numberRejected = numberRejected;
    }

    public Integer getNumberProcessed() {
        return _numberProcessed;
    }

    public void setNumberProcessed(Integer numberProcessed) {
        _numberProcessed = numberProcessed;
    }

    public Integer getNumberMatched() {
        return _numberMatched;
    }

    public void setNumberMatched(Integer numberMatched) {
        _numberMatched = numberMatched;
    }
}
