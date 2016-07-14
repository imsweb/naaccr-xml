package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;

public class PatientXmlWriterTest {

    @Test
    public void testWriter() throws IOException {

        // create the root data
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_15_INCIDENCE);
        data.addItem(new Item("registryId", "0000000001"));

        // a patient with no tumor
        File file = TestingUtils.createFile("test-xml-writer-no-tumor.xml");
        PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, null);
        Patient patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));
        writer.writePatient(patient);
        writer.close();
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        String xmlAsString = TestingUtils.readFileAsOneString(file);
        Assert.assertTrue(xmlAsString.contains("specificationVersion=\"" + NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION + "\""));
        Assert.assertTrue(xmlAsString.contains("timeGenerated="));

        // a patient with one tumor
        file = TestingUtils.createFile("test-flat-writer-one-tumor.txt");
        writer = new PatientXmlWriter(new FileWriter(file), data, null, null);
        patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));
        Tumor tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C123"));
        patient.addTumor(tumor1);
        writer.writePatient(patient);
        writer.close();
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("C123", patient.getTumors().get(0).getItemValue("primarySite"));

        // a patient with two tumors
        file = TestingUtils.createFile("test-flat-writer-two-tumors.txt");
        writer = new PatientXmlWriter(new FileWriter(file), data, null, null);
        patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));
        tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C123"));
        patient.addTumor(tumor1);
        tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C456"));
        patient.addTumor(tumor1);
        writer.writePatient(patient);
        writer.close();
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("C123", patient.getTumors().get(0).getItemValue("primarySite"));
        Assert.assertEquals("C456", patient.getTumors().get(1).getItemValue("primarySite"));

        // two patients with one tumor each
        file = TestingUtils.createFile("test-flat-writer-two-patients.txt");
        writer = new PatientXmlWriter(new FileWriter(file), data, null, null);
        Patient patient1 = new Patient();
        patient1.addItem(new Item("patientIdNumber", "00000001"));
        tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C123"));
        patient1.addTumor(tumor1);
        writer.writePatient(patient1);
        Patient patient2 = new Patient();
        patient2.addItem(new Item("patientIdNumber", "00000002"));
        Tumor tumor2 = new Tumor();
        tumor2.addItem(new Item("primarySite", "C456"));
        patient2.addTumor(tumor2);
        writer.writePatient(patient2);
        writer.close();
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("C123", patient.getTumors().get(0).getItemValue("primarySite"));
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(1);
        Assert.assertEquals("00000002", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("C456", patient.getTumors().get(0).getItemValue("primarySite"));
    }
}
