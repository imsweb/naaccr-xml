/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryGroupedItem;

public class RecreateStandardDictionaries {

    public static void main(String[] args) throws IOException {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            //if (!"160".equals(version))
            //    continue;
            Path path = Paths.get("src/main/resources/naaccr-dictionary-" + version + ".xml");
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.readDictionary(path.toFile());
            applyFix(dictionary, true);
            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());

            path = Paths.get("src/main/resources/user-defined-naaccr-dictionary-" + version + ".xml");
            dictionary = NaaccrXmlDictionaryUtils.readDictionary(path.toFile());
            applyFix(dictionary, false);
            NaaccrXmlDictionaryUtils.writeDictionary(dictionary, path.toFile());
        }
    }

    private static void applyFix(NaaccrDictionary dictionary, boolean isBase) {
        // default is to do nothing

        if (isBase) {

            NaaccrDictionaryGroupedItem item = new NaaccrDictionaryGroupedItem();
            item.setNaaccrName("Morph--Type&Behav ICD-O-2");
            item.setNaaccrId(NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(item.getNaaccrName()));
            item.setNaaccrNum(419);
            item.setContains("histologyIcdO2,behaviorIcdO2");
            item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            item.setRecordTypes("A,M,C,I");
            item.setStartColumn(545);
            item.setLength(5);
            dictionary.addGroupedItem(item);

            item = new NaaccrDictionaryGroupedItem();
            item.setNaaccrName("Morph--Type&Behav ICD-O-3");
            item.setNaaccrId(NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(item.getNaaccrName()));
            item.setNaaccrNum(521);
            item.setContains("histologicTypeIcdO3,behaviorCodeIcdO3");
            item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            item.setRecordTypes("A,M,C,I");
            item.setStartColumn(550);
            item.setLength(5);
            dictionary.addGroupedItem(item);

            item = new NaaccrDictionaryGroupedItem();
            item.setNaaccrName("Extent of Disease 10-Dig");
            item.setNaaccrId(NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(item.getNaaccrName()));
            item.setNaaccrNum(779);
            item.setContains("eodTumorSize,eodExtension,eodExtensionProstPath,eodLymphNodeInvolv,regionalNodesPositive,regionalNodesExamined");
            item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            item.setRecordTypes("A,M,C,I");
            item.setStartColumn(909);
            item.setLength(12);
            dictionary.addGroupedItem(item);

            item = new NaaccrDictionaryGroupedItem();
            item.setNaaccrName("Subsq RX 2nd Course Codes");
            item.setNaaccrId(NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(item.getNaaccrName()));
            item.setNaaccrNum(1670);
            item.setContains(
                    "subsqRx2ndCourseSurg,subsqRx2ndScopeLnSu,subsqRx2ndSurgOth,subsqRx2ndRegLnRem,subsqRx2ndCourseRad,subsqRx2ndCourseChemo,subsqRx2ndCourseHorm,subsqRx2ndCourseBrm,subsqRx2ndCourseOth");
            item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            item.setRecordTypes("A,M,C,I");
            item.setStartColumn(1734);
            item.setLength(11);
            dictionary.addGroupedItem(item);

            item = new NaaccrDictionaryGroupedItem();
            item.setNaaccrName("Subsq RX 3rd Course Codes");
            item.setNaaccrId(NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(item.getNaaccrName()));
            item.setNaaccrNum(1690);
            item.setContains(
                    "subsqRx3rdCourseSurg,subsqRx3rdScopeLnSu,subsqRx3rdSurgOth,subsqRx3rdRegLnRem,subsqRx3rdCourseRad,subsqRx3rdCourseChemo,subsqRx3rdCourseHorm,subsqRx3rdCourseBrm,subsqRx3rdCourseOth");
            item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            item.setRecordTypes("A,M,C,I");
            item.setStartColumn(1755);
            item.setLength(11);
            dictionary.addGroupedItem(item);

            item = new NaaccrDictionaryGroupedItem();
            item.setNaaccrName("Subsq RX 4th Course Codes");
            item.setNaaccrId(NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(item.getNaaccrName()));
            item.setNaaccrNum(1710);
            item.setContains(
                    "subsqRx4thCourseSurg,subsqRx4thScopeLnSu,subsqRx4thSurgOth,subsqRx4thRegLnRem,subsqRx4thCourseRad,subsqRx4thCourseChemo,subsqRx4thCourseHorm,subsqRx4thCourseBrm,subsqRx4thCourseOth");
            item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            item.setRecordTypes("A,M,C,I");
            item.setStartColumn(1776);
            item.setLength(11);
            dictionary.addGroupedItem(item);

            item = new NaaccrDictionaryGroupedItem();
            item.setNaaccrName("Morph (73-91) ICD-O-1");
            item.setNaaccrId(NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(item.getNaaccrName()));
            item.setNaaccrNum(1970);
            item.setContains("histologyIcdO1,behaviorIcdO1,gradeIcdO1");
            item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            item.setRecordTypes("A,M,C,I");
            item.setStartColumn(1913);
            item.setLength(6);
            dictionary.addGroupedItem(item);
        }
    }
}
