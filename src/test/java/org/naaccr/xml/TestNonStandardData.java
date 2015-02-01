/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;

public class TestNonStandardData {

    public static void main(String[] args) throws IOException {

        /**
        XStream xstream = NaaccrXmlUtils.getStandardXStream();
        // tell XStream how to read our customized data structure
        xstream.alias("EditsReport", EditsReport.class);
        xstream.alias("EditFailure", EditFailure.class);
        xstream.addImplicitCollection(EditsReport.class, "failures", EditFailure.class);
        // replace the standard Patient by our customized one (which supports the edits report)
        xstream.alias("Patient", ExtendedPatient.class);
        xstream.aliasField("EditsReport", ExtendedPatient.class, "editsReport");
        // replace the standard Tumor by our customized one (which supports the edits reports)
        xstream.alias("Tumor", ExtendedTumor.class);
        xstream.aliasField("EditsReport", ExtendedTumor.class, "editsReport");
        
        // read our testing file and print the failures for the patient and the first tumor
        File inputFile = new File(System.getProperty("user.dir") + "/build/another-xml-test2.xml");
        try (PatientXmlReader reader = new PatientXmlReader(xstream, new FileReader(inputFile), NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, null)) {
            ExtendedPatient patient = (ExtendedPatient)reader.readPatient();
            System.out.println("Patient failures: " + patient.getEditsReport().getFailures().size());
            ExtendedTumor tumor = (ExtendedTumor)patient.getTumors().get(0);
            System.out.println("Tumor #0 failures: " + tumor.getEditsReport().getFailures().size());
        }
         */
    }
    
}
