/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public class PatientFlatWriterTest {

    @Test
    public void testWriter() throws IOException {

        // create the root data
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_15_INCIDENCE);
        data.addItem(new Item("registryId", "0000000001"));

        // a patient with no tumor
        File file = TestingUtils.createFile("test-flat-writer-no-tumor.txt");
        PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), data, null, null);
        Patient patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));
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
        patient.addItem(new Item("patientIdNumber", "00000001"));
        Tumor tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C123"));
        patient.addTumor(tumor1);
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
        patient.addItem(new Item("patientIdNumber", "00000001"));
        tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C123"));
        patient.addTumor(tumor1);
        tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C456"));
        patient.addTumor(tumor1);
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
    public void testUserDefinedDictionary() throws IOException {

        // we will use this root data (it's only used to initialize the reader, so the patient doesn't need to be added to it...)
        NaaccrData root = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_15_ABSTRACT);

        // we will use this patient
        Patient patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));

        // we will write this file
        File file = TestingUtils.createFile("test-flat-writer-user-dict.xml");

        // first, let's use the default user dictionary (so null)
        try (PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), root, null, null)) {
            writer.writePatient(patient);
            Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("00000001"));
        }

        // the result should be the same if we pass the same default dictionary to the method
        NaaccrDictionary dict = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_150);
        try (PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), root, null, dict)) {
            writer.writePatient(patient);
            Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("00000001"));
        }

        // using a dictionary with items that are not tied to a start location; those should be ignored...
        try (Reader dictReader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/testing-user-dictionary.xml"))) {
            dict = NaaccrXmlDictionaryUtils.readDictionary(dictReader);
            try (PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), root, null, dict)) {
                writer.writePatient(patient);
                Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("00000001"));
            }
            
            // using the same dictionary, define a value too long for a field that is supposed to be 1 long; option say to not report errors -> truncated
            patient.addItem(new Item("myVariable5", "XX"));
            try (PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), root, null, dict)) {
                writer.writePatient(patient);
                Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("X"));
                Assert.assertFalse(TestingUtils.readFileAsOneString(file).contains("XX"));
            }

            // using the same dictionary, define a value too long for a field that is supposed to be 1 long; option say to report errors -> validation error
            NaaccrOptions options = new NaaccrOptions();
            options.setReportValuesTooLong(true);
            try (PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), root, options, dict)) {
                writer.writePatient(patient);
                Assert.assertFalse(patient.getValidationErrors().isEmpty());
                Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("X"));
                Assert.assertFalse(TestingUtils.readFileAsOneString(file).contains("XX"));
            }

            // using the same dictionary, define a value too long for a field that is supposed to be 1 long but it supports unlimited text -> truncated
            patient.addItem(new Item("myVariable6", "YY"));
            try (PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), root, options, dict)) {
                writer.writePatient(patient);
                Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("Y"));
                Assert.assertFalse(TestingUtils.readFileAsOneString(file).contains("YY"));
            }
        }
    }

    @Test
    public void testValuesWithNewLines() throws IOException {

        // create the root data
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_15_INCIDENCE);
        data.addItem(new Item("registryId", "0000000001"));

        File file = TestingUtils.createFile("test-flat-writer-values-with-new-lines.txt");
        PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), data, null, null);
        Patient patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "000000\r\n1"));
        writer.writePatient(patient);
        writer.close();
        List<String> lines = TestingUtils.readFile(file);
        Assert.assertEquals(1, lines.size());
        // new line should have been replaced by a space...
        Assert.assertEquals("000000 1", lines.get(0).substring(41, 49)); // patient ID
    }
}
