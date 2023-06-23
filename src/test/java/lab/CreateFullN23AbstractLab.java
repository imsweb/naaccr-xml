/*
 * Copyright (C) 2023 Information Management Services, Inc.
 */
package lab;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class CreateFullN23AbstractLab {

    public static void main2(String[] args) throws IOException {
        File file = new File(TestingUtils.getWorkingDirectory() + "/build/test-formats.txt");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.US_ASCII))) {
            writer.write("input\r\n");
            int counter = 1;
            for (NaaccrDictionaryItem item : NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("230").getItems()) {
                writer.write("@" + counter + " " + item.getNaaccrId() + " $" + item.getLength() + ".\r\n");
                counter += item.getLength();
            }
            writer.write(";");
        }
    }

    public static void main(String[] args) throws IOException {
        NaaccrData data = new NaaccrData();
        data.setBaseDictionaryUri(NaaccrXmlDictionaryUtils.createUriFromVersion("230", true));
        data.setRecordType("A");
        data.setTimeGenerated(new Date());
        for (NaaccrDictionaryItem item : NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("230").getItems().stream().filter(i -> i.getParentXmlElement().equals("NaaccrData")).collect(
                Collectors.toList())) {
            data.addItem(new Item(item.getNaaccrId(), getTestString(item.getLength())));
        }
        Patient patient = new Patient();
        for (NaaccrDictionaryItem item : NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("230").getItems().stream().filter(i -> i.getParentXmlElement().equals("Patient")).collect(
                Collectors.toList())) {
            patient.addItem(new Item(item.getNaaccrId(), getTestString(item.getLength())));
        }
        Tumor tumor = new Tumor();
        for (NaaccrDictionaryItem item : NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("230").getItems().stream().filter(i -> i.getParentXmlElement().equals("Tumor")).collect(
                Collectors.toList())) {
            tumor.addItem(new Item(item.getNaaccrId(), getTestString(item.getLength())));
        }
        patient.addTumor(tumor);
        data.addPatient(patient);

        // write XML file
        File file = new File(TestingUtils.getWorkingDirectory() + "/build/n23-abstract-full-text.xml");
        NaaccrXmlUtils.writeXmlFile(data, file, null, null, null);

        // write fixed-column flat file
        file = new File(TestingUtils.getWorkingDirectory() + "/build/n23-abstract-full-text.txt");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.US_ASCII))) {
            for (NaaccrDictionaryItem item : NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("230").getItems())
                writer.write(String.join("", getTestString(item.getLength())));
        }
        file = new File(TestingUtils.getWorkingDirectory() + "/build/n23-abstract-full-text-single-values.txt");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.US_ASCII))) {
            for (NaaccrDictionaryItem item : NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("230").getItems())
                writer.write(String.join("", StringUtils.rightPad("X", item.getLength(), " ")));
        }

        //        // write character-delimited flat file
        //        file = new File(TestingUtils.getWorkingDirectory() + "/build/n23-abstract-full-text.csv");
        //        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.US_ASCII))) {
        //            writer.write(String.join("\t", fullValues));
        //        }
        //        file = new File(TestingUtils.getWorkingDirectory() + "/build/n23-abstract-full-text-single-values.csv");
        //        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.US_ASCII))) {
        //            writer.write(String.join("\t", singleValues));
        //        }
    }

    private static String getTestString(int length) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < length; i++)
            buf.append("X");
        return buf.toString();
    }
}
