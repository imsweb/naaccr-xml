/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab.extension;

import java.io.File;
import java.io.IOException;

import com.imsweb.naaccrxml.TestingUtils;

public class NaaccrDataExtensionTest {

    public static void main(String[] args) throws IOException {

        File file = TestingUtils.createFile("test-root-extension.xml", false);

//        NaaccrStreamConfiguration configuration = new NaaccrStreamConfiguration() {
//            @Override
//            protected XStream createXStream(HierarchicalStreamDriver driver, NaaccrPatientConverter patientConverter) {
//                XStream xstream = super.createXStream(driver, patientConverter);
//
//                xstream.autodetectAnnotations(true);
//                xstream.alias("FileContentSummary", FileContentSummary.class);
//
//                return xstream;
//            }
//        };

//        // TODO - call a "registerNamespace" for "ext" and the tags/attributes, then maybe still pass the prefix to the three methods?
//        NaaccrStreamConfiguration configuration = new NaaccrStreamConfiguration();
//        configuration.getXstream().alias("ext:FileContentSummary", FileContentSummary.class);
//        configuration.getXstream().aliasField("ext:NumberOfPatients", FileContentSummary.class, "numPatients");
//        configuration.getXstream().aliasAttribute(FileContentSummary.class, "summarySpecificationVersion", "ext:specificationVersion");
//
//
//
//        NaaccrData data = new NaaccrData(NaaccrFormat.NAACCR_FORMAT_16_ABSTRACT);
//        data.addExtraRootParameters("xmlns:ext", "http://test.org");
//        data.addItem(new Item("registryId", "0000000001"));
//
//        FileContentSummary summary = new FileContentSummary();
//        summary.setSummarySpecificationVersion("1.0");
//        summary.setNumPatients(1);
//
//        try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), data, null, null, configuration) {
//            @Override
//            protected void writeExtension(XStream xstream, HierarchicalStreamWriter writer) {
//                xstream.marshal(summary, writer);
//            }
//        }) {
//            Patient patient = new Patient();
//            patient.addItem(new Item("patientIdNumber", "00000001"));
//            writer.writePatient(patient);
//        }
    }
}
