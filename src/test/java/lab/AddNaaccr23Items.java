/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.TestingUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class AddNaaccr23Items {

    /**
     * > updated name for rxHospSurgPrimSite from "RX Hosp--Surg Prim Site" to "RX Hosp--Surg Prim Site 03-2022"
     * > added new item: rxHospSurgPrimSite2023
     * > updated name for rxSummSurgPrimSite from "RX Summ--Surg Prim Site" to "RX Summ--Surg Prim Site 03-2022"
     * > added new item: rxSummSurgPrimSite2023
     * > added new item: noPatientContactFlag
     * > added new item: reportingFacilityRestrictionFlag
     * > updated length for ehrReporting from 1000 to 4000
     * > updated length for textDxProcPe from 1000 to 4000
     * > updated length for textDxProcXRayScan from 1000 to 4000
     * > updated length for textDxProcScopes from 1000 to 4000
     * > updated length for textDxProcLabTests from 1000 to 4000
     * > updated length for textDxProcOp from 1000 to 4000
     * > updated length for textDxProcPath from 1000 to 4000
     * > updated length for textStaging from 1000 to 4000
     * > updated length for rxTextSurgery from 1000 to 4000
     * > updated length for rxTextRadiation from 1000 to 4000
     * > updated length for rxTextRadiationOther from 1000 to 4000
     * > updated length for rxTextChemo from 1000 to 4000
     * > updated length for rxTextHormone from 1000 to 4000
     * > updated length for rxTextBrm from 1000 to 4000
     * > updated length for rxTextOther from 1000 to 4000
     * > updated length for textRemarks from 1000 to 4000
     * > added new item: histologicSubtype
     * > added new item: clinicalMarginWidth
     */

    public static void main(String[] args) throws Exception {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            if (!"230".equals(version))
                continue;

            Path path = Paths.get("src/main/resources/naaccr-dictionary-" + version + ".xml");
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.readDictionary(path.toFile());
            dictionary.getItemByNaaccrId("force-caching");

            Set<String> itemIds = dictionary.getItems().stream().map(NaaccrDictionaryItem::getNaaccrId).collect(Collectors.toSet());

            List<NaaccrDictionaryItem> newItems = new ArrayList<>();
            try (CSVReader reader = new CSVReader(new FileReader(TestingUtils.getWorkingDirectory() + "/docs/naaccr-23/N23 Copy of XML Group report.csv"))) {
                String[] line = reader.readNext(); // ignore headers

                line = reader.readNext();
                while (line != null) {
                    if (!line[0].isEmpty()) {

                        Integer length = Integer.parseInt(line[3]);
                        Integer num = Integer.parseInt(line[1]);
                        String name = line[2].trim();
                        String id = line[0];
                        String level = line[4];

                        NaaccrDictionaryItem item = dictionary.getItemByNaaccrId(id);
                        if (item == null) {
                            if (!"New".equals(line[8]))
                                System.out.println("!!! Unable to find " + id);
                            else {
                                String type = NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT;
                                if (id.equals("noPatientContactFlag") || id.equals("reportingFacilityRestrictionFlag") || id.equals("histologicSubtype"))
                                    type = NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS;

                                item = new NaaccrDictionaryItem();
                                item.setNaaccrId(id);
                                item.setNaaccrName(name);
                                item.setParentXmlElement(level);
                                item.setNaaccrNum(num);
                                item.setRecordTypes("A,M,C,I");
                                item.setLength(length);
                                item.setDataType(type);
                                dictionary.addItem(item);
                                System.out.println(" > added new item: " + item.getNaaccrId());
                            }
                        }
                        else {
                            item.setAllowUnlimitedText(null);

                            if (!item.getLength().equals(length) && !"placeOfDeath".equals(item.getNaaccrId())) {
                                System.out.println(" > updated length for " + item.getNaaccrId() + " from " + item.getLength() + " to " + length);
                                item.setLength(length);
                            }

                            if (!item.getNaaccrName().equals(name)) {
                                System.out.println(" > updated name for " + item.getNaaccrId() + " from \"" + item.getNaaccrName() + "\" to \"" + name + "\"");
                                item.setNaaccrName(name);
                            }

                            if (!item.getParentXmlElement().equals(level))
                                System.out.println("  !!! wrong level for " + item.getNaaccrId() + "; expected " + item.getParentXmlElement() + " but got " + level);
                        }

                        newItems.add(item);
                        itemIds.remove(item.getNaaccrId());
                    }

                    line = reader.readNext();
                }
            }

            for (String id : itemIds)
                System.out.println("  > removed item " + id);


            dictionary.setItems(newItems.stream().sorted(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId)).collect(Collectors.toList()));

            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());
        }
    }
}
