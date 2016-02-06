package com.imsweb.naaccrxml;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PatientXmlWriterTest {

    @Test
    public void testWriter() throws IOException {

        // create the root data
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_15_INCIDENCE);
        data.getItems().add(new Item("registryId", null, "0000000001"));

        // a patient with no tumor
        File file = TestingUtils.createFile("test-xml-writer-no-tumor.xml");
        PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, null);
        Patient patient = new Patient();
        patient.getItems().add(new Item("patientIdNumber", null, "00000001"));
        writer.writePatient(patient);
        writer.close();
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));

        // a patient with one tumor
        file = TestingUtils.createFile("test-flat-writer-one-tumor.txt");
        writer = new PatientXmlWriter(new FileWriter(file), data, null, null);
        patient = new Patient();
        patient.getItems().add(new Item("patientIdNumber", null, "00000001"));
        Tumor tumor1 = new Tumor();
        tumor1.getItems().add(new Item("primarySite", null, "C123"));
        patient.getTumors().add(tumor1);
        writer.writePatient(patient);
        writer.close();
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("C123", patient.getTumors().get(0).getItemValue("primarySite"));

        // a patient with two tumors
        file = TestingUtils.createFile("test-flat-writer-two-tumors.txt");
        writer = new PatientXmlWriter(new FileWriter(file), data, null, null);
        patient = new Patient();
        patient.getItems().add(new Item("patientIdNumber", null, "00000001"));
        tumor1 = new Tumor();
        tumor1.getItems().add(new Item("primarySite", null, "C123"));
        patient.getTumors().add(tumor1);
        tumor1 = new Tumor();
        tumor1.getItems().add(new Item("primarySite", null, "C456"));
        patient.getTumors().add(tumor1);
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
        patient1.getItems().add(new Item("patientIdNumber", null, "00000001"));
        tumor1 = new Tumor();
        tumor1.getItems().add(new Item("primarySite", null, "C123"));
        patient1.getTumors().add(tumor1);
        writer.writePatient(patient1);
        Patient patient2 = new Patient();
        patient2.getItems().add(new Item("patientIdNumber", null, "00000002"));
        Tumor tumor2 = new Tumor();
        tumor2.getItems().add(new Item("primarySite", null, "C456"));
        patient2.getTumors().add(tumor2);
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
