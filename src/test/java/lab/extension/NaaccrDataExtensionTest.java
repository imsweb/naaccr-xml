/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab.extension;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.PatientXmlReader;
import com.imsweb.naaccrxml.PatientXmlWriter;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;

public class NaaccrDataExtensionTest {

    public static void main(String[] args) throws IOException {

        // setup configuration
        NaaccrStreamConfiguration configuration = new NaaccrStreamConfiguration();
        configuration.registerNamespace("ext", "http://demo.org");
        configuration.registerTag("ext", "OverallSummary", OverallSummary.class);
        configuration.registerTag("ext", "Patient", OverallSummary.class, "_patientSummary");
        configuration.registerTag("ext", "Patient", Summary.class);
        configuration.registerTag("ext", "Tumor", OverallSummary.class, "_tumorSummary");
        configuration.registerTag("ext", "Tumor", Summary.class);
        configuration.registerTag("ext", "NumberRejected", Summary.class, "_numberRejected");
        configuration.registerTag("ext", "NumberProcessed", Summary.class, "_numberProcessed");
        configuration.registerTag("ext", "NumberMatched", Summary.class, "_numberMatched");

        // setup extension data
        OverallSummary summary = new OverallSummary();
        Summary patientSummary = new Summary();
        patientSummary.setNumberRejected(1);
        patientSummary.setNumberProcessed(256);
        patientSummary.setNumberMatched(250);
        summary.setPatientSummary(patientSummary);
        Summary tumorSummary = new Summary();
        tumorSummary.setNumberRejected(3);
        tumorSummary.setNumberProcessed(348);
        tumorSummary.setNumberMatched(338);
        summary.setTumorSummary(tumorSummary);

        // setup regular data
        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
        data.setExtension(summary);

        // write the file
        File file = TestingUtils.createFile("test-root-extension.xml", false);
        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, null, configuration)) {
            Patient patient = new Patient();
            patient.addItem(new Item("patientIdNumber", "00000001"));
            writer.writePatient(patient);
        }

        // read back the file
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(file), null, null, configuration)) {
            OverallSummary os = (OverallSummary)reader.getRootData().getExtension();
            System.out.println(os.getPatientSummary().getNumberProcessed() + " processed patients");
            System.out.println(os.getTumorSummary().getNumberProcessed() + " processed tumors");
            System.out.println(reader.readPatient().getItemValue("patientIdNumber"));
        }
    }
}
