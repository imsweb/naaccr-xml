/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;

public class PatientFlatFileWriterTest {

    @Test
    public void testWriter() throws IOException {

        // create the root data
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_15_INCIDENCE);
        data.getItems().add(new Item("registryId", null, "0000000001"));

        // a patient with no tumor
        File file = TestingUtils.createFile("test-flat-writer-no-tumor.txt");
        PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), data, null, null);
        Patient patient = new Patient();
        patient.getItems().add(new Item("patientIdNumber", null, "00000001"));
        writer.writePatient(patient);
        writer.close();
        List<String> lines = TestingUtils.readFile(file);
        Assert.assertEquals(1, lines.size());
        Assert.assertEquals("I", lines.get(0).substring(0, 1)); // record type
        Assert.assertEquals("150", lines.get(0).substring(16, 19)); // NAACCR version
        Assert.assertEquals("0000000001", lines.get(0).substring(29, 39)); // registry ID
        Assert.assertEquals("00000001", lines.get(0).substring(41, 49)); // patient ID
        Assert.assertEquals("    ", lines.get(0).substring(539, 543)); // primary site

        // a patient with one tumor
        file = TestingUtils.createFile("test-flat-writer-one-tumor.txt");
        writer = new PatientFlatWriter(new FileWriter(file), data, null, null);
        patient = new Patient();
        patient.getItems().add(new Item("patientIdNumber", null, "00000001"));
        Tumor tumor1 = new Tumor();
        tumor1.getItems().add(new Item("primarySite", null, "C123"));
        patient.getTumors().add(tumor1);
        writer.writePatient(patient);
        writer.close();
        lines = TestingUtils.readFile(file);
        Assert.assertEquals(1, lines.size());
        Assert.assertEquals("I", lines.get(0).substring(0, 1)); // record type
        Assert.assertEquals("150", lines.get(0).substring(16, 19)); // NAACCR version
        Assert.assertEquals("0000000001", lines.get(0).substring(29, 39)); // registry ID
        Assert.assertEquals("00000001", lines.get(0).substring(41, 49)); // patient ID
        Assert.assertEquals("C123", lines.get(0).substring(539, 543)); // primary site

        // a patient with two tumors
        file = TestingUtils.createFile("test-flat-writer-two-tumors.txt");
        writer = new PatientFlatWriter(new FileWriter(file), data, null, null);
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
        lines = TestingUtils.readFile(file);
        Assert.assertEquals(2, lines.size());
        Assert.assertEquals("I", lines.get(0).substring(0, 1)); // record type
        Assert.assertEquals("150", lines.get(0).substring(16, 19)); // NAACCR version
        Assert.assertEquals("0000000001", lines.get(0).substring(29, 39)); // registry ID
        Assert.assertEquals("00000001", lines.get(0).substring(41, 49)); // patient ID
        Assert.assertEquals("C123", lines.get(0).substring(539, 543)); // primary site
        Assert.assertEquals("I", lines.get(1).substring(0, 1)); // record type
        Assert.assertEquals("150", lines.get(1).substring(16, 19)); // NAACCR version
        Assert.assertEquals("0000000001", lines.get(1).substring(29, 39)); // registry ID
        Assert.assertEquals("00000001", lines.get(1).substring(41, 49)); // patient ID
        Assert.assertEquals("C456", lines.get(1).substring(539, 543)); // primary site

        // two patients with one tumor each
        file = TestingUtils.createFile("test-flat-writer-two-patients.txt");
        writer = new PatientFlatWriter(new FileWriter(file), data, null, null);
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
        lines = TestingUtils.readFile(file);
        Assert.assertEquals(2, lines.size());
        Assert.assertEquals("I", lines.get(0).substring(0, 1)); // record type
        Assert.assertEquals("150", lines.get(0).substring(16, 19)); // NAACCR version
        Assert.assertEquals("0000000001", lines.get(0).substring(29, 39)); // registry ID
        Assert.assertEquals("00000001", lines.get(0).substring(41, 49)); // patient ID
        Assert.assertEquals("C123", lines.get(0).substring(539, 543)); // primary site
        Assert.assertEquals("I", lines.get(1).substring(0, 1)); // record type
        Assert.assertEquals("150", lines.get(1).substring(16, 19)); // NAACCR version
        Assert.assertEquals("0000000001", lines.get(1).substring(29, 39)); // registry ID
        Assert.assertEquals("00000002", lines.get(1).substring(41, 49)); // patient ID
        Assert.assertEquals("C456", lines.get(1).substring(539, 543)); // primary site
    }

    @Test
    public void testValuesWithNewLines() throws IOException {

        // create the root data
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_15_INCIDENCE);
        data.getItems().add(new Item("registryId", null, "0000000001"));

        File file = TestingUtils.createFile("test-flat-writer-values-with-new-lines.txt");
        PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), data, null, null);
        Patient patient = new Patient();
        patient.getItems().add(new Item("patientIdNumber", null, "000000\r\n1"));
        writer.writePatient(patient);
        writer.close();
        List<String> lines = TestingUtils.readFile(file);
        Assert.assertEquals(1, lines.size());
        // new line should have been replaced by a space...
        Assert.assertEquals("000000 1", lines.get(0).substring(41, 49)); // patient ID
    }
}
