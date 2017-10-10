/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public class PatientFlatReaderTest {

    @Test
    public void testReader() throws IOException {

        // file with one record (one patient with one tumor)
        StringBuilder rec1 = TestingUtils.createEmptyRecord("150", "I", null);
        rec1.replace(29, 39, "0000000001"); // registry ID (Root level)
        rec1.replace(41, 49, "00000001"); // patient ID number (Patient level)
        rec1.replace(539, 543, "C123"); // primarySite (Tumor level)
        File file = TestingUtils.createAndPopulateFile("test-flat-reader-one-rec.txt", rec1);
        PatientFlatReader reader = new PatientFlatReader(new FileReader(file));
        Assert.assertEquals(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION, reader.getRootData().getSpecificationVersion());
        Assert.assertEquals("0000000001", reader.getRootData().getItem("registryId").getValue());
        Patient patient = reader.readPatient();
        Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
        Assert.assertEquals(1, patient.getTumors().size());
        Assert.assertEquals("C123", patient.getTumors().get(0).getItem("primarySite").getValue());
        Assert.assertEquals(1, patient.getTumors().get(0).getItem("primarySite").getStartLineNumber().intValue());
        Assert.assertNull(reader.readPatient());
        reader.close();

        // file with two records for different patients (two patients with each one tumor)
        StringBuilder rec2 = TestingUtils.createEmptyRecord("150", "I", null);
        rec2.replace(29, 39, "0000000001"); // registry ID (Root level)
        rec2.replace(41, 49, "00000002"); // patient ID number (Patient level)
        rec2.replace(539, 543, "C456"); // primarySite (Tumor level)
        file = TestingUtils.createAndPopulateFile("test-flat-reader-two-rec-diff.txt", rec1, rec2);
        reader = new PatientFlatReader(new FileReader(file), null);
        Assert.assertEquals("0000000001", reader.getRootData().getItem("registryId").getValue());
        patient = reader.readPatient();
        Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
        Assert.assertEquals(1, patient.getTumors().size());
        Assert.assertEquals("C123", patient.getTumors().get(0).getItem("primarySite").getValue());
        Assert.assertEquals(1, patient.getTumors().get(0).getItem("primarySite").getStartLineNumber().intValue());
        patient = reader.readPatient();
        Assert.assertEquals("00000002", patient.getItem("patientIdNumber").getValue());
        Assert.assertEquals(1, patient.getTumors().size());
        Assert.assertEquals("C456", patient.getTumors().get(0).getItem("primarySite").getValue());
        Assert.assertEquals(2, patient.getTumors().get(0).getItem("primarySite").getStartLineNumber().intValue());
        Assert.assertNull(reader.readPatient());
        reader.close();

        // file with two records for the same patient (one patient with two tumors)
        rec2.replace(41, 49, "00000001"); // patient ID number (Patient level)
        file = TestingUtils.createAndPopulateFile("test-flat-reader-two-rec-same.txt", rec1, rec2);
        reader = new PatientFlatReader(new FileReader(file), null, (NaaccrDictionary)null);
        Assert.assertEquals("0000000001", reader.getRootData().getItem("registryId").getValue());
        patient = reader.readPatient();
        Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
        Assert.assertEquals(2, patient.getTumors().size());
        Assert.assertEquals("C123", patient.getTumors().get(0).getItem("primarySite").getValue());
        Assert.assertEquals("C456", patient.getTumors().get(1).getItem("primarySite").getValue());
        Assert.assertNull(reader.readPatient());
        reader.close();
    }

    @Test
    public void testReaderBadConditions() throws IOException {

        // empty file
        File file = TestingUtils.createAndPopulateFile("test-flat-reader-empty.txt");
        try {
            new PatientFlatReader(new FileReader(file), null, (NaaccrDictionary)null);
            Assert.fail("Should have been an exception!");
        }
        catch (NaaccrIOException e) {
            // expected...
        }

        // one line that is empty
        file = TestingUtils.createAndPopulateFile("test-flat-reader-blank.txt", new StringBuilder());
        try {
            new PatientFlatReader(new FileReader(file), null, (NaaccrDictionary)null);
            Assert.fail("Should have been an exception!");
        }
        catch (NaaccrIOException e) {
            // expected...
        }

        StringBuilder line = TestingUtils.createEmptyRecord("150", "I", null);

        // line doesn't have the record type
        line.replace(0, 1, " ");
        file = TestingUtils.createAndPopulateFile("test-flat-reader-missing-type.txt", line);
        try {
            new PatientFlatReader(new FileReader(file), null, (NaaccrDictionary)null);
            Assert.fail("Should have been an exception!");
        }
        catch (NaaccrIOException e) {
            // expected...
        }

        // line has an invalid record type
        line.replace(0, 1, "X");
        file = TestingUtils.createAndPopulateFile("test-flat-reader-invalid-type.txt", line);
        try {
            new PatientFlatReader(new FileReader(file), null, (NaaccrDictionary)null);
            Assert.fail("Should have been an exception!");
        }
        catch (NaaccrIOException e) {
            // expected...
        }

        // line doesn't have the NAACCR version
        line.replace(0, 1, "I");
        line.replace(16, 19, "   ");
        file = TestingUtils.createAndPopulateFile("test-flat-reader-missing-version.txt", line);
        try {
            new PatientFlatReader(new FileReader(file), null, (NaaccrDictionary)null);
            Assert.fail("Should have been an exception!");
        }
        catch (NaaccrIOException e) {
            // expected...
        }

        // line has an invalid NAACCR version
        line.replace(16, 19, "XXX");
        file = TestingUtils.createAndPopulateFile("test-flat-reader-invalid-version.txt", line);
        try {
            new PatientFlatReader(new FileReader(file), null, (NaaccrDictionary)null);
            Assert.fail("Should have been an exception!");
        }
        catch (NaaccrIOException e) {
            // expected...
        }

        // line is too long
        line.replace(16, 19, "150");
        line.append("X");
        file = TestingUtils.createAndPopulateFile("test-flat-reader-invalid-version.txt", line);
        try {
            new PatientFlatReader(new FileReader(file), null, (NaaccrDictionary)null);
            Assert.fail("Should have been an exception!");
        }
        catch (NaaccrIOException e) {
            // expected...
        }
    }

    @Test
    public void testPatientLevelMismatch() throws IOException {

        // we need to turn the miss-match checking on...
        NaaccrOptions options = new NaaccrOptions();
        options.setReportLevelMismatch(true);

        // create a testing file with two records having a different value for a patient-level item
        StringBuilder rec1 = TestingUtils.createEmptyRecord("150", "I", "00000001");
        rec1.replace(189, 190, "1"); // computed ethnicity
        StringBuilder rec2 = TestingUtils.createEmptyRecord("150", "I", "00000001");
        rec2.replace(189, 190, "2"); // computed ethnicity
        File file = TestingUtils.createAndPopulateFile("test-flat-reader-pat-mismatch.txt", rec1, rec2);

        // lets read the patient, it should contain two tumors
        PatientFlatReader reader = new PatientFlatReader(new FileReader(file), options, (NaaccrDictionary)null);

        Patient pat1 = reader.readPatient();
        Assert.assertNotNull(pat1);
        Assert.assertEquals(2, pat1.getTumors().size()); // tumors are both for the same patient ID number, should be one patient with two tumors...
        Assert.assertEquals(1, pat1.getAllValidationErrors().size());
        Assert.assertTrue(pat1.getAllValidationErrors().get(0).getMessage().contains("patient-level"));

        Assert.assertNull(reader.readPatient());
        reader.close();
    }

    @Test
    public void testRootLevelMismatch() throws IOException {

        // we need to turn the miss-match checking on...
        NaaccrOptions options = new NaaccrOptions();
        options.setReportLevelMismatch(true);

        // create a testing file with two records having a different value for a root-level item
        StringBuilder rec1 = TestingUtils.createEmptyRecord("150", "I", null);
        rec1.replace(19, 29, "0000000001"); // registry ID
        StringBuilder rec2 = TestingUtils.createEmptyRecord("150", "I", null);
        rec2.replace(19, 29, "0000000002"); // registry ID
        File file = TestingUtils.createAndPopulateFile("test-flat-reader-root-mismatch.txt", rec1, rec2);

        // lets read the patients; first one should be used to populate the root data; second one should report a failure for the mismatch
        PatientFlatReader reader = new PatientFlatReader(new FileReader(file), options, (NaaccrDictionary)null);

        Patient pat1 = reader.readPatient();
        Assert.assertNotNull(pat1);
        Assert.assertEquals(1, pat1.getTumors().size()); // no patient ID number provided, should be two patients with one tumor each
        Assert.assertEquals(0, pat1.getAllValidationErrors().size());

        Patient pat2 = reader.readPatient();
        Assert.assertNotNull(pat2);
        Assert.assertEquals(1, pat2.getTumors().size());
        Assert.assertEquals(1, pat2.getAllValidationErrors().size());
        Assert.assertTrue(pat2.getAllValidationErrors().get(0).getMessage().contains("root-level"));

        Assert.assertNull(reader.readPatient());
        reader.close();
    }

    @Test
    public void testUserDefinedDictionary() throws IOException {

        StringBuilder rec1 = TestingUtils.createEmptyRecord("150", "I", "00000001");
        rec1.replace(2339, 2340, "X"); // state requestor items
        File file = TestingUtils.createAndPopulateFile("test-flat-reader-user-dict.txt", rec1);

        // first, let's use the default user dictionary (so null)
        try (PatientFlatReader reader = new PatientFlatReader(new FileReader(file), null, (NaaccrDictionary)null)) {
            Tumor tumor = reader.readPatient().getTumors().get(0);
            Assert.assertEquals(1000, tumor.getItem("stateRequestorItems").getValue().length());
            Assert.assertTrue(tumor.getItem("stateRequestorItems").getValue().startsWith("X"));
        }

        // the result should be the same if we pass the same default dictionary to the method
        try (PatientFlatReader reader = new PatientFlatReader(new FileReader(file), null, NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_150))) {
            Tumor tumor = reader.readPatient().getTumors().get(0);
            Assert.assertEquals(1000, tumor.getItem("stateRequestorItems").getValue().length());
            Assert.assertTrue(tumor.getItem("stateRequestorItems").getValue().startsWith("X"));
        }

        // if we pass an "empty" user dictionary, then the state requestor item should be ignored, resulting in a null item...
        NaaccrDictionary dict = new NaaccrDictionary();
        dict.setNaaccrVersion("150");
        dict.setDictionaryUri("whatever");
        try (PatientFlatReader reader = new PatientFlatReader(new FileReader(file), null, dict)) {
            Tumor tumor = reader.readPatient().getTumors().get(0);
            Assert.assertNull(tumor.getItem("stateRequestorItems"));
        }

        // using a dictionary with items that are not tied to a start location; those should be ignored...
        try (Reader dictReader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary.xml"))) {
            try (PatientFlatReader reader = new PatientFlatReader(new FileReader(file), null, NaaccrXmlDictionaryUtils.readDictionary(dictReader))) {
                Tumor tumor = reader.readPatient().getTumors().get(0);
                Assert.assertNull(tumor.getItem("stateRequestorItems"));
            }
        }
    }
}
