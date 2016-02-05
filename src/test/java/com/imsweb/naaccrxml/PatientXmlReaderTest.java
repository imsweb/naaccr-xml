/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Patient;

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

}
