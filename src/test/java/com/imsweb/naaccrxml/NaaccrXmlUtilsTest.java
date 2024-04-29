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
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
    public void testXmlToXml() throws IOException {
        File sourceXml = TestingUtils.getDataFile("standard-file.xml");
        File targetXml = new File(TestingUtils.getWorkingDirectory() + "/build/test.xml");

        // create a "processor" that sets the ICD revision number to 0
        NaaccrPatientProcessor processor = patient -> patient.addItem(new Item("icdRevisionNumber", "0"));

        // make sure the ICD revision number is not 0 before processing the file
        List<Patient> patients = NaaccrXmlUtils.readXmlFile(sourceXml, null, null, null).getPatients();
        Assert.assertEquals(2, patients.size());
        Assert.assertNotEquals("0", patients.get(0).getItemValue("icdRevisionNumber"));
        Assert.assertNotEquals("0", patients.get(1).getItemValue("icdRevisionNumber"));

        // process the file
        NaaccrXmlUtils.xmlToXml(sourceXml, targetXml, processor, null, null, null);

        // make sure the ICD revision number is 0 after processing the file
        patients = NaaccrXmlUtils.readXmlFile(targetXml, null, null, null).getPatients();
        Assert.assertEquals(2, patients.size());
        Assert.assertEquals("0", patients.get(0).getItemValue("icdRevisionNumber"));
        Assert.assertEquals("0", patients.get(1).getItemValue("icdRevisionNumber"));
    }

    @Test
    public void testFlatToXml() throws IOException {
        File xmlFile = new File(TestingUtils.getWorkingDirectory() + "/build/test.xml");

        // it's not great to use another write method for testing this one, but it's convenient, so whatever...
        NaaccrData data = NaaccrXmlUtils.readXmlFile(new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/standard-file.xml"), null, null, null);
        File flatFile = new File(TestingUtils.getWorkingDirectory() + "/build/test.txt");
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
        NaaccrXmlUtils.writeFlatFile(data, flatFile, null, Collections.singletonList(dict), null);
        NaaccrXmlUtils.flatToXml(flatFile, xmlFile, null, Collections.singletonList(dict), null);
        Assert.assertTrue(TestingUtils.readFileAsOneString(xmlFile).contains("myVariable"));
    }

    @Test
    public void testXmlToFlat() throws IOException {
        File flatFile = new File(TestingUtils.getWorkingDirectory() + "/build/test.txt");

        // it's not great to use another write method for testing this one, but it's convenient, so whatever...
        NaaccrData data = NaaccrXmlUtils.readXmlFile(new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/standard-file.xml"), null, null, null);
        File xmlFile = new File(TestingUtils.getWorkingDirectory() + "/build/test.xml");
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
        NaaccrXmlUtils.writeXmlFile(data, xmlFile, null, Collections.singletonList(dict), null);
        NaaccrXmlUtils.xmlToFlat(xmlFile, flatFile, null, Collections.singletonList(dict), null);
        Assert.assertTrue(TestingUtils.readFileAsOneString(xmlFile).contains("01"));
    }

    @Test
    public void testReadXmlFile() throws IOException {
        File file = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/standard-file.xml");

        // get the format from the file (not necessary, one could hard-code it in the reading call)
        String format = NaaccrXmlUtils.getFormatFromXmlFile(file);
        Assert.assertNotNull(format);

        // read the entire file at once
        NaaccrData data = NaaccrXmlUtils.readXmlFile(file, null, null, null);
        Assert.assertNotNull(data.getBaseDictionaryUri());
        Assert.assertTrue(data.getUserDictionaryUri().isEmpty());
        Assert.assertNotNull(data.getRecordType());
        Assert.assertNotNull(data.getTimeGenerated());
        Assert.assertEquals(1, data.getItems().size());
        Assert.assertEquals(2, data.getPatients().size());

        // read the file using a stream
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(file))) {
            data = reader.getRootData();
            Assert.assertNotNull(data.getBaseDictionaryUri());
            Assert.assertTrue(data.getUserDictionaryUri().isEmpty());
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
        File file = new File(TestingUtils.getWorkingDirectory() + "/build/test-writing-1.xml");
        NaaccrXmlUtils.writeXmlFile(data, file, null, null, null);
        Assert.assertTrue(file.exists());

        // write the file using a steam
        file = new File(TestingUtils.getWorkingDirectory() + "/build/test-writing-2.xml");
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data)) {
            for (Patient patient : data.getPatients())
                writer.writePatient(patient);
        }
        Assert.assertTrue(file.exists());
    }

    @Test
    public void testLineToPatient() throws IOException {
        StringBuilder line = TestingUtils.createEmptyRecord("160", "A", "00000001");

        NaaccrContext context = new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
        Patient patient = NaaccrXmlUtils.lineToPatient(line.toString(), context);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));

        List<NaaccrDictionary> dictionaries = Collections.singletonList(TestingUtils.createUserDictionary());
        line.replace(2339, 2341, "00");
        context = new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT, dictionaries);
        patient = NaaccrXmlUtils.lineToPatient(line.toString(), context);
        Assert.assertEquals("00", patient.getTumors().get(0).getItemValue("myVariable"));

        NaaccrOptions options = NaaccrOptions.getDefault();
        options.setItemsToExclude(Collections.singletonList("patientIdNumber"));
        context = new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT, dictionaries, options);
        patient = NaaccrXmlUtils.lineToPatient(line.toString(), context);
        Assert.assertNull(patient.getItemValue("patientIdNumber"));

        line.replace(0, 20, "                    ");
        context = new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
        patient = NaaccrXmlUtils.lineToPatient(line.toString(), context);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
    }

    @Test
    public void testPatientToLine() throws IOException {
        Patient patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));

        NaaccrContext context = new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
        String line = NaaccrXmlUtils.patientToLine(patient, context);
        Assert.assertEquals("00000001", line.substring(41, 49));

        List<NaaccrDictionary> dictionaries = Collections.singletonList(TestingUtils.createUserDictionary());
        Tumor tumor = new Tumor();
        tumor.addItem(new Item("myVariable", "00"));
        patient.setTumors(Collections.singletonList(tumor));
        context = new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT, dictionaries);
        line = NaaccrXmlUtils.patientToLine(patient, context);
        Assert.assertEquals("00", line.substring(2339, 2341));

        NaaccrOptions options = NaaccrOptions.getDefault();
        options.setItemsToExclude(Collections.singletonList("patientIdNumber"));
        context = new NaaccrContext(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT, dictionaries, options);
        line = NaaccrXmlUtils.patientToLine(patient, context);
        Assert.assertEquals("        ", line.substring(41, 49));
    }

    @Test
    public void testGetFormatFromFlatFile() {

        // regular file
        File file = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/fake-naaccr14inc-1-rec.txt");
        Assert.assertEquals(NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, NaaccrXmlUtils.getFormatFromFlatFile(file));

        // not a valid file
        file = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/standard-file.xml");
        Assert.assertNull(NaaccrXmlUtils.getFormatFromFlatFile(file));
    }

    @Test
    @SuppressWarnings("resource")
    public void testGetFormatFromXmlFile() throws IOException {

        // regular file
        File file1 = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/standard-file.xml");
        Assert.assertEquals(NaaccrFormat.NAACCR_FORMAT_16_INCIDENCE, NaaccrXmlUtils.getFormatFromXmlFile(file1));

        // this one contains extensions
        File file2 = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/standard-file-extension.xml");
        Assert.assertEquals(NaaccrFormat.NAACCR_FORMAT_16_INCIDENCE, NaaccrXmlUtils.getFormatFromXmlFile(file2));

        Files.newDirectoryStream(Paths.get(TestingUtils.getWorkingDirectory(), "src", "test", "resources", "data", "validity", "valid")).forEach(path ->
                Assert.assertNotNull(path.toString(), NaaccrXmlUtils.getFormatFromXmlFile(path.toFile())));

    }

    @Test
    public void testGetAttributesFromXmlFile() {

        // a regular file which includes an extra attribute
        File file = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/read-attributes-1.xml");
        Map<String, String> attr = NaaccrXmlUtils.getAttributesFromXmlFile(file);
        Assert.assertEquals("http://naaccr.org/naaccrxml/naaccr-dictionary-160.xml", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT));
        Assert.assertNull(attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_USER_DICT));
        Assert.assertEquals("I", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE));
        Assert.assertEquals("2015-03-13T12:09:19.0Z", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_TIME_GENERATED));
        Assert.assertEquals("whatever", attr.get("myOwnExtraAttribute"));

        // another good file with less attributes
        file = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/read-attributes-2.xml");
        attr = NaaccrXmlUtils.getAttributesFromXmlFile(file);
        Assert.assertEquals("http://naaccr.org/naaccrxml/naaccr-dictionary-160.xml", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_BASE_DICT));
        Assert.assertEquals("I", attr.get(NaaccrXmlUtils.NAACCR_XML_ROOT_ATT_REC_TYPE));

        // a bad file (missing required attributes)
        file = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/read-attributes-3.xml");
        Assert.assertTrue(NaaccrXmlUtils.getAttributesFromXmlFile(file).isEmpty());

        // a complete garbage file
        file = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/read-attributes-4.xml");
        Assert.assertTrue(NaaccrXmlUtils.getAttributesFromXmlFile(file).isEmpty());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testGetAttributesFromXmlReader() throws IOException {
        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(false);

        // peek at the attributes using a reader that does support marking
        InputStreamReader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/read-attributes-1.xml"), StandardCharsets.UTF_8);
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            Assert.assertFalse(NaaccrXmlUtils.getAttributesFromXmlReader(bufferedReader).isEmpty());
            // at this point, we should still be able to consume the file (which contains a single patient)
            try (PatientXmlReader xmlReader = new PatientXmlReader(bufferedReader, options)) {
                Assert.assertNotNull(xmlReader.readPatient());
                Assert.assertNull(xmlReader.readPatient());
            }
        }

        // peek at the attributes using a reader that doesn't support marking
        try (Reader reader2 = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/read-attributes-1.xml"), StandardCharsets.UTF_8)) {
            Assert.assertFalse(NaaccrXmlUtils.getAttributesFromXmlReader(reader2).isEmpty());
            // at this point, we shouldn't be able to consume the data anymore
            try {
                new PatientXmlReader(reader2, options);
                Assert.fail("There should have been an exception!");
            }
            catch (Exception e) {
                // ignored, expected
            }
        }
    }

    @Test
    public void testParseIso8601Date() {
        // following examples are from http://books.xmlschemata.org/relaxng/ch19-77049.html

        assertValidDateValue("2001-10-26T21:32:52");
        assertValidDateValue("2001-10-26T21:32:52+02:00");
        assertValidDateValue("2001-10-26T19:32:52Z");
        assertValidDateValue("2001-10-26T19:32:52+00:00");
        assertValidDateValue("-2001-10-26T21:32:52");
        assertValidDateValue("2001-10-26T21:32:52.12679");

        assertInvalidDateValue("2001-10-26");
        assertInvalidDateValue("2001-10-26T21:32");
        assertInvalidDateValue("2001-10-26T25:32:52+02:00");
        assertInvalidDateValue("01-10-26T21:32");
        assertInvalidDateValue("2001-10-26T21:32:52+2:00");
    }

    private void assertValidDateValue(String dateValue) {
        try {
            ZonedDateTime.parse(dateValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        catch (RuntimeException e1) {
            try {
                LocalDateTime.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            catch (RuntimeException e2) {
                Assert.fail("Value should be valid, but isn't: " + dateValue);
            }
        }
    }

    private void assertInvalidDateValue(String dateValue) {
        try {
            ZonedDateTime.parse(dateValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            LocalDateTime.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        catch (RuntimeException e) {
            return;
        }
        Assert.fail("Value should be invalid, but isn't: " + dateValue);
    }

    @Test
    public void testWriteIso8601Date() {
        Assert.assertNotNull(NaaccrXmlUtils.formatIso8601Date(new Date()));
    }
}
