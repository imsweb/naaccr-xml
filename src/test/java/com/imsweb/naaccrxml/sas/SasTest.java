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

public class SasTest {

    @Test
    public void testGetFields() throws IOException {
        Map<String, String> fields = new HashMap<>();
        for (SasFieldInfo field : SasUtils.getFields("I", new FileInputStream("docs/naaccr-xml-items-180.csv")))
            fields.put(field.getNaaccrId(), field.getParentTag());

        Assert.assertTrue(fields.containsKey("primarySite"));
        Assert.assertEquals("Tumor", fields.get("primarySite"));
        Assert.assertFalse(fields.containsKey("nameLast"));
    }

    @Test
    public void testConvert() throws IOException {
        File xmlFile = new File(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/sas/test.xml");
        File csvFile = new File(TestingUtils.getBuildDirectory(), "test.csv");
        File xmlFile2 = new File(TestingUtils.getBuildDirectory(), "test2.xml");

        assertXmlData(xmlFile, true);

        SasXmlToCsv xmlToCsv = new SasXmlToCsv(xmlFile.getPath(), csvFile.getPath(), "180", "I") {
            @Override
            public List<SasFieldInfo> getFields() {
                return SasTest.this.getFields();
            }
        };
        Assert.assertNotNull(xmlToCsv.getXmlPath());
        Assert.assertNotNull(xmlToCsv.getCsvPath());
        xmlToCsv.convert(null, false);

        SasCsvToXml csvToXml = new SasCsvToXml(csvFile.getPath(), xmlFile2.getPath(), "180", "I") {
            @Override
            public List<SasFieldInfo> getFields() {
                return SasTest.this.getFields();
            }
        };
        Assert.assertNotNull(csvToXml.getXmlPath());
        Assert.assertNotNull(csvToXml.getCsvPath());
        csvToXml.convert();

        assertXmlData(xmlFile2, true);

        Assert.assertTrue(csvFile.exists());
        xmlToCsv.cleanup();
        Assert.assertFalse(csvFile.exists());

        // redo the full conversion but ignore some fields
        xmlToCsv.convert("patientIdNumber,primarySite", false);
        csvToXml.convert("patientIdNumber,primarySite");
        assertXmlData(xmlFile2, false);
    }

    private List<SasFieldInfo> getFields() {
        try {
            return SasUtils.getFields("I", new FileInputStream(TestingUtils.getWorkingDirectory() + "/docs/naaccr-xml-items-180.csv"));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertXmlData(File xmlFile, boolean expectRegistryId) throws IOException {
        NaaccrData data = NaaccrXmlUtils.readXmlFile(xmlFile, null, null, null);
        if (expectRegistryId)
            Assert.assertEquals("0000000001", data.getItemValue("registryId"));
        else
            Assert.assertNull(data.getItemValue("registryId"));
        Assert.assertEquals(2, data.getPatients().size());
        Assert.assertEquals("00000001", data.getPatients().get(0).getItemValue("patientIdNumber"));
        Assert.assertEquals(1, data.getPatients().get(0).getTumors().size());
        Assert.assertEquals("C123", data.getPatients().get(0).getTumors().get(0).getItemValue("primarySite"));
        Assert.assertEquals("00000002", data.getPatients().get(1).getItemValue("patientIdNumber"));
        Assert.assertEquals(2, data.getPatients().get(1).getTumors().size());
        Assert.assertEquals("C456", data.getPatients().get(1).getTumors().get(0).getItemValue("primarySite"));
        Assert.assertEquals("C789", data.getPatients().get(1).getTumors().get(1).getItemValue("primarySite"));
    }
}
