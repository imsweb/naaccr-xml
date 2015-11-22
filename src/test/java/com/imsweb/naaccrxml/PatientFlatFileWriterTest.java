/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;

public class PatientFlatFileWriterTest {

    @Test
    public void testValuesWithNewLines() throws IOException {

        File targetFile = TestingUtils.createFile("values-with-new-lines.txt");
        Writer writer = NaaccrXmlUtils.createWriter(targetFile);

        NaaccrData data = new NaaccrData();
        data.setBaseDictionaryUri(NaaccrXmlDictionaryUtils.createUriFromVersion("150", true));
        data.setRecordType("I");
        PatientFlatWriter pfw = new PatientFlatWriter(writer, data);

        Patient patient = new Patient();
        patient.getItems().add(createItem("patientIdNumber", "000000\r\n1"));
        pfw.writePatient(patient);
        pfw.close();

        Reader reader = NaaccrXmlUtils.createReader(targetFile);
        PatientFlatReader pfr = new PatientFlatReader(reader, null, null);
        patient = pfr.readPatient();
        pfr.close();
        Assert.assertEquals("000000 1", patient.getItemValue("patientIdNumber")); // new line should have been replaced by a space...
    }

    private Item createItem(String id, String value) {
        Item item = new Item();
        item.setNaaccrId(id);
        item.setValue(value);
        return item;
    }
}
