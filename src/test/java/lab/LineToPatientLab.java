/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.stream.IntStream;

import com.imsweb.naaccrxml.NaaccrOptions;
import com.imsweb.naaccrxml.NaaccrValidationError;
import com.imsweb.naaccrxml.PatientFlatReader;
import com.imsweb.naaccrxml.entity.Patient;

public class LineToPatientLab {

    public static void main(String[] args) throws IOException {

        // simulate a line of 22824 characters (full NAACCR Abstract line); type and version are require and should have a legit value...
        StringBuilder buf = new StringBuilder();
        IntStream.range(0, 22824).forEach(i -> buf.append('1'));
        buf.replace(0, 1, "A");
        buf.replace(16, 19, "160");
        String line = buf.toString();

        // let's read the line as a patient and display some information (we have many errors because we are using a line of dummy data...
        try (PatientFlatReader reader = new PatientFlatReader(new StringReader(line), NaaccrOptions.getDefault(), Collections.emptyList())) {
            Patient patient = reader.readPatient();
            System.out.println("built " + patient.getItems().size() + " patient items");
            System.out.println("built " + patient.getTumors().size() + " tumor");
            System.out.println("built " + patient.getTumors().get(0).getItems().size() + " tumor items");
            System.out.println("found " + patient.getAllValidationErrors().size() + " errors:");
            for (NaaccrValidationError error : patient.getAllValidationErrors())
                System.out.println("   > " + error.getNaaccrId() + ": " + error.getMessage());
        }
    }
}
