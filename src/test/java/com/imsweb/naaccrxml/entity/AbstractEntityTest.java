/*
 * Copyright (C) 2019 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.NaaccrFormat;

public class AbstractEntityTest {

    @Test
    public void testAddRemove() {
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_18_ABSTRACT);
        Patient patient1 = new Patient();
        patient1.addItem(new Item("patientIdNumber", "00000001"));
        patient1.addItem(new Item("nameLast", "TEST"));
        Tumor tumor1 = new Tumor();
        tumor1.addItem(new Item("primarySite", "C123"));
        Tumor tumor2 = new Tumor();
        tumor2.addItem(new Item("primarySite", "C456"));
        tumor2.addItem(new Item("laterality", "1"));
        patient1.setTumors(Arrays.asList(tumor1, tumor2));
        Patient patient2 = new Patient();
        patient2.addItem(new Item("patientIdNumber", "00000002"));
        data.setPatients(Arrays.asList(patient1, patient2));

        Assert.assertEquals(2, data.getPatients().size());
        data.removePatient(1);
        Assert.assertEquals(1, data.getPatients().size());
        Assert.assertEquals(2, patient1.getTumors().size());
        patient1.removeTumor(0);
        Assert.assertEquals(1, patient1.getTumors().size());

        Assert.assertEquals(2, patient1.getItems().size());
        Assert.assertTrue(patient1.removeItem("nameLast"));
        Assert.assertEquals(1, patient1.getItems().size());
        Assert.assertEquals("00000001", patient1.getItemValue("patientIdNumber"));

        Assert.assertEquals(2, tumor2.getItems().size());
        Assert.assertTrue(tumor2.removeItem("primarySite"));
        Assert.assertEquals(1, tumor2.getItems().size());
        Assert.assertEquals("1", tumor2.getItemValue("laterality"));
    }

}
