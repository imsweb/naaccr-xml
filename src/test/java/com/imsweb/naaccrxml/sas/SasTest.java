/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public class SasTest {

    @Test
    public void testNaaccrVersions() {

        // the SAS java code needs to validate the version, but it can't call the normal code, so I have to hard-code the supported versions...

        Set<String> formats = new HashSet<>(NaaccrFormat.getSupportedVersions());
        formats.remove("140");
        formats.remove("150");
        formats.remove("160");
        formats.remove("180");
        formats.remove("210");
        formats.remove("220");
        formats.remove("230");
        if (!formats.isEmpty())
            Assert.fail("Looks like a new format was added, please handle it in the SasXmlToCsv and SasCsvToXml constructors, and adjust this test: " + formats);

        //noinspection ConstantConditions
        if (!"1.6".equals(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION))
            Assert.fail("Current implementation changed, please adjust it in the SasCsvToXml code, then change this test!");
    }

    @Test
    public void testGetFields() throws IOException {
        Map<String, String> fields = new HashMap<>();
        for (SasFieldInfo field : SasUtils.getFields("I", new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-180.csv"), null))
            fields.put(field.getNaaccrId(), field.getParentTag());
        Assert.assertTrue(fields.containsKey("primarySite"));
        Assert.assertEquals("Tumor", fields.get("primarySite"));
        Assert.assertFalse(fields.containsKey("nameLast"));

        fields.clear();
        List<File> csvDictionaries = Collections.singletonList(TestingUtils.getDataFile("sas/user-dictionary.csv"));
        for (SasFieldInfo field : SasUtils.getFields("I", new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-180.csv"), csvDictionaries))
            fields.put(field.getNaaccrId(), field.getParentTag());
        Assert.assertTrue(fields.containsKey("myVariable"));
        Assert.assertEquals("Tumor", fields.get("myVariable"));
    }


    @Test
    public void testGetGroupedFields() throws IOException {
        Map<String, List<String>> fields = new HashMap<>();
        for (SasFieldInfo field : SasUtils.getGroupedFields("I", new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/grouped-items/naaccr-xml-grouped-items-180.csv")))
            fields.put(field.getNaaccrId(), field.getContains());
        Assert.assertTrue(fields.containsKey("morphTypebehavIcdO3"));
        Assert.assertEquals(Arrays.asList("histologicTypeIcdO3", "behaviorCodeIcdO3"), fields.get("morphTypebehavIcdO3"));
    }

    @Test
    public void testConvert() throws IOException {
        File xmlFile = TestingUtils.getDataFile("sas/test.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test.csv");
        File xmlCopyFile = new File(TestingUtils.getBuildDirectory(), "test-copy.xml");

        // make sure we start as expected
        assertXmlData(xmlFile, false);

        // translate XML to CSV
        SasXmlToCsv xmlToCsv = createXmlToCsvConverter(xmlFile, csvFile, "180", "I");
        Assert.assertNotNull(xmlToCsv.getXmlPath());
        Assert.assertNotNull(xmlToCsv.getCsvPath());
        xmlToCsv.convert(null, false); // extra row of fields are added for SAS, it messes up this test so it's not added here
        Assert.assertTrue(csvFile.exists());

        // translate CSV to XML, make sure we get back to the same data
        SasCsvToXml csvToXml = createCsvToXmlConverter(csvFile, xmlCopyFile, "180", "I");
        Assert.assertNotNull(csvToXml.getXmlPath());
        Assert.assertNotNull(csvToXml.getCsvPath());
        csvToXml.convert();
        assertXmlData(xmlCopyFile, false);

        // test cleaning up the CSV file
        Assert.assertTrue(csvFile.exists());
        xmlToCsv.cleanup("no");
        Assert.assertTrue(csvFile.exists());
        xmlToCsv.cleanup("yes");
        Assert.assertFalse(csvFile.exists());

        // redo a double-conversion but ignore some fields
        xmlToCsv.convert("patientIdNumber,primarySite", false);
        csvToXml.convert("patientIdNumber,primarySite");
        assertXmlData(xmlCopyFile, true);

        // test grouped items
        xmlToCsv.setIncludeGroupedItems("yes");
        xmlToCsv.convert(null, false);
        Assert.assertTrue(csvFile.exists());
        Assert.assertTrue(FileUtils.readLines(csvFile, StandardCharsets.US_ASCII).get(0).contains("morphTypebehavIcdO3"));

        // another (more complex) file
        xmlFile = TestingUtils.getDataFile("sas/test2.xml");
        csvFile = new File(TestingUtils.getBuildDirectory(), "test2.csv");
        xmlCopyFile = new File(TestingUtils.getBuildDirectory(), "test2-copy.xml");
        createXmlToCsvConverter(xmlFile, csvFile, "180", "A").convert(null, false);
        createCsvToXmlConverter(csvFile, xmlCopyFile, "180", "A").convert(null);
        Patient secondPatient = NaaccrXmlUtils.readXmlFile(xmlCopyFile, null, null, null).getPatients().get(1);
        Assert.assertEquals("00000002", secondPatient.getItemValue("patientIdNumber"));
        Assert.assertEquals(2, secondPatient.getTumors().size());
        Assert.assertEquals("9-9/9-9-16 HOSPITAL, DR DOCTOR: XXX BRAIN (9999 CGY), 99 FX’S, XXXX & 9MV tumor #1", secondPatient.getTumors().get(0).getItemValue("rxTextRadiation"));
        Assert.assertEquals("9-9/9-9-16 HOSPITAL, DR DOCTOR: XXX BRAIN (9999 CGY), 99 FX’S, XXXX & 9MV tumor #2", secondPatient.getTumors().get(1).getItemValue("rxTextRadiation"));
    }

    @Test
    public void testConvertCdata() throws IOException {
        File xmlFile = TestingUtils.getDataFile("sas/test-cdata.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test-cdata.csv");
        File xmlCopyFile = new File(TestingUtils.getBuildDirectory(), "test-cdata-copy.xml");
        createXmlToCsvConverter(xmlFile, csvFile, "180", "I").convert(null, false);
        createCsvToXmlConverter(csvFile, xmlCopyFile, "180", "I").convert();
        Tumor tumor = NaaccrXmlUtils.readXmlFile(xmlCopyFile, null, null, null).getPatients().get(0).getTumors().get(0);
        Assert.assertEquals("C123", tumor.getItemValue("primarySite"));

        xmlFile = TestingUtils.getDataFile("sas/test-cdata2.xml");
        csvFile = new File(TestingUtils.getBuildDirectory(), "test-cdata2.csv");
        xmlCopyFile = new File(TestingUtils.getBuildDirectory(), "test-cdata2-copy.xml");
        createXmlToCsvConverter(xmlFile, csvFile, "180", "A").convert(null, false);
        createCsvToXmlConverter(csvFile, xmlCopyFile, "180", "A").convert();
        tumor = NaaccrXmlUtils.readXmlFile(xmlCopyFile, null, null, null).getPatients().get(0).getTumors().get(0);
        Assert.assertEquals("1/10/2016 DrugB (Part1, Part2, & Part3) @ Facility w/ Dr. Name. [DrugA started in 1/2015 & DrugB regimen planned] 1/15/2017 Drugc @ Facility w/ Dr Name2",
                tumor.getItemValue("rxTextChemo"));
        Assert.assertEquals("1/10/2016 DrugB (Part1, Part2, & Part3) @ Facility w/ Dr. Name. [DrugA started in 1/2015 & DrugB regimen planned] 1/15/2017 Drugc @ Facility w/ Dr Name2",
                tumor.getItemValue("rxTextHormone"));
    }

    @Test
    public void testConvertMultiLine() throws IOException {
        File xmlFile = TestingUtils.getDataFile("sas/test-multi-line.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test-multi-line.csv");
        File xmlCopyFile = new File(TestingUtils.getBuildDirectory(), "test-multi-line-copy.xml");
        createXmlToCsvConverter(xmlFile, csvFile, "180", "A").convert(null, false);
        createCsvToXmlConverter(csvFile, xmlCopyFile, "180", "A").convert();
        Tumor tumor = NaaccrXmlUtils.readXmlFile(xmlCopyFile, null, null, null).getPatients().get(0).getTumors().get(0);
        Assert.assertEquals("Some\ntext", tumor.getItemValue("textRemarks"));

        // same test but this file uses a CDATA section
        xmlFile = TestingUtils.getDataFile("sas/test-multi-line-cdata.xml");
        csvFile = new File(TestingUtils.getBuildDirectory(), "test-multi-line-cdata.csv");
        xmlCopyFile = new File(TestingUtils.getBuildDirectory(), "test-multi-line-cdata-copy.xml");
        createXmlToCsvConverter(xmlFile, csvFile, "180", "A").convert(null, false);
        createCsvToXmlConverter(csvFile, xmlCopyFile, "180", "A").convert();
        tumor = NaaccrXmlUtils.readXmlFile(xmlCopyFile, null, null, null).getPatients().get(0).getTumors().get(0);
        Assert.assertEquals("Some\ntext", tumor.getItemValue("textRemarks"));
    }

    @Test
    public void testConvertEpath() throws IOException {
        List<File> csvDictionaries = Collections.singletonList(TestingUtils.getDataFile("sas/epath/path-text-dictionary.csv"));
        List<NaaccrDictionary> xmlDictionaries = Collections.singletonList(NaaccrXmlDictionaryUtils.readDictionary(TestingUtils.getDataFile("sas/epath/path-text-dictionary.xml")));

        // normal case using the user-defined dictionary
        Tumor tumor = convertForEpathSingleTumor("160", "A", "HL7_AS_NAACCR_XML_V16_ABS.XML", csvDictionaries, xmlDictionaries);
        Assert.assertEquals("20200101", tumor.getItemValue("pathDateSpecCollect1")); // standard tumor field
        Assert.assertEquals("REC-1234", tumor.getItemValue("recordDocumentId")); // non-standard tumor field

        // not providing the dictionaries means the non-standard items will be ignored
        tumor = convertForEpathSingleTumor("160", "A", "HL7_AS_NAACCR_XML_V16_ABS.XML", null, null);
        Assert.assertEquals("20200101", tumor.getItemValue("pathDateSpecCollect1")); // standard tumor field
        Assert.assertNull(tumor.getItemValue("recordDocumentId")); // non-standard tumor field

        // providing a different version shouldn't really matter (in this case)
        tumor = convertForEpathSingleTumor("180", "A", "HL7_AS_NAACCR_XML_V16_ABS.XML", csvDictionaries, xmlDictionaries);
        Assert.assertEquals("20200101", tumor.getItemValue("pathDateSpecCollect1")); // standard tumor field
        Assert.assertEquals("REC-1234", tumor.getItemValue("recordDocumentId")); // non-standard tumor field

        // proving a different record type means some items are going ot be dropped because they apply only to the full abstract
        tumor = convertForEpathSingleTumor("160", "I", "HL7_AS_NAACCR_XML_V16_ABS.XML", csvDictionaries, xmlDictionaries);
        Assert.assertNull(tumor.getItemValue("pathDateSpecCollect1")); // standard tumor field
        Assert.assertEquals("REC-1234", tumor.getItemValue("recordDocumentId")); // non-standard tumor field

        // testing a normal v18 file
        tumor = convertForEpathSingleTumor("180", "A", "HL7_AS_NAACCR_XML_V18_ABS.XML", csvDictionaries, xmlDictionaries);
        Assert.assertEquals("20200101", tumor.getItemValue("pathDateSpecCollect1")); // standard tumor field
        Assert.assertEquals("REC-1234", tumor.getItemValue("recordDocumentId")); // non-standard tumor field

        // testing a different record type
        tumor = convertForEpathSingleTumor("180", "I", "HL7_AS_NAACCR_XML_V18_INC.XML", csvDictionaries, xmlDictionaries);
        Assert.assertNull(tumor.getItemValue("pathDateSpecCollect1")); // standard tumor field
        Assert.assertEquals("REC-1234", tumor.getItemValue("recordDocumentId")); // non-standard tumor field

        // testing the root provided on multi lines
        tumor = convertForEpathSingleTumor("180", "A", "HL7_AS_NAACCR_XML_V18_ABS_MULTI_LINES_ROOT.XML", csvDictionaries, xmlDictionaries);
        Assert.assertEquals("20200101", tumor.getItemValue("pathDateSpecCollect1")); // standard tumor field
        Assert.assertEquals("REC-1234", tumor.getItemValue("recordDocumentId")); // non-standard tumor field

        // testing the data on multiple lines
        tumor = convertForEpathSingleTumor("180", "A", "HL7_AS_NAACCR_XML_V18_ABS_MULTI_LINES_DATA_1.XML", csvDictionaries, xmlDictionaries);
        Assert.assertEquals("Comments on\n multiple lines, which\n is permitted in XML...", tumor.getItemValue("textPathComments"));
        tumor = convertForEpathSingleTumor("180", "A", "HL7_AS_NAACCR_XML_V18_ABS_MULTI_LINES_DATA_2.XML", csvDictionaries, xmlDictionaries);
        Assert.assertEquals("\nComments on multiple lines, which is permitted in XML...\n            ", tumor.getItemValue("textPathComments"));
        tumor = convertForEpathSingleTumor("180", "A", "HL7_AS_NAACCR_XML_V18_ABS_MULTI_LINES_DATA_3.XML", csvDictionaries, xmlDictionaries);
        Assert.assertEquals("\n                Comments on multiple lines, which is permitted in XML...\n            ", tumor.getItemValue("textPathComments"));
        tumor = convertForEpathSingleTumor("180", "A", "HL7_AS_NAACCR_XML_V18_ABS_MULTI_LINES_DATA_4.XML", csvDictionaries, xmlDictionaries);
        Assert.assertEquals("\nComments on multiple lines,\n which is permitted in XML...\n            ", tumor.getItemValue("textPathComments"));

        // test a ZIP file (all internal files should be added to one CSV file)
        NaaccrData data = convertForEpath("160", "A", "HL7_AS_NAACCR_XML_V18_ABS.zip", csvDictionaries, xmlDictionaries);
        Assert.assertEquals(4, data.getPatients().size());
    }

    private Tumor convertForEpathSingleTumor(String naaccrVersion, String recordType, String input, List<File> csvDictionaryFiles, List<NaaccrDictionary> xmlDictionaries) throws IOException {
        return convertForEpath(naaccrVersion, recordType, input, csvDictionaryFiles, xmlDictionaries).getPatients().get(0).getTumors().get(0);
    }

    private NaaccrData convertForEpath(String naaccrVersion, String recordType, String input, List<File> csvDictionaryFiles, List<NaaccrDictionary> xmlDictionaries) throws IOException {
        File xmlFile = TestingUtils.copyFile(TestingUtils.getDataFile("sas/epath/" + input), TestingUtils.getBuildDirectory());
        File csvFile = new File(TestingUtils.getBuildDirectory(), xmlFile.getName().replace(".XML", ".csv").replace(".zip", ".csv"));
        File xmlCopyFile = new File(TestingUtils.getBuildDirectory(), xmlFile.getName().replace(".XML", "-COPY.XML").replace(".zip", "-COPY.XML"));
        createXmlToCsvConverter(xmlFile, csvFile, naaccrVersion, recordType, csvDictionaryFiles).convert(null, false);
        createCsvToXmlConverter(csvFile, xmlCopyFile, naaccrVersion, recordType, csvDictionaryFiles, xmlDictionaries).convert(null);
        return NaaccrXmlUtils.readXmlFile(xmlCopyFile, null, xmlDictionaries, null);
    }

    @SuppressWarnings("SameParameterValue")
    private SasXmlToCsv createXmlToCsvConverter(File xmlFile, File csvFile, String naaccrVersion, String recordType) {
        return createXmlToCsvConverter(xmlFile, csvFile, naaccrVersion, recordType, null);
    }

    private SasXmlToCsv createXmlToCsvConverter(File xmlFile, File csvFile, String naaccrVersion, String recordType, List<File> csvDictionaryFiles) {
        return new SasXmlToCsv(xmlFile.getPath(), csvFile.getPath(), naaccrVersion, recordType) {
            @Override
            public List<SasFieldInfo> getFields() {
                try {
                    return SasUtils.getFields(recordType, new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-" + naaccrVersion + ".csv"), csvDictionaryFiles);
                }
                catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public List<SasFieldInfo> getGroupedFields() {
                try {
                    return SasUtils.getGroupedFields(recordType, new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/grouped-items/naaccr-xml-grouped-items-" + naaccrVersion + ".csv"));
                }
                catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @SuppressWarnings("SameParameterValue")
    private SasCsvToXml createCsvToXmlConverter(File csvFile, File xmlFile, String naaccrVersion, String recordType) {
        return createCsvToXmlConverter(csvFile, xmlFile, naaccrVersion, recordType, null, null);
    }

    private SasCsvToXml createCsvToXmlConverter(File csvFile, File xmlFile, String naaccrVersion, String recordType, List<File> csvDictionaryFiles, List<NaaccrDictionary> xmlDictionaries) {
        SasCsvToXml converter = new SasCsvToXml(csvFile.getPath(), xmlFile.getPath(), naaccrVersion, recordType) {
            @Override
            public List<SasFieldInfo> getFields() {
                try {
                    return SasUtils.getFields(recordType, new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-" + naaccrVersion + ".csv"), csvDictionaryFiles);
                }
                catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

        };

        if (csvDictionaryFiles != null && xmlDictionaries != null) {
            converter.setDictionary(
                    csvDictionaryFiles.stream().map(File::getPath).collect(Collectors.joining(" ")),
                    xmlDictionaries.stream().map(NaaccrDictionary::getDictionaryUri).collect(Collectors.joining(" ")));
        }

        converter.setWriteNumbers("true");

        return converter;
    }

    private void assertXmlData(File xmlFile, boolean ignoreFields) throws IOException {
        NaaccrData data = NaaccrXmlUtils.readXmlFile(xmlFile, null, null, null);
        if (!ignoreFields)
            Assert.assertEquals("0000000001", data.getItemValue("registryId"));
        else
            Assert.assertNull(data.getItemValue("registryId"));
        Assert.assertEquals(2, data.getPatients().size());
        Assert.assertEquals("00000001", data.getPatients().get(0).getItemValue("patientIdNumber"));
        if (!ignoreFields)
            Assert.assertEquals("1", data.getPatients().get(0).getItemValue("sex"));
        else
            Assert.assertNull(data.getPatients().get(0).getItemValue("sex"));
        Assert.assertEquals(1, data.getPatients().get(0).getTumors().size());
        Assert.assertEquals("C123", data.getPatients().get(0).getTumors().get(0).getItemValue("primarySite"));
        Assert.assertEquals("00000002", data.getPatients().get(1).getItemValue("patientIdNumber"));
        Assert.assertNull(data.getPatients().get(1).getItemValue("sex"));
        Assert.assertEquals(2, data.getPatients().get(1).getTumors().size());
        Assert.assertEquals("C456", data.getPatients().get(1).getTumors().get(0).getItemValue("primarySite"));
        Assert.assertEquals("C789", data.getPatients().get(1).getTumors().get(1).getItemValue("primarySite"));
        if (!ignoreFields)
            Assert.assertEquals("  2.0", data.getPatients().get(1).getTumors().get(1).getItemValue("ki67"));
    }

    @Test
    public void testSetDictionary() {
        File xmlFile = TestingUtils.getDataFile("sas/test.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test.csv");

        List<File> csvDictionaryFiles = new ArrayList<>();
        csvDictionaryFiles.add(TestingUtils.getDataFile("sas/user-dictionary-1.csv"));
        csvDictionaryFiles.add(TestingUtils.getDataFile("sas/user-dictionary-2.csv"));

        SasCsvToXml csvConverter = new SasCsvToXml(csvFile.getPath(), xmlFile.getPath(), "210", "I");
        csvConverter.setDictionary(csvDictionaryFiles.stream().map(File::getPath).collect(Collectors.joining(" ")), "test1 test2");
        Assert.assertEquals(2, csvConverter.getUserDictionaryFiles().size());

        SasXmlToCsv xmlConverter = new SasXmlToCsv(csvFile.getPath(), xmlFile.getPath(), "210", "I");
        xmlConverter.setDictionary(csvDictionaryFiles.stream().map(File::getPath).collect(Collectors.joining(" ")));
        Assert.assertEquals(2, xmlConverter.getUserDictionaryFiles().size());
    }

    @Test
    public void testSetWriteNumbers() {
        File xmlFile = TestingUtils.getDataFile("sas/test.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test.csv");

        SasCsvToXml csvConverter = new SasCsvToXml(csvFile.getPath(), xmlFile.getPath(), "210", "I");
        Assert.assertEquals("No", csvConverter.getWriteNumbers());

        csvConverter.setWriteNumbers("yes");
        Assert.assertEquals("Yes", csvConverter.getWriteNumbers());

        csvConverter.setWriteNumbers("True");
        Assert.assertEquals("Yes", csvConverter.getWriteNumbers());

        csvConverter.setWriteNumbers("no");
        Assert.assertEquals("No", csvConverter.getWriteNumbers());

        csvConverter.setWriteNumbers("False");
        Assert.assertEquals("No", csvConverter.getWriteNumbers());

        csvConverter.setWriteNumbers("XXX");
        Assert.assertEquals("No", csvConverter.getWriteNumbers());
    }

    @Test
    public void testSetGroupTumors() {
        File xmlFile = TestingUtils.getDataFile("sas/test.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test.csv");

        SasCsvToXml csvConverter = new SasCsvToXml(csvFile.getPath(), xmlFile.getPath(), "210", "I");
        Assert.assertEquals("Yes", csvConverter.getGroupTumors());

        csvConverter.setGroupTumors("yes");
        Assert.assertEquals("Yes", csvConverter.getGroupTumors());

        csvConverter.setGroupTumors("True");
        Assert.assertEquals("Yes", csvConverter.getGroupTumors());

        csvConverter.setGroupTumors("no");
        Assert.assertEquals("No", csvConverter.getGroupTumors());

        csvConverter.setGroupTumors("False");
        Assert.assertEquals("No", csvConverter.getGroupTumors());

        csvConverter.setGroupTumors("XXX");
        Assert.assertEquals("Yes", csvConverter.getGroupTumors());
    }
}
