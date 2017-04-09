package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public class PatientXmlWriterTest {

    @Test
    public void testWriter() throws IOException {

        // create the root data
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
        data.addItem(new Item("registryId", "0000000001"));

        // a patient with no tumor
        File file = TestingUtils.createFile("test-xml-writer-no-tumor.xml");
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data)) {
            Patient patient = new Patient();
            patient.addItem(new Item("patientIdNumber", "00000001"));
            writer.writePatient(patient);
        }
        Patient patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        String xmlAsString = TestingUtils.readFileAsOneString(file);
        Assert.assertTrue(xmlAsString.contains("specificationVersion=\"" + NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION + "\""));
        Assert.assertTrue(xmlAsString.contains("timeGenerated="));

        // a patient with one tumor
        file = TestingUtils.createFile("test-flat-writer-one-tumor.txt");
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data)) {
            patient = new Patient();
            patient.addItem(new Item("patientIdNumber", "00000001"));
            Tumor tumor1 = new Tumor();
            tumor1.addItem(new Item("primarySite", "C123"));
            patient.addTumor(tumor1);
            writer.writePatient(patient);
        }
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("C123", patient.getTumors().get(0).getItemValue("primarySite"));

        // a patient with two tumors
        file = TestingUtils.createFile("test-flat-writer-two-tumors.txt");
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data)) {
            patient = new Patient();
            patient.addItem(new Item("patientIdNumber", "00000001"));
            Tumor tumor1 = new Tumor();
            tumor1.addItem(new Item("primarySite", "C123"));
            patient.addTumor(tumor1);
            tumor1 = new Tumor();
            tumor1.addItem(new Item("primarySite", "C456"));
            patient.addTumor(tumor1);
            writer.writePatient(patient);
        }
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("C123", patient.getTumors().get(0).getItemValue("primarySite"));
        Assert.assertEquals("C456", patient.getTumors().get(1).getItemValue("primarySite"));

        // two patients with one tumor each
        file = TestingUtils.createFile("test-flat-writer-two-patients.txt");
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data)) {
            Patient patient1 = new Patient();
            patient1.addItem(new Item("patientIdNumber", "00000001"));
            Tumor tumor1 = new Tumor();
            tumor1.addItem(new Item("primarySite", "C123"));
            patient1.addTumor(tumor1);
            writer.writePatient(patient1);
            Patient patient2 = new Patient();
            patient2.addItem(new Item("patientIdNumber", "00000002"));
            Tumor tumor2 = new Tumor();
            tumor2.addItem(new Item("primarySite", "C456"));
            patient2.addTumor(tumor2);
            writer.writePatient(patient2);
        }
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("C123", patient.getTumors().get(0).getItemValue("primarySite"));
        patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(1);
        Assert.assertEquals("00000002", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("C456", patient.getTumors().get(0).getItemValue("primarySite"));

        // test some special characters
        file = TestingUtils.createFile("test-xml-writer-special-chars.xml");
        String val =
                "\nFollowing characters should be translated:\n<\n>\n\"\n'\n&\n\nFollowing characters should appear as-is:\n~\n@\n#\n%\n^\n*\n()\n{}\n[]\n,\n;\n.\n|\n\\\n/\n`\n\nFollowing characters are the few controls characters allowed in XML 1.0 (not visible):\n\t\n\r\n\nFollowing characters are not valid and should be ignored:\n\u0000\n\u001C\n\nFollowing characters are valid:\n\u0009\n\u0040\n";
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data)) {
            patient = new Patient();
            patient.addItem(new Item("patientIdNumber", "00000001"));
            Tumor tumor = new Tumor();
            tumor.addItem(new Item("rxTextSurgery", val));
            patient.addTumor(tumor);
            writer.writePatient(patient);
        }
        xmlAsString = TestingUtils.readFileAsOneString(file);
        Assert.assertTrue(xmlAsString.contains("&lt;"));
        Assert.assertTrue(xmlAsString.contains("&gt;"));
        Assert.assertTrue(xmlAsString.contains("&quot;"));
        Assert.assertTrue(xmlAsString.contains("&apos;"));
        Assert.assertTrue(xmlAsString.contains("&amp;"));
        Assert.assertTrue(xmlAsString.contains("~"));
        Assert.assertTrue(xmlAsString.contains("`"));
        // no need to test all of them...

        // same test, but fail on bad characters
        file = TestingUtils.createFile("test-xml-writer-special-chars-error.xml");
        NaaccrOptions options = new NaaccrOptions();
        options.setIgnoreControlCharacters(false);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options)) {
            patient = new Patient();
            patient.addItem(new Item("patientIdNumber", "00000001"));
            Tumor tumor = new Tumor();
            tumor.addItem(new Item("rxTextSurgery", val));
            patient.addTumor(tumor);
            writer.writePatient(patient);
            Assert.fail("An exception should have been thrown");
        }
        catch (NaaccrIOException e) {
            // expected
        }
    }

    @Test
    public void testOptions() throws IOException {

        NaaccrOptions options = new NaaccrOptions();

        NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(TestingUtils.getDataFile("dictionary/testing-user-dictionary.xml"));

        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_15_INCIDENCE);
        Patient patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));
        Tumor tumor = new Tumor();
        patient.addItem(new Item("myVariable2", "XX"));
        patient.addTumor(tumor);
        data.addPatient(patient);

        File file = TestingUtils.createFile("test-xml-writer-options.xml");

        // value is too long, option says to report the error (it should still be truncated)
        options.setReportValuesTooLong(true);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        Assert.assertFalse(TestingUtils.readFileAsOneString(file).contains("XX"));
        Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("X"));
        Assert.assertFalse(patient.getAllValidationErrors().isEmpty());
        Assert.assertTrue(patient.getAllValidationErrors().get(0).getCode().equals(NaaccrErrorUtils.CODE_VAL_TOO_LONG));
        patient.getAllValidationErrors().clear();

        // value is too long, options says to ignore the error (it should still be truncated)
        options.setReportValuesTooLong(false);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        Assert.assertFalse(TestingUtils.readFileAsOneString(file).contains("XX"));
        Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("X"));
        Assert.assertFalse(patient.getAllValidationErrors().isEmpty());

        // value is too long, but the field is flagged as unlimited text -> no error, not truncated
        options.setReportValuesTooLong(true);
        patient.addItem(new Item("myVariable6", "YY"));
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("YY"));
        Assert.assertFalse(patient.getAllValidationErrors().isEmpty());

        // option is set to pad the values
        options.setApplyPaddingRules(true);
        data.addItem(new Item("npiRegistryId", "1"));
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("0000000001"));

        // same test, but option is set to NOT pad the values
        options.setApplyPaddingRules(false);
        data.addItem(new Item("npiRegistryId", "1"));
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("1"));
        Assert.assertFalse(TestingUtils.readFileAsOneString(file).contains("0000000001"));
    }

    @Test
    public void testUserDefinedDictionary() throws IOException {
        NaaccrDictionary dict = TestingUtils.createUserDictionary();

        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_16_INCIDENCE);
        Patient patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));
        Tumor tumor = new Tumor();
        patient.addItem(new Item("myVariable", "02"));
        patient.addTumor(tumor);
        data.addPatient(patient);

        File file = TestingUtils.createFile("test-xml-writer-user-dict.xml");

        // user dictionary is not referenced -> error
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data)) {
            writer.writePatient(patient);
            Assert.fail("Was expecting an exception...");
        }
        catch (NaaccrIOException e) {
            // expected
        }

        // user dictionary is referenced
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, dict)) {
            writer.writePatient(patient);
        }
        Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("02"));
        Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains(dict.getDictionaryUri()));

        // root data is providing a different dictionary -> error
        data.setUserDictionaryUri(Collections.singletonList("something-else"));
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, dict)) {
            Assert.fail("Was expecting an exception...");
        }
        catch (NaaccrIOException e) {
            // expected
        }
    }

    @Test
    public void testZipFile() throws IOException {

        File file = TestingUtils.createFile("test-xml-writer.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            for (int i = 1; i <= 5; i++) {
                zos.putNextEntry(new ZipEntry("test-file-" + i + ".xml"));

                // we can't use a try-with-resource since it would call close, but we have to call closeAndKeepAlive!
                PatientXmlWriter writer = null;
                try {
                    writer = new PatientXmlWriter(new OutputStreamWriter(zos), new NaaccrData(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT));
                    Patient patient = new Patient();
                    patient.addItem(new Item("patientIdNumber", "0000000" + i));
                    writer.writePatient(patient);
                }
                finally {
                    if (writer != null)
                        writer.closeAndKeepAlive();
                }
            }
        }
    }
}
