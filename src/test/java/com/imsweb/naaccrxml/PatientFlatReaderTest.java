/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Patient;

public class PatientFlatReaderTest {

    @Test
    public void testPatientLevelMismatch() throws IOException {

        // create a testing file with two records having a different value for a root-level item
        StringBuilder rec1 = createEmptyRecord();
        rec1.replace(41, 49, "00000001"); // patient ID number
        rec1.replace(189, 190, "1"); // computed ethnicity
        StringBuilder rec2 = createEmptyRecord();
        rec2.replace(41, 49, "00000001"); // patient ID number
        rec2.replace(189, 190, "2"); // computed ethnicity
        File file = createFile("test-pat-level.txt", rec1.toString(), rec2.toString());

        // create the option object we are going to use
        NaaccrOptions options = new NaaccrOptions();
        options.setReportLevelMismatch(true);
        options.setValidateReadValues(false);

        // lets read the patients; first one should be used to populate the root data; second one should report a failure for the mismatch
        PatientFlatReader reader = new PatientFlatReader(new FileReader(file), options, null);

        Patient pat1 = reader.readPatient();
        Assert.assertNotNull(pat1);
        Assert.assertEquals(2, pat1.getTumors().size()); // tumors are both for the same patient ID number, should be one patient with two tumors...
        Assert.assertEquals(1, pat1.getAllValidationErrors().size());
        Assert.assertTrue(pat1.getAllValidationErrors().get(0).getMessage().contains("patient-level"));

        Assert.assertNull(reader.readPatient());
        reader.close();
    }

    @Test
    public void testRootLevelMismatch() throws IOException {

        // create a testing file with two records having a different value for a root-level item
        StringBuilder rec1 = createEmptyRecord();
        rec1.replace(19, 29, "0000000001"); // registry ID
        StringBuilder rec2 = createEmptyRecord();
        rec2.replace(19, 29, "0000000002"); // registry ID
        File file = createFile("test-root-level.txt", rec1.toString(), rec2.toString());

        // create the option object we are going to use
        NaaccrOptions options = new NaaccrOptions();
        options.setReportLevelMismatch(true);
        options.setValidateReadValues(false);

        // lets read the patients; first one should be used to populate the root data; second one should report a failure for the mismatch
        PatientFlatReader reader = new PatientFlatReader(new FileReader(file), options, null);

        Patient pat1 = reader.readPatient();
        Assert.assertNotNull(pat1);
        Assert.assertEquals(1, pat1.getTumors().size()); // no patient ID number provided, should be two patients with one tumor each
        Assert.assertEquals(0, pat1.getAllValidationErrors().size());

        Patient pat2 = reader.readPatient();
        Assert.assertNotNull(pat2);
        Assert.assertEquals(1, pat2.getTumors().size());
        Assert.assertEquals(1, pat2.getAllValidationErrors().size());
        Assert.assertTrue(pat2.getAllValidationErrors().get(0).getMessage().contains("root-level"));

        Assert.assertNull(reader.readPatient());
        reader.close();
    }

    private StringBuilder createEmptyRecord() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 3339; i++)
            buf.append(" ");
        buf.replace(0, 1, "I");
        buf.replace(16, 19, "150");
        return buf;
    }

    private File createFile(String filename, String... records) throws IOException {
        File tmpDir = new File(System.getProperty("user.dir") + "/build/test-tmp");
        if (!tmpDir.exists() && !tmpDir.mkdirs())
            throw new IOException("Unable to create tmp dir...");

        File file = new File(tmpDir, filename);
        FileWriter writer = new FileWriter(file);
        for (String rec : records) {
            writer.write(rec);
            writer.write("\n");
        }
        writer.close();

        return file;
    }
}
