/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public class PatientXmlReaderTest {

    @Test
    public void testReader() throws IOException {

        // one patient with no tumor
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-one-patient-no-tumor.xml")), null, null, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
            Assert.assertEquals(3, patient.getItem("patientIdNumber").getStartLineNumber().intValue());
            Assert.assertEquals(0, patient.getTumors().size());
            Assert.assertNull(reader.readPatient());
        }

        // one patient with one tumor
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-one-patient-one-tumor.xml")), null, null, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
            Assert.assertEquals(1, patient.getTumors().size());
            Assert.assertEquals("C123", patient.getTumors().get(0).getItem("primarySite").getValue());
            Assert.assertNull(reader.readPatient());
        }

        // one patient with two tumors
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-one-patient-two-tumors.xml")), null, null, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
            Assert.assertEquals(2, patient.getTumors().size());
            Assert.assertEquals("C123", patient.getTumors().get(0).getItem("primarySite").getValue());
            Assert.assertEquals("C456", patient.getTumors().get(1).getItem("primarySite").getValue());
            Assert.assertNull(reader.readPatient());
        }

        // two patients with one tumor each
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-two-patients.xml")), null, null, null)) {
            Patient patient1 = reader.readPatient();
            Assert.assertEquals("00000001", patient1.getItem("patientIdNumber").getValue());
            Assert.assertEquals(1, patient1.getTumors().size());
            Assert.assertEquals("C123", patient1.getTumors().get(0).getItem("primarySite").getValue());
            Assert.assertEquals(6, patient1.getTumors().get(0).getItem("primarySite").getStartLineNumber().intValue());
            Patient patient2 = reader.readPatient();
            Assert.assertEquals("00000002", patient2.getItem("patientIdNumber").getValue());
            Assert.assertEquals(1, patient2.getTumors().size());
            Assert.assertEquals("C456", patient2.getTumors().get(0).getItem("primarySite").getValue());
            Assert.assertEquals(12, patient2.getTumors().get(0).getItem("primarySite").getStartLineNumber().intValue());
            Assert.assertNull(reader.readPatient());
        }

        // test the root data attributes
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-root-data-1.xml")), null, null, null)) {
            NaaccrData data = reader.getRootData();
            Assert.assertNotNull(data.getBaseDictionaryUri());
            Assert.assertNull(data.getUserDictionaryUri());
            Assert.assertNotNull(data.getRecordType());
            Assert.assertNull(data.getTimeGenerated());
            Assert.assertEquals("1.0", data.getSpecificationVersion());
            Assert.assertEquals(0, data.getItems().size());
        }

        // another test with the root data and the new 1.1 specification
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-root-data-2.xml")), null, null, null)) {
            NaaccrData data = reader.getRootData();
            Assert.assertNotNull(data.getBaseDictionaryUri());
            Assert.assertNull(data.getUserDictionaryUri());
            Assert.assertNotNull(data.getRecordType());
            Assert.assertNotNull(data.getTimeGenerated());
            Assert.assertEquals("1.1", data.getSpecificationVersion());
            Assert.assertEquals(1, data.getItems().size());
        }

        // test validation
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-validation-1.xml")), null, null, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals(1, patient.getTumors().size());
            // error should have been reported on the item
            Assert.assertNotNull(patient.getTumors().get(0).getItem("primarySite").getValidationError());
            // it shouldn't be available on the tumor...
            Assert.assertTrue(patient.getTumors().get(0).getValidationErrors().isEmpty());
            // ... unless the "get-all-errors" method is used
            Assert.assertFalse(patient.getTumors().get(0).getAllValidationErrors().isEmpty());
            // even if the value is bad, its still being made available in the patient (if possible)
            Assert.assertEquals("XXXX", patient.getTumors().get(0).getItem("primarySite").getValue());
            // the validation error shouldn't be available on the patient...
            Assert.assertTrue(patient.getValidationErrors().isEmpty());
            // ... unless the "get-all-errors" method is used
            Assert.assertFalse(patient.getAllValidationErrors().isEmpty());
        }

        // this file has a duplicate item for the patient
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-validation-2.xml")), null, null, null)) {
            try {
                reader.readPatient();
                Assert.fail("Should have been an exception!");
            }
            catch (NaaccrIOException e) {
                // expected
            }
        }

        // by default, a value too long should be reported as an error but shouldn't be truncated
        NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(TestingUtils.getDataFile("testing-user-dictionary.xml"));
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-too-long.xml")), null, dict, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("XX", patient.getItemValue("myVariable2"));
            Assert.assertFalse(patient.getAllValidationErrors().isEmpty());
            NaaccrValidationError error = patient.getAllValidationErrors().get(0);
            Assert.assertTrue(error.getMessage().contains("long"));
        }
    }

    @Test
    public void testOptions() throws IOException {

        NaaccrOptions options = new NaaccrOptions();

        // only valid items are validated, so setting it to false shouldn't affect the test...
        options.setValidateReadValues(false);

        // we want to process the unknown item
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_PROCESS);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-unk.xml")), options, null, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("X", patient.getItemValue("unknown"));
            Assert.assertTrue(patient.getValidationErrors().isEmpty());
        }

        // we want to ignore the unknown item
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_IGNORE);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-unk.xml")), options, null, null)) {
            Patient patient = reader.readPatient();
            Assert.assertNull(patient.getItemValue("unknown"));
            Assert.assertTrue(patient.getValidationErrors().isEmpty());
        }

        // we want to report an error for the unknown item
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_ERROR);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-unk.xml")), options, null, null)) {
            Patient patient = reader.readPatient();
            Assert.assertNull(patient.getItemValue("unknown"));
            Assert.assertFalse(patient.getValidationErrors().isEmpty());
            NaaccrValidationError error = patient.getValidationErrors().get(0);
            Assert.assertNotNull(error.getMessage());
            Assert.assertNotNull(error.getLineNumber());
            Assert.assertNull(error.getNaaccrId());
            Assert.assertNull(error.getNaaccrNum());
            Assert.assertNull(error.getValue());
        }
    }

    @Test
    public void testUserDefinedDictionary() throws IOException {

        NaaccrDictionary dict = TestingUtils.createUserDictionary();
        Assert.assertNotNull(dict.getItemByNaaccrId("myVariable"));
        Assert.assertNotNull(dict.getItemByNaaccrNum(10000));

        // regular value for the extra variable
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-1.xml")), null, dict, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("01", patient.getTumors().get(0).getItemValue("myVariable"));
        }

        // padding (and trimming) only applies to reading from flat-file, not XML...
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-2.xml")), null, dict, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("1", patient.getTumors().get(0).getItemValue("myVariable"));
        }

        // value is not correct according to the provided regex (error should be reported on the item itself)
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-3.xml")), null, dict, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("09", patient.getTumors().get(0).getItemValue("myVariable"));
            Assert.assertNotNull(patient.getTumors().get(0).getItem("myVariable").getValidationError());
            Assert.assertFalse(patient.getTumors().get(0).getAllValidationErrors().isEmpty());
            Assert.assertTrue(patient.getTumors().get(0).getValidationErrors().isEmpty());
        }
    }
}
