/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
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
        File file = TestingUtils.getDataFile("xml-reader-one-patient-no-tumor.xml");
        PatientXmlReader reader = new PatientXmlReader(new FileReader(file), null, null, null);
        Patient patient = reader.readPatient();
        Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
        Assert.assertEquals(0, patient.getTumors().size());
        Assert.assertNull(reader.readPatient());
        reader.close();

        // one patient with one tumor
        file = TestingUtils.getDataFile("xml-reader-one-patient-one-tumor.xml");
        reader = new PatientXmlReader(new FileReader(file), null, null, null);
        patient = reader.readPatient();
        Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
        Assert.assertEquals(1, patient.getTumors().size());
        Assert.assertEquals("C123", patient.getTumors().get(0).getItem("primarySite").getValue());
        Assert.assertNull(reader.readPatient());
        reader.close();

        // one patient with two tumors
        file = TestingUtils.getDataFile("xml-reader-one-patient-two-tumors.xml");
        reader = new PatientXmlReader(new FileReader(file), null, null, null);
        patient = reader.readPatient();
        Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
        Assert.assertEquals(2, patient.getTumors().size());
        Assert.assertEquals("C123", patient.getTumors().get(0).getItem("primarySite").getValue());
        Assert.assertEquals("C456", patient.getTumors().get(1).getItem("primarySite").getValue());
        Assert.assertNull(reader.readPatient());
        reader.close();

        // two patients with one tumor each
        file = TestingUtils.getDataFile("xml-reader-two-patients.xml");
        reader = new PatientXmlReader(new FileReader(file), null, null, null);
        Patient patient1 = reader.readPatient();
        Assert.assertEquals("00000001", patient1.getItem("patientIdNumber").getValue());
        Assert.assertEquals(1, patient1.getTumors().size());
        Assert.assertEquals("C123", patient1.getTumors().get(0).getItem("primarySite").getValue());
        Patient patient2 = reader.readPatient();
        Assert.assertEquals("00000002", patient2.getItem("patientIdNumber").getValue());
        Assert.assertEquals(1, patient2.getTumors().size());
        Assert.assertEquals("C456", patient2.getTumors().get(0).getItem("primarySite").getValue());
        Assert.assertNull(reader.readPatient());
        reader.close();
    }

    @Test
    public void testRootData() throws IOException {

        File file = TestingUtils.getDataFile("xml-reader-root-data-1.xml");
        PatientXmlReader reader = new PatientXmlReader(new FileReader(file), null, null, null);
        NaaccrData data = reader.getRootData();
        Assert.assertNotNull(data.getBaseDictionaryUri());
        Assert.assertNull(data.getUserDictionaryUri());
        Assert.assertNotNull(data.getRecordType());
        Assert.assertNull(data.getTimeGenerated());
        Assert.assertEquals("1.0", data.getSpecificationVersion());
        Assert.assertEquals(0, data.getItems().size());
        reader.close();

        file = TestingUtils.getDataFile("xml-reader-root-data-2.xml");
        reader = new PatientXmlReader(new FileReader(file), null, null, null);
        data = reader.getRootData();
        Assert.assertNotNull(data.getBaseDictionaryUri());
        Assert.assertNull(data.getUserDictionaryUri());
        Assert.assertNotNull(data.getRecordType());
        Assert.assertNotNull(data.getTimeGenerated());
        Assert.assertEquals("1.1", data.getSpecificationVersion());
        Assert.assertEquals(1, data.getItems().size());
        reader.close();
    }

    @Test
    public void testOptions() throws IOException {

        NaaccrOptions options = new NaaccrOptions();

        // only valid items are validated, so setting it to false shouldn't affect the test...
        options.setValidateReadValues(false);

        // we want to process the unknown item
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_PROCESS);
        File file = TestingUtils.getDataFile("xml-reader-options.xml");
        PatientXmlReader reader = new PatientXmlReader(new FileReader(file), options, null, null);
        Patient patient = reader.readPatient();
        Assert.assertEquals("X", patient.getItemValue("unknown"));
        Assert.assertTrue(patient.getValidationErrors().isEmpty());
        reader.close();

        // we want to ignore the unknown item
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_IGNORE);
        file = TestingUtils.getDataFile("xml-reader-options.xml");
        reader = new PatientXmlReader(new FileReader(file), options, null, null);
        patient = reader.readPatient();
        Assert.assertNull(patient.getItemValue("unknown"));
        Assert.assertTrue(patient.getValidationErrors().isEmpty());
        reader.close();

        // we want to report an error for the unknown item
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_ERROR);
        file = TestingUtils.getDataFile("xml-reader-options.xml");
        reader = new PatientXmlReader(new FileReader(file), options, null, null);
        patient = reader.readPatient();
        Assert.assertNull(patient.getItemValue("unknown"));
        Assert.assertFalse(patient.getValidationErrors().isEmpty());
        NaaccrValidationError error = patient.getValidationErrors().get(0);
        Assert.assertNotNull(error.getMessage());
        Assert.assertNotNull(error.getLineNumber());
        Assert.assertNull(error.getNaaccrId());
        Assert.assertNull(error.getNaaccrNum());
        Assert.assertNull(error.getValue());
        reader.close();
    }

    @Test
    public void testValidation() throws IOException {

        // validation is on by default...

        File file = TestingUtils.getDataFile("xml-reader-validation-1.xml");
        PatientXmlReader reader = new PatientXmlReader(new FileReader(file), null, null, null);
        Patient patient = reader.readPatient();
        reader.close();
        Assert.assertEquals(1, patient.getTumors().size());
        Assert.assertFalse(patient.getTumors().get(0).getValidationErrors().isEmpty());
        // even if the value is bad, its still being made available in the patient (if possible)
        Assert.assertEquals("XXXX", patient.getTumors().get(0).getItem("primarySite").getValue());
        // the validation error shouldn't be available on the patient...
        Assert.assertTrue(patient.getValidationErrors().isEmpty());
        // ... unless the "get-all-errors" method is used
        Assert.assertFalse(patient.getAllValidationErrors().isEmpty());

        // this file has a duplicate item for the patient
        file = TestingUtils.getDataFile("xml-reader-validation-2.xml");
        reader = new PatientXmlReader(new FileReader(file), null, null, null);
        try {
            patient = reader.readPatient();
            Assert.fail("Should have been an exception!");
        }
        catch (NaaccrIOException e) {
            // expected
        }
        reader.close();
    }

    @Test
    public void testUserDefinedDictionary() throws IOException {

        NaaccrDictionary dict = TestingUtils.createUserDictionary();
        Assert.assertNotNull(dict.getItemByNaaccrId("myVariable"));
        Assert.assertNotNull(dict.getItemByNaaccrNum(10000));

        // regular value for the extra variable
        File file = TestingUtils.getDataFile("xml-reader-user-dict-1.xml");
        PatientXmlReader reader = new PatientXmlReader(new FileReader(file), null, dict, null);
        Patient patient = reader.readPatient();
        Assert.assertEquals("01", patient.getTumors().get(0).getItemValue("myVariable"));
        reader.close();

        // padding (and trimming) only applies to reading from flat-file, not XML...
        file = TestingUtils.getDataFile("xml-reader-user-dict-2.xml");
        reader = new PatientXmlReader(new FileReader(file), null, dict, null);
        patient = reader.readPatient();
        Assert.assertEquals("1", patient.getTumors().get(0).getItemValue("myVariable"));
        reader.close();

        // value is not correct according to the provided regex
        file = TestingUtils.getDataFile("xml-reader-user-dict-3.xml");
        reader = new PatientXmlReader(new FileReader(file), null, dict, null);
        patient = reader.readPatient();
        Assert.assertEquals("09", patient.getTumors().get(0).getItemValue("myVariable"));
        Assert.assertFalse(patient.getTumors().get(0).getValidationErrors().isEmpty());
        reader.close();
    }
}
