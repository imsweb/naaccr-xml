/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab.extension;

public class OverallSummary {

    private Summary _patientSummary;
    private Summary _tumorSummary;

    public Summary getPatientSummary() {
        return _patientSummary;
    }

    public void setPatientSummary(Summary patientSummary) {
        _patientSummary = patientSummary;
    }

    public Summary getTumorSummary() {
        return _tumorSummary;
    }

    public void setTumorSummary(Summary tumorSummary) {
        _tumorSummary = tumorSummary;
    }
}
