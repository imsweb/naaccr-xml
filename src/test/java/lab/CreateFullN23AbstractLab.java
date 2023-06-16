/*
 * Copyright (C) 2023 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.stream.Collectors;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class CreateFullN23AbstractLab {

    public static void main(String[] args) throws IOException {
        NaaccrData data = new NaaccrData();
        data.setBaseDictionaryUri(NaaccrXmlDictionaryUtils.createUriFromVersion("230", true));
        data.setRecordType("A");
        data.setTimeGenerated(new Date());
        Patient patient = new Patient();
        for (NaaccrDictionaryItem item : NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("230").getItems().stream().filter(i -> i.getParentXmlElement().equals("Patient")).collect(Collectors.toList()))
            patient.addItem(new Item(item.getNaaccrId(), getTestString(item.getLength())));
        Tumor tumor = new Tumor();
        for (NaaccrDictionaryItem item : NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("230").getItems().stream().filter(i -> i.getParentXmlElement().equals("Tumor")).collect(Collectors.toList()))
            tumor.addItem(new Item(item.getNaaccrId(), getTestString(item.getLength())));
        patient.addTumor(tumor);
        data.addPatient(patient);

        // write the entire file at once
        File file = new File(TestingUtils.getWorkingDirectory() + "/build/n23-abstract-full-text.xml");
        NaaccrXmlUtils.writeXmlFile(data, file, null, null, null);
    }

    private static String getTestString(int length) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < length; i++)
            buf.append("X");
        return buf.toString();
    }
}
