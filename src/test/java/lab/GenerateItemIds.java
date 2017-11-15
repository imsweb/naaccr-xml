/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;

public class GenerateItemIds {

    public static void main(String[] args) throws IOException {
        // use this to create the IDs from names, input file must have the names, one per line (and nothing else)
        Files.lines(Paths.get("C:\\dev\\data_items.txt")).forEach(line -> System.out.println(NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(line)));

        // use this to create the parent XML elements from the section, input file must have the sections, one per line (and nothing else)
        //Files.lines(Paths.get("C:\\dev\\data_items.txt")).forEach(line -> System.out.println(getParentTagFromSection(line)));
    }

    private static String getParentTagFromSection(String section) {

        // exception (have to manually fix it in the file): Over-ride Name/Sex should be "Patient" and not "Tumor" (I think)
        if ("Demographic".equals(section) || "Follow-up/Recurrence/Death".equals(section) || "Patient-Confidential".equals(section))
            return NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT;
        return NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR;
    }
}
