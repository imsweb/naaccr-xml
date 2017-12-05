/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package demo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.PatientXmlReader;
import com.imsweb.naaccrxml.PatientXmlWriter;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;

public class ExtensionsDemo {

    public static void main(String[] args) throws IOException {

        // setup demo data (25 patients each with one tumor)
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
        // *** start of root extension
        OverallSummary summary = new OverallSummary();
        PatientOverallSummary patientSummary = new PatientOverallSummary();
        patientSummary.setNumberRejected(1);
        patientSummary.setNumberProcessed(25);
        patientSummary.setNumberMatched(24);
        summary.setPatientSummary(patientSummary);
        TumorOverallSummary tumorSummary = new TumorOverallSummary();
        tumorSummary.setNumberRejected(1);
        tumorSummary.setNumberProcessed(25);
        tumorSummary.setNumberMatched(24);
        summary.setTumorSummary(tumorSummary);
        // *** end of root extension
        data.addExtesion(summary);
        for (int i = 1; i <= 25; i++) {
            Patient patient = new Patient();
            patient.addItem(new Item("patientIdNumber", StringUtils.leftPad(String.valueOf(i), 8, '0')));
            // *** start of patient extension
            PatientSummary patSummary = new PatientSummary();
            patSummary.setMatchingMethod("Patient Matching Algorithm #1");
            patSummary.setMatchingScore(i - 1);
            patSummary.setMatchingIdentifier("PAT-XXX");
            patient.addExtesion(patSummary);
            // *** end of patient extension
            data.addPatient(patient);
            Tumor tumor = new Tumor();
            tumor.addItem(new Item("sequenceNumberCentral", "01"));
            // *** start of tumor extension
            TumorSummary tumSummary = new TumorSummary();
            tumSummary.setMatchingMethod("Tumor Matching Algorithm #1");
            tumSummary.setMatchingScore(i - 1);
            tumSummary.setMatchingIdentifier("TUM-YYY");
            tumor.addExtesion(tumSummary);
            // *** end of patient extension
            patient.addTumor(tumor);
        }

        // setup configuration; tell the library how to read/write the extensions
        NaaccrStreamConfiguration configuration = new NaaccrStreamConfiguration();
        configuration.registerNamespace("ext", "http://demo.org");
        configuration.registerTag("ext", "OverallSummary", OverallSummary.class);
        configuration.registerTag("ext", "Patient", OverallSummary.class, "_patientSummary", PatientOverallSummary.class);
        configuration.registerTag("ext", "NumberRejected", PatientOverallSummary.class, "_numberRejected", Integer.class);
        configuration.registerTag("ext", "NumberProcessed", PatientOverallSummary.class, "_numberProcessed", Integer.class);
        configuration.registerTag("ext", "NumberMatched", PatientOverallSummary.class, "_numberMatched", Integer.class);
        configuration.registerTag("ext", "Tumor", OverallSummary.class, "_tumorSummary", TumorOverallSummary.class);
        configuration.registerTag("ext", "NumberRejected", TumorOverallSummary.class, "_numberRejected", Integer.class);
        configuration.registerTag("ext", "NumberProcessed", TumorOverallSummary.class, "_numberProcessed", Integer.class);
        configuration.registerTag("ext", "NumberMatched", TumorOverallSummary.class, "_numberMatched", Integer.class);
        configuration.registerTag("ext", "PatientSummary", PatientSummary.class);
        configuration.registerTag("ext", "MatchingMethod", PatientSummary.class, "_matchingMethod", String.class);
        configuration.registerTag("ext", "MatchingScore", PatientSummary.class, "_matchingScore", Integer.class);
        configuration.registerTag("ext", "MatchingIdentifier", PatientSummary.class, "_matchingIdentifier", String.class);
        configuration.registerTag("ext", "TumorSummary", TumorSummary.class);
        configuration.registerTag("ext", "MatchingMethod", TumorSummary.class, "_matchingMethod", String.class);
        configuration.registerTag("ext", "MatchingScore", TumorSummary.class, "_matchingScore", Integer.class);
        configuration.registerTag("ext", "MatchingIdentifier", TumorSummary.class, "_matchingIdentifier", String.class);

        // write the patients (the root is written as part of the writer creation)
        File file = TestingUtils.createFile("test-root-extension.xml", false);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, (NaaccrDictionary)null, configuration)) {
            for (Patient p : data.getPatients())
                writer.writePatient(p);
        }

        // read back the file and print a few values to make sure we can access them
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(file), null, (NaaccrDictionary)null, configuration)) {
            OverallSummary os = (OverallSummary)reader.getRootData().getExtensions().get(0);
            System.out.println("total number of processed patients: " + os.getPatientSummary().getNumberProcessed());
            System.out.println("total number of processed tumors:" + os.getTumorSummary().getNumberProcessed());
            Patient pat = reader.readPatient();
            PatientSummary patSum = (PatientSummary)reader.readPatient().getExtensions().get(0);
            System.out.println(" > first patient matching method: " + patSum.getMatchingMethod());
            TumorSummary tumSum = (TumorSummary)pat.getTumors().get(0).getExtensions().get(0);
            System.out.println(" > first tumor matching method: " + tumSum.getMatchingMethod());
        }
    }

    /**
     * Helper class that encapsulates the patient and tumor overall summary.
     */
    public static class OverallSummary {

        private PatientOverallSummary _patientSummary;
        private TumorOverallSummary _tumorSummary;

        public PatientOverallSummary getPatientSummary() {
            return _patientSummary;
        }

        public void setPatientSummary(PatientOverallSummary patientSummary) {
            _patientSummary = patientSummary;
        }

        public TumorOverallSummary getTumorSummary() {
            return _tumorSummary;
        }

        public void setTumorSummary(TumorOverallSummary tumorSummary) {
            _tumorSummary = tumorSummary;
        }
    }

    /**
     * Helper class representing an overall summary for patients.
     */
    public static class PatientOverallSummary {

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

    /**
     * Helper class representing an overall summary for tumors.
     */
    public static class TumorOverallSummary {

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

    /**
     * Helper class representing the summary of a single patient.
     */
    public static class PatientSummary {

        private String _matchingMethod;
        private Integer _matchingScore;
        private String _matchingIdentifier;

        public String getMatchingMethod() {
            return _matchingMethod;
        }

        public void setMatchingMethod(String matchingMethod) {
            _matchingMethod = matchingMethod;
        }

        public Integer getMatchingScore() {
            return _matchingScore;
        }

        public void setMatchingScore(Integer matchingScore) {
            _matchingScore = matchingScore;
        }

        public String getMatchingIdentifier() {
            return _matchingIdentifier;
        }

        public void setMatchingIdentifier(String matchingIdentifier) {
            _matchingIdentifier = matchingIdentifier;
        }
    }

    /**
     * Helper class representing the summary of a single tumor.
     */
    public static class TumorSummary {

        private String _matchingMethod;
        private Integer _matchingScore;
        private String _matchingIdentifier;

        public String getMatchingMethod() {
            return _matchingMethod;
        }

        public void setMatchingMethod(String matchingMethod) {
            _matchingMethod = matchingMethod;
        }

        public Integer getMatchingScore() {
            return _matchingScore;
        }

        public void setMatchingScore(Integer matchingScore) {
            _matchingScore = matchingScore;
        }

        public String getMatchingIdentifier() {
            return _matchingIdentifier;
        }

        public void setMatchingIdentifier(String matchingIdentifier) {
            _matchingIdentifier = matchingIdentifier;
        }
    }
}
