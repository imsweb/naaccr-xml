/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public class NaaccrXmlUtilsTest {

    @Test
    public void testFlatToXml() throws IOException {
        File xmlFile = new File(System.getProperty("user.dir") + "/build/test.xml");

        // it's not great to use another write method for testing this one, but it's convenient, so whatever...
        NaaccrData data = NaaccrXmlUtils.readXmlFile(new File(System.getProperty("user.dir") + "/src/test/resources/data/standard-file.xml"), null, null, null);
        File flatFile = new File(System.getProperty("user.dir") + "/build/test.txt");
        NaaccrXmlUtils.writeFlatFile(data, flatFile, null, null, null);

        // convert flat to XML; make sure the conversion worked by reading the data back
        NaaccrXmlUtils.flatToXml(flatFile, xmlFile, null, null, null);
        NaaccrData data2 = NaaccrXmlUtils.readXmlFile(xmlFile, null, null, null);
        Assert.assertEquals(data.getBaseDictionaryUri(), data2.getBaseDictionaryUri());
        Assert.assertEquals(data.getPatients().size(), data2.getPatients().size());

        // same test, but use an option; make sure the item numbers are written
        NaaccrOptions options = new NaaccrOptions();
        options.setWriteItemNumber(true);
        Assert.assertFalse(TestingUtils.readFileAsOneString(xmlFile).contains("naaccrNum="));
        NaaccrXmlUtils.flatToXml(flatFile, xmlFile, options, null, null);
        Assert.assertTrue(TestingUtils.readFileAsOneString(xmlFile).contains("naaccrNum="));

        // same test, but use a user-defined dictionary (we have to re-write the flat-file to use the extra variable)
        NaaccrDictionary dict = TestingUtils.createUserDictionary();
        data.getPatients().get(0).getTumors().get(0).addItem(new Item("myVariable", "01"));
        NaaccrXmlUtils.writeFlatFile(data, flatFile, null, dict, null);
        NaaccrXmlUtils.flatToXml(flatFile, xmlFile, null, dict, null);
        Assert.assertTrue(TestingUtils.readFileAsOneString(xmlFile).contains("myVariable"));
    }

    @Test
    public void testXmlToFlat() throws IOException {
        File flatFile = new File(System.getProperty("user.dir") + "/build/test.txt");

        // it's not great to use another write method for testing this one, but it's convenient, so whatever...
        NaaccrData data = NaaccrXmlUtils.readXmlFile(new File(System.getProperty("user.dir") + "/src/test/resources/data/standard-file.xml"), null, null, null);
        File xmlFile = new File(System.getProperty("user.dir") + "/build/test.xml");
        NaaccrXmlUtils.writeXmlFile(data, xmlFile, null, null, null);

        // convert XML to flat; make sure the conversion worked by reading the data back
        NaaccrXmlUtils.xmlToFlat(xmlFile, flatFile, null, null, null);
        NaaccrData data2 = NaaccrXmlUtils.readFlatFile(flatFile, null, null, null);
        Assert.assertEquals(data.getBaseDictionaryUri(), data2.getBaseDictionaryUri());
        Assert.assertEquals(data.getPatients().size(), data2.getPatients().size());

        // same test, but use an option; make sure the item numbers are written
        Assert.assertTrue(TestingUtils.readFileAsOneString(flatFile).contains("C123"));
        NaaccrOptions options = new NaaccrOptions();
        options.setItemsToExclude(Collections.singletonList("primarySite"));
        NaaccrXmlUtils.xmlToFlat(xmlFile, flatFile, options, null, null);
        Assert.assertFalse(TestingUtils.readFileAsOneString(flatFile).contains("C123"));
        options.setItemsToInclude(Collections.singletonList("primarySite"));
        NaaccrXmlUtils.xmlToFlat(xmlFile, flatFile, options, null, null);
        Assert.assertTrue(TestingUtils.readFileAsOneString(flatFile).contains("C123"));

        // same test, but use a user-defined dictionary (we have to re-write the xml-file to use the extra variable)
        NaaccrDictionary dict = TestingUtils.createUserDictionary();
        data.getPatients().get(0).getTumors().get(0).addItem(new Item("myVariable", "01"));
        NaaccrXmlUtils.writeXmlFile(data, xmlFile, null, dict, null);
        NaaccrXmlUtils.xmlToFlat(xmlFile, flatFile, null, dict, null);
        Assert.assertTrue(TestingUtils.readFileAsOneString(xmlFile).contains("01"));
    }

    @Test
    public void testReadXmlFile() throws IOException {
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/data/standard-file.xml");

        // get the format from the file (not necessary, one could hard-code it in the reading call)
        String format = NaaccrXmlUtils.getFormatFromXmlFile(file);
        Assert.assertNotNull(format);

        // read the entire file at once
        NaaccrData data = NaaccrXmlUtils.readXmlFile(file, null, null, null);
        Assert.assertNotNull(data.getBaseDictionaryUri());
        Assert.assertNull(data.getUserDictionaryUri());
        Assert.assertNotNull(data.getRecordType());
        Assert.assertNotNull(data.getTimeGenerated());
        Assert.assertEquals(1, data.getItems().size());
        Assert.assertEquals(2, data.getPatients().size());

        // read the file using a stream
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(file), null, null, null)) {
            data = reader.getRootData();
            Assert.assertNotNull(data.getBaseDictionaryUri());
            Assert.assertNull(data.getUserDictionaryUri());
            Assert.assertNotNull(data.getRecordType());
            Assert.assertNotNull(data.getTimeGenerated());
            Assert.assertEquals(1, data.getItems().size());
            Assert.assertEquals(0, data.getPatients().size()); // this one is important, patients are not read right away in a stream!
            List<Patient> patients = new ArrayList<>();
            Patient patient = reader.readPatient();
            while (patient != null) {
                patients.add(patient);
                patient = reader.readPatient();
            }
            Assert.assertEquals(2, patients.size());
        }
    }

    @Test
    public void testWriteXmlFile() throws IOException {
        NaaccrData data = new NaaccrData();
        data.setBaseDictionaryUri(NaaccrXmlDictionaryUtils.createUriFromVersion("140", true));
        data.setRecordType("I");
        data.setTimeGenerated(new Date());
        data.addItem(new Item("vendorName", "VENDOR"));
        Patient patient1 = new Patient();
        patient1.addItem(new Item("patientIdNumber", null, "00000001", null));
        Tumor tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C123"));
        patient1.addTumor(tumor1);
        data.addPatient(patient1);
        Patient patient2 = new Patient();
        patient2.addItem(new Item("patientIdNumber", "00000002"));
        data.addPatient(patient2);

        // write the entire file at once
        File file = new File(System.getProperty("user.dir") + "/build/test-writing-1.xml");
        NaaccrXmlUtils.writeXmlFile(data, file, null, null, null);

        // write the file using a steam
        file = new File(System.getProperty("user.dir") + "/build/test-writing-2.xml");
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, null)) {
            for (Patient patient : data.getPatients())
                writer.writePatient(patient);
        }
    }

    @Test
    public void testGetFormatFromFlatFile() throws IOException {

        // regular file
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/data/fake-naaccr14inc-1-rec.txt");
        Assert.assertEquals(NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, NaaccrXmlUtils.getFormatFromFlatFile(file));

        // not a valid file
        file = new File(System.getProperty("user.dir") + "/src/test/resources/data/standard-file.xml");
        Assert.assertNull(NaaccrXmlUtils.getFormatFromFlatFile(file));
    }

    @Test
    public void testGetFormatFromXmlFile() throws IOException {

        // regular file
        File file1 = new File(System.getProperty("user.dir") + "/src/test/resources/data/standard-file.xml");
        Assert.assertEquals(NaaccrFormat.NAACCR_FORMAT_16_INCIDENCE, NaaccrXmlUtils.getFormatFromXmlFile(file1));

        // this one contains extensions
        File file2 = new File(System.getProperty("user.dir") + "/src/test/resources/data/standard-file-extension.xml");
        Assert.assertEquals(NaaccrFormat.NAACCR_FORMAT_16_INCIDENCE, NaaccrXmlUtils.getFormatFromXmlFile(file2));

        Files.newDirectoryStream(Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "data", "validity", "valid")).forEach(path ->
                Assert.assertNotNull(path.toString(), NaaccrXmlUtils.getFormatFromXmlFile(path.toFile())));

    }

    @Test
    public void testGetAttributesFromXmlFile() {

        // a regular file which includes an extra attribute
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/data/read-attributes-1.xml");
        Map<String, String> attr = NaaccrXmlUtils.getAttributesFromXmlFile(file);
        Assert.assertEquals("http://naaccr.org/naaccrxml/naaccr-dictionary-160.xml", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT));
        Assert.assertNull(attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT));
        Assert.assertEquals("I", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE));
        Assert.assertEquals("2015-03-13T12:09:19.0Z", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED));
        Assert.assertEquals("whatever", attr.get("myOwnExtraAttribute"));

        // another good file with less attributes
        file = new File(System.getProperty("user.dir") + "/src/test/resources/data/read-attributes-2.xml");
        attr = NaaccrXmlUtils.getAttributesFromXmlFile(file);
        Assert.assertEquals("http://naaccr.org/naaccrxml/naaccr-dictionary-160.xml", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT));
        Assert.assertEquals("I", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE));

        // a bad file (missing required attributes)
        file = new File(System.getProperty("user.dir") + "/src/test/resources/data/read-attributes-3.xml");
        Assert.assertTrue(NaaccrXmlUtils.getAttributesFromXmlFile(file).isEmpty());

        // a complete garbage file
        file = new File(System.getProperty("user.dir") + "/src/test/resources/data/read-attributes-4.xml");
        Assert.assertTrue(NaaccrXmlUtils.getAttributesFromXmlFile(file).isEmpty());
    }

    @Test
    public void testGetAttributesFromXmlReader() throws IOException {
        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(false);

        // peek at the attributes using a reader that does support marking
        InputStreamReader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/read-attributes-1.xml"), StandardCharsets.UTF_8);
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            Assert.assertFalse(NaaccrXmlUtils.getAttributesFromXmlReader(bufferedReader).isEmpty());
            // at this point, we should still be able to consume the file (which contains a single patient)
            try (PatientXmlReader xmlReader = new PatientXmlReader(bufferedReader, options, null)) {
                Assert.assertNotNull(xmlReader.readPatient());
                Assert.assertNull(xmlReader.readPatient());
            }
        }

        // peek at the attributes using a reader that doesn't support marking
        try (Reader reader2 = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/read-attributes-1.xml"), StandardCharsets.UTF_8)) {
            Assert.assertFalse(NaaccrXmlUtils.getAttributesFromXmlReader(reader2).isEmpty());
            // at this point, we should't be able to consume the data anymore
            try {
                new PatientXmlReader(reader2, options, null);
                Assert.fail("There should have been an exception!");
            }
            catch (Exception e) {
                // ignored, expected
            }
        }
    }
}
