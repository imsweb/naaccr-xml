/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

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

            File file = new File(TestingUtils.getWorkingDirectory() + "/docs/naaccr-23/N23 Copy of XML Group report.csv");
            try (CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(file.toPath())) {
                reader.stream().forEach(line -> {
                    if (line.getField(0).isEmpty())
                        return;

                    Integer length = Integer.parseInt(line.getField(3));
                    Integer num = Integer.parseInt(line.getField(1));
                    String name = line.getField(2).trim();
                    String id = line.getField(0);
                    String level = line.getField(4);

                    NaaccrDictionaryItem item = dictionary.getItemByNaaccrId(id);
                    if (item == null) {
                        if (!"New".equals(line.getField(8)))
                            System.out.println("!!! Unable to find " + id);
                        else {
                            String type = NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT;
                            if (id.equals("noPatientContactFlag") || id.equals("reportingFacilityRestrictionFlag") || id.equals("histologicSubtype"))
                                type = NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS;
                            else if (id.equals("rxHospSurgPrimSite2023") || id.equals("rxSummSurgPrimSite2023"))
                                type = NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_MIXED;

                            item = new NaaccrDictionaryItem();
                            item.setNaaccrId(id);
                            item.setNaaccrName(name);
                            item.setParentXmlElement(level);
                            item.setNaaccrNum(num);
                            item.setRecordTypes("A,M,C,I");
                            item.setLength(length);
                            item.setDataType(type);
                            if (id.equals("clinicalMarginWidth"))
                                item.setPadding(NaaccrXmlDictionaryUtils.NAACCR_PADDING_LEFT_BLANK);
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
                    itemIds.remove(Objects.requireNonNull(item).getNaaccrId());
                });
            }

            for (String id : itemIds)
                System.out.println("  > removed item " + id);

            dictionary.setItems(newItems.stream().sorted(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId)).collect(Collectors.toList()));

            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());
        }
    }
}
