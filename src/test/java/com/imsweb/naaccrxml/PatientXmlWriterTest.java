package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Test;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;

public class PatientXmlWriterTest {

    @SuppressWarnings("UnnecessaryUnicodeEscape")
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
            patient.addItem(new Item("nameLast", "Smith < Wilson"));
            writer.writePatient(patient);
        }
        Patient patient = NaaccrXmlUtils.readXmlFile(file, null, null, null).getPatients().get(0);
        Assert.assertEquals("00000001", patient.getItemValue("patientIdNumber"));
        Assert.assertEquals("Smith < Wilson", patient.getItemValue("nameLast"));
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
            Tumor tumor2 = new Tumor();
            tumor2.addItem(new Item("primarySite", "C456"));
            patient.setTumors(Arrays.asList(tumor1, tumor2));
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

        // duplicate items should generate an exception
        try {
            data.addItem(new Item("registryId", "0000000002"));
            throw new AssertionError("Was expecting an exception here!");
        }
        catch (DuplicateItemException e) {
            Assert.assertEquals("registryId", e.getItemId());
            Assert.assertTrue(e.getMessage().contains("registryId"));
        }
    }

    @Test
    public void testCachedRuntimeDictionary() throws IOException {

        // we are going to need all the constructor params for this test...
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
        NaaccrOptions options = NaaccrOptions.getDefault();
        NaaccrStreamConfiguration conf = NaaccrStreamConfiguration.getDefault();
        NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(TestingUtils.getDataFile("dictionary/testing-user-dictionary.xml"));

        File file = TestingUtils.createFile("test-cached-runtime-dictionary.xml");

        // first, write the file once without a configuration
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict, null)) {
            Patient patient = new Patient();
            patient.addItem(new Item("patientIdNumber", "00000001"));
            writer.writePatient(patient);
        }
        String content = TestingUtils.readFileAsOneString(file);

        // then write the same file with the same data in a loop using a unique configuration (the runtime dictionary should be cached)
        for (int i = 0; i < 3; i++) {
            try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict, conf)) {
                Patient patient = new Patient();
                patient.addItem(new Item("patientIdNumber", "00000001"));
                writer.writePatient(patient);
            }
            Assert.assertEquals(content, TestingUtils.readFileAsOneString(file));
        }
        Assert.assertNotNull(conf.getCachedDictionary());
        String runtimeId = conf.getCachedDictionary().getId();

        // change the ID of the user dictionary -> should not use the caching anymore
        dict.setDictionaryUri("something-else");
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict, conf)) {
            Patient patient = new Patient();
            patient.addItem(new Item("patientIdNumber", "00000001"));
            writer.writePatient(patient);
        }
        Assert.assertNotEquals(content, TestingUtils.readFileAsOneString(file));
        Assert.assertNotNull(conf.getCachedDictionary());
        Assert.assertNotEquals(runtimeId, conf.getCachedDictionary().getId());

        // change the main version -> should not use the caching anymore
        data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_16_INCIDENCE);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict, conf)) {
            Patient patient = new Patient();
            patient.addItem(new Item("patientIdNumber", "00000001"));
            writer.writePatient(patient);
        }
        Assert.assertNotEquals(content, TestingUtils.readFileAsOneString(file));
        Assert.assertNotNull(conf.getCachedDictionary());
        Assert.assertNotEquals(runtimeId, conf.getCachedDictionary().getId());

    }

    @Test
    public void testOptions() throws IOException {

        NaaccrOptions options = new NaaccrOptions();

        NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(TestingUtils.getDataFile("dictionary/testing-user-dictionary.xml"));

        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_15_ABSTRACT);
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
        Assert.assertEquals(NaaccrErrorUtils.CODE_VAL_TOO_LONG, patient.getAllValidationErrors().get(0).getCode());
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
        options.setApplyZeroPaddingRules(true);
        // test a "leftZero" (registryId)
        data.addItem(new Item("registryId", "1"));
        // test a "rightZero" (comorbidComplication1)
        tumor.addItem(new Item("comorbidComplication1", "2"));
        // test a "leftBlank" (medicalRecordNumber)
        tumor.addItem(new Item("medicalRecordNumber", "3"));
        // test a "rightBlank" (nameLast) - this is the default
        tumor.addItem(new Item("primarySite", "4"));
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        String writtenContent = TestingUtils.readFileAsOneString(file);
        Assert.assertTrue(writtenContent.contains("<Item naaccrId=\"registryId\">0000000001</Item>"));
        Assert.assertTrue(writtenContent.contains("<Item naaccrId=\"comorbidComplication1\">20000</Item>"));
        Assert.assertTrue(writtenContent.contains("<Item naaccrId=\"medicalRecordNumber\">3</Item>")); // spaces are not taken into account!
        Assert.assertTrue(writtenContent.contains("<Item naaccrId=\"primarySite\">4</Item>")); // spaces are not taken into account!

        // only numeric values should be padded
        tumor.getItem("comorbidComplication1").setValue("X");
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        writtenContent = TestingUtils.readFileAsOneString(file);
        Assert.assertTrue(writtenContent.contains("<Item naaccrId=\"comorbidComplication1\">X</Item>"));
        tumor.getItem("comorbidComplication1").setValue("2");

        // same test, but option is set to NOT pad the values
        options.setApplyZeroPaddingRules(false);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        writtenContent = TestingUtils.readFileAsOneString(file);
        Assert.assertTrue(writtenContent.contains("<Item naaccrId=\"registryId\">1</Item>"));
        Assert.assertTrue(writtenContent.contains("<Item naaccrId=\"comorbidComplication1\">2</Item>"));
        Assert.assertTrue(writtenContent.contains("<Item naaccrId=\"medicalRecordNumber\">3</Item>"));
        Assert.assertTrue(writtenContent.contains("<Item naaccrId=\"primarySite\">4</Item>"));

        data.addItem(new Item("unknown", "1"));

        // unknown item - default option - should report an error
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
            throw new AssertionError("Was expecting an exception here!");
        }
        catch (NaaccrIOException e) {
            // expected
        }

        // unknown item - report error
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_ERROR);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
            throw new AssertionError("Was expecting an exception here!");
        }
        catch (NaaccrIOException e) {
            // expected
        }

        // unknown item - process
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_PROCESS);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        Assert.assertTrue(TestingUtils.readFileAsOneString(file).contains("<Item naaccrId=\"unknown\">1</Item>"));

        // unknown item - ignore
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_IGNORE);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options, dict)) {
            writer.writePatient(patient);
        }
        Assert.assertFalse(TestingUtils.readFileAsOneString(file).contains("<Item naaccrId=\"unknown\">1</Item>"));

        // **** test new lines
        data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_18_ABSTRACT);
        patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));
        Tumor tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C123"));
        patient.addTumor(tumor1);
        Tumor tumor2 = new Tumor();
        tumor2.addItem(new Item("primarySite", "C123"));
        patient.addTumor(tumor2);
        data.addPatient(patient);

        // force new lines to LF
        options.setNewLine(NaaccrOptions.NEW_LINE_LF);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options)) {
            writer.writePatient(patient);
            Assert.assertEquals("\n", writer.getNewLine());
        }
        writtenContent = TestingUtils.readFileAsOneString(file);
        Assert.assertFalse(writtenContent.contains("\r\n"));

        // force new lines to CRLF
        options.setNewLine(NaaccrOptions.NEW_LINE_CRLF);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, options)) {
            writer.writePatient(patient);
            Assert.assertEquals("\r\n", writer.getNewLine());
        }
        writtenContent = TestingUtils.readFileAsOneString(file);
        Assert.assertTrue(writtenContent.contains("\r\n"));

        // force new lines to CRLF
        options.setNewLine(NaaccrOptions.NEW_LINE_CRLF);
        try (PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(file), data, options)) {
            writer.writePatient(patient);
            Assert.assertEquals("\r\n", writer.getNewLine());
        }
        writtenContent = TestingUtils.readFileAsOneString(file);
        Assert.assertTrue(writtenContent.contains("\r\n"));
        Assert.assertEquals(2, writtenContent.split("\r\n").length);

        // test providing a specifications version
        options.setSpecificationVersionWritten("ZYZ");
        NaaccrXmlUtils.writeXmlFile(data, file, options, null, null);
        Assert.assertEquals(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION, NaaccrXmlUtils.readXmlFile(file, null, null, null).getSpecificationVersion());
        options.setSpecificationVersionWritten("1.5");
        NaaccrXmlUtils.writeXmlFile(data, file, options, null, null);
        Assert.assertEquals("1.5", NaaccrXmlUtils.readXmlFile(file, null, null, null).getSpecificationVersion());
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
        try (@SuppressWarnings("unused") PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, dict)) {
            Assert.fail("Was expecting an exception...");
        }
        catch (NaaccrIOException e) {
            // expected
        }
    }

    @Test
    public void testZipFile() throws IOException {

        File file = TestingUtils.createFile("test-xml-writer.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
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

    @Test
    public void testExtensions() throws IOException {

        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_22_INCIDENCE);
        data.addExtraRootParameters("other:myAttribute", "XXX");
        Patient patient = new Patient();
        patient.addItem(new Item("patientIdNumber", "00000001"));
        OuterTag tag = new OuterTag();
        tag.setInnerTag("INNER");
        tag.setSomeAttribute("XXX");
        patient.setExtensions(Collections.singletonList(tag));
        data.addPatient(patient);

        // to properly process extensions, we have to register them to the framework; this is done through a configuration object
        NaaccrStreamConfiguration conf = new NaaccrStreamConfiguration();
        conf.getXstream().autodetectAnnotations(true); // required only because we want to use annotation on the extension classes (it's more convenient)
        conf.registerNamespace("other", "http://whatever.org");
        conf.registerTag("other", "MyOuterTag", OuterTag.class);
        conf.registerAttribute("other", "someAttribute", OuterTag.class, "_someAttribute", String.class);

        File file = TestingUtils.createFile("standard-file-extension.xml");

        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, (NaaccrDictionary)null, conf)) {
            writer.writePatient(patient);
        }

        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(file), null, (NaaccrDictionary)null, conf)) {
            Patient p = reader.readPatient();
            Assert.assertNotNull(p.getExtensions());
        }
    }

    @SuppressWarnings("unused")
    @XStreamAlias("other:MyOuterTag")
    private static class OuterTag implements NaaccrXmlExtension {

        @XStreamOmitField
        private Integer _startLineNumber;

        @XStreamAlias("other:MyInnerTag")
        private String _innerTag;

        @XStreamAsAttribute
        private String _someAttribute;

        @Override
        public Integer getStartLineNumber() {
            return _startLineNumber;
        }

        @Override
        public void setStartLineNumber(Integer startLineNumber) {
            _startLineNumber = startLineNumber;
        }

        public String getInnerTag() {
            return _innerTag;
        }

        public void setInnerTag(String innerTag) {
            _innerTag = innerTag;
        }

        public String getSomeAttribute() {
            return _someAttribute;
        }

        public void setSomeAttribute(String someAttribute) {
            this._someAttribute = someAttribute;
        }
    }
}
