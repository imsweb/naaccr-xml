/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package lab;

import java.io.File;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public class FixDataTypesLab {

    // fix for issue #46
    public static void main(String[] args) throws Exception {
        for (String version : NaaccrFormat.getSupportedVersions()) {
            File dictFile = new File(System.getProperty("user.dir") + "/src/main/resources/naaccr-dictionary-" + version + ".xml");
            NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(dictFile);

            dict.getItemByNaaccrId("countyAtDx").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            dict.getItemByNaaccrId("censusBlockGrp197090").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            dict.getItemByNaaccrId("censusBlockGroup2000").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            dict.getItemByNaaccrId("abstractedBy").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("eodExtension").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("eodLymphNodeInvolv").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("eodOld13Digit").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("eodOld2Digit").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("eodOld4Digit").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("csVersionInputCurrent").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            dict.getItemByNaaccrId("csVersionInputOriginal").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            dict.getItemByNaaccrId("diagnosticProc7387").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("countyCurrent").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
            dict.getItemByNaaccrId("placeOfDeath").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("birthplace").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("namePrefix").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            dict.getItemByNaaccrId("dcStateFileNumber").setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);

            NaaccrXmlDictionaryUtils.writeDictionary(dict, dictFile);
        }
    }
}
