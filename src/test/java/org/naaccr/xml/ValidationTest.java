/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;

// TODO FPD beef up these tests; add cases for options, user dictionary and observer...
// TODO FPD add tests for line number on main entities...
public class ValidationTest {

    @Test
    public void testReadingXml() throws IOException {
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/validation-test-1.xml");

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
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(file))) {
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
    public void testWritingXml() throws IOException {
        NaaccrData data = new NaaccrData();
        data.setBaseDictionaryUri(NaaccrDictionaryUtils.createUriFromVersion("140", true));
        data.setRecordType("I");
        data.setTimeGenerated(new Date());
        data.getItems().add(createItem("vendorName", "VENDOR"));
        Patient patient1 = new Patient();
        patient1.getItems().add(createItem("patientIdNumber", "00000001"));
        Tumor tumor1 = new Tumor();
        tumor1.getItems().add(createItem("primarySite", "C123"));
        patient1.getTumors().add(tumor1);
        data.getPatients().add(patient1);
        Patient patient2 = new Patient();
        patient2.getItems().add(createItem("patientIdNumber", "00000002"));
        data.getPatients().add(patient2);

        // write the entire file at once
        File file = new File(System.getProperty("user.dir") + "/build/test-writing-1.xml");
        NaaccrXmlUtils.writeXmlFile(data, file, null, null, null);

        // write the file using a steam
        file = new File(System.getProperty("user.dir") + "/build/test-writing-2.xml");
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data)) {
            for (Patient patient : data.getPatients())
                writer.writePatient(patient);
        }
    }

    @Test
    public void testFlatToXmlAndXmlToFlat() throws IOException {
        File xmlFile1 = new File(System.getProperty("user.dir") + "/src/test/resources/validation-test-1.xml");
        NaaccrData data1 = NaaccrXmlUtils.readXmlFile(xmlFile1, null, null, null);

        File flatFile1 = new File(System.getProperty("user.dir") + "/build/test.txt");
        NaaccrXmlUtils.writeFlatFile(data1, flatFile1, null, null, null);
        data1 = NaaccrXmlUtils.readFlatFile(flatFile1, null, null, null);
        Assert.assertEquals("VENDOR", data1.getItemValue("vendorName"));

        File xmlFile2 = new File(System.getProperty("user.dir") + "/build/test.xml");
        NaaccrXmlUtils.flatToXml(flatFile1, xmlFile2, null, null, null);

        File flatFile2 = new File(System.getProperty("user.dir") + "/build/test2.txt");
        NaaccrXmlUtils.xmlToFlat(xmlFile2, flatFile2, null, null, null);
    }

    private Item createItem(String id, String value) {
        Item item = new Item();
        item.setNaaccrId(id);
        item.setValue(value);
        return item;
    }
}
