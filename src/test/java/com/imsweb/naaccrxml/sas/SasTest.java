/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.sas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Tumor;

public class SasTest {

    @Test
    public void testGetFields() throws IOException {
        Map<String, String> fields = new HashMap<>();
        for (SasFieldInfo field : SasUtils.getFields("I", new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-180.csv")))
            fields.put(field.getNaaccrId(), field.getParentTag());

        Assert.assertTrue(fields.containsKey("primarySite"));
        Assert.assertEquals("Tumor", fields.get("primarySite"));
        Assert.assertFalse(fields.containsKey("nameLast"));
    }

    @Test
    public void testConvert() throws IOException {
        File xmlFile = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/sas/test.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test.csv");
        File xmlCopyFile = new File(TestingUtils.getBuildDirectory(), "test-copy.xml");

        // make sure we start as expected
        assertXmlData(xmlFile, false);

        // translate XML to CSV
        SasXmlToCsv xmlToCsv = createXmlToCsvConverter(xmlFile, csvFile);
        Assert.assertNotNull(xmlToCsv.getXmlPath());
        Assert.assertNotNull(xmlToCsv.getCsvPath());
        xmlToCsv.convert(null, false); // extra row of fields are added for SAS, it messes up this test so it's not added here
        Assert.assertTrue(csvFile.exists());

        // translate CSV to XML, make sure we get back to the same data
        SasCsvToXml csvToXml = createCsvToXmlConverter(csvFile, xmlCopyFile);
        Assert.assertNotNull(csvToXml.getXmlPath());
        Assert.assertNotNull(csvToXml.getCsvPath());
        csvToXml.convert();
        assertXmlData(xmlCopyFile, false);

        // test cleaning up the CSV file
        Assert.assertTrue(csvFile.exists());
        xmlToCsv.cleanup();
        Assert.assertFalse(csvFile.exists());

        // redo a double-conversion but ignore some fields
        xmlToCsv.convert("patientIdNumber,primarySite", false);
        csvToXml.convert("patientIdNumber,primarySite");
        assertXmlData(xmlCopyFile, true);
    }

    @Test
    public void testConvertCdata() throws IOException {
        File xmlFile = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/sas/test-cdata.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test-cdata.csv");
        File xmlCopyFile = new File(TestingUtils.getBuildDirectory(), "test-cdata-copy.xml");
        createXmlToCsvConverter(xmlFile, csvFile).convert(null, false);
        createCsvToXmlConverter(csvFile, xmlCopyFile).convert();
        Tumor tumor = NaaccrXmlUtils.readXmlFile(xmlCopyFile, null, null, null).getPatients().get(0).getTumors().get(0);
        Assert.assertEquals("C123", tumor.getItemValue("primarySite"));
    }

    @Test
    public void testConvertMultiLine() throws IOException {
        File xmlFile = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/sas/test-multi-line.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test-multi-line.csv");
        File xmlCopyFile = new File(TestingUtils.getBuildDirectory(), "test-multi-line-copy.xml");
        createXmlToCsvConverter(xmlFile, csvFile).convert(null, false);
        createCsvToXmlConverter(csvFile, xmlCopyFile).convert();
        Tumor tumor = NaaccrXmlUtils.readXmlFile(xmlCopyFile, null, null, null).getPatients().get(0).getTumors().get(0);
        Assert.assertEquals("Some\ntext", tumor.getItemValue("textRemarks").replace("\r\n", "\n"));
    }

    private SasXmlToCsv createXmlToCsvConverter(File xmlFile, File csvFile) {
        return new SasXmlToCsv(xmlFile.getPath(), csvFile.getPath(), "180", "I") {
            @Override
            public List<SasFieldInfo> getFields() {
                try {
                    return SasUtils.getFields("I", new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-180.csv"));
                }
                catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private SasCsvToXml createCsvToXmlConverter(File csvFile, File xmlFile) {
        return new SasCsvToXml(csvFile.getPath(), xmlFile.getPath(), "180", "I") {
            @Override
            public List<SasFieldInfo> getFields() {
                try {
                    return SasUtils.getFields("I", new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-180.csv"));
                }
                catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
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
    }
}
