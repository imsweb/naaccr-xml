/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Test;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

import com.imsweb.naaccr.api.client.NaaccrApiClient;
import com.imsweb.naaccr.api.client.entity.NaaccrDataItem;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

import static org.junit.Assert.fail;

@SuppressWarnings("ConstantConditions")
public class NaaccrXmlDictionaryUtilsTest {

    @Test
    public void testInternalDictionaries() throws IOException {

        List<String> versionsToCheckAgainstApi = List.of(NaaccrFormat.NAACCR_VERSION_LATEST);

        for (String version : NaaccrFormat.getSupportedVersions()) {
            try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-dictionary-" + version + ".xml"))) {
                NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(reader);
                Assert.assertTrue(version, NaaccrXmlDictionaryUtils.validateBaseDictionary(dict).isEmpty());
                Assert.assertTrue(version, NaaccrXmlDictionaryUtils.BASE_DICTIONARY_URI_PATTERN.matcher(dict.getDictionaryUri()).matches());
                for (NaaccrDictionaryItem item : dict.getItems())
                    if (item.getNaaccrId().length() > 50)
                        fail("Found item with ID too long: " + item.getNaaccrId());

                if (versionsToCheckAgainstApi.contains(dict.getNaaccrVersion())) {
                    Map<String, NaaccrDataItem> apiItems = new HashMap<>();
                    for (NaaccrDataItem apiItem : NaaccrApiClient.getInstance().getDataItems(dict.getNaaccrVersion().substring(0, 2))) {
                        if (apiItem.getXmlNaaccrId() != null) { // API returns old items that aren't part of the current version...
                            if (apiItem.getItemDataType() == null)
                                apiItem.setItemDataType("text"); // this feels like a mistake in the API data!

                            // definitively errors in the API data!
                            if ("derivedPediatricT".equals(apiItem.getXmlNaaccrId()) || "derivedPediatricN".equals(apiItem.getXmlNaaccrId()))
                                apiItem.setItemDataType("text");

                            if ("pathDateSpecCollect5".equals(apiItem.getXmlNaaccrId()))
                                apiItem.setItemDataType("dateTime");

                            apiItems.put(apiItem.getXmlNaaccrId(), apiItem);
                        }
                    }

                    List<String> issues = new ArrayList<>();
                    for (NaaccrDictionaryItem item : dict.getItems())
                        compareItems(item, apiItems.remove(item.getNaaccrId()), issues);
                    for (String apiId : apiItems.keySet())
                        issues.add(apiId + " was returned by the API but it was not found in local dictionary");

                    if (!issues.isEmpty())
                        Assert.fail("Found the following issues when comparing v" + dict.getNaaccrVersion() + " dictionaries:\n\n   " + String.join("\n   ", issues));
                }
            }
        }

        // clear the caches, force other tests to reload them again
        NaaccrXmlDictionaryUtils.clearCachedDictionaries();
    }

    private void compareItems(NaaccrDictionaryItem item, NaaccrDataItem apiItem, List<String> issues) {
        String prefix = item.getNaaccrId();
        if (apiItem == null)
            issues.add(prefix + " was found in local dictionary but not returned by the API");
        else {
            if (!Objects.equals(item.getNaaccrNum(), apiItem.getItemNumber()))
                issues.add(prefix + " number differs: \"" + item.getNaaccrNum() + "\" vs \"" + apiItem.getItemNumber() + "\"");
            if (!Objects.equals(item.getNaaccrName(), apiItem.getItemName().trim())) // API should trim the name!!!
                issues.add(prefix + " name differs: \"" + item.getNaaccrName() + "\" vs \"" + apiItem.getItemName() + "\"");
            if (!Objects.equals(item.getDataType(), apiItem.getItemDataType()))
                issues.add(prefix + " data type differs: \"" + item.getDataType() + "\" vs \"" + apiItem.getItemDataType() + "\"");
            if (!Objects.equals(item.getLength(), apiItem.getItemLength()))
                issues.add(prefix + " length differs: \"" + item.getLength() + "\" vs \"" + apiItem.getItemLength() + "\"");
            if (!Objects.equals(item.getParentXmlElement(), apiItem.getXmlParentId()))
                issues.add(prefix + " parent XML tag differs: \"" + item.getParentXmlElement() + "\" vs \"" + apiItem.getXmlParentId() + "\"");
            if (!Objects.equals(item.getRecordTypes().length(), apiItem.getRecordTypes().length())) // can't compare values, API returns random order of letters!
                issues.add(prefix + " record types differs: \"" + item.getRecordTypes() + "\" vs \"" + apiItem.getRecordTypes() + "\"");
        }
    }

    @Test
    public void testReadDictionary() throws IOException {

        // get a base dictionary
        NaaccrDictionary baseDictionary1 = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_140);
        NaaccrDictionary baseDictionary2 = NaaccrXmlDictionaryUtils.getBaseDictionaryByUri(baseDictionary1.getDictionaryUri());
        Assert.assertEquals(baseDictionary1.getDictionaryUri(), baseDictionary2.getDictionaryUri());
        Assert.assertEquals(baseDictionary1.getNaaccrVersion(), baseDictionary2.getNaaccrVersion());
        Assert.assertEquals(baseDictionary1.getSpecificationVersion(), baseDictionary2.getSpecificationVersion());
        Assert.assertEquals(baseDictionary1.getItems().size(), baseDictionary2.getItems().size());
        Assert.assertEquals(SpecificationVersion.SPEC_1_5, baseDictionary1.getSpecificationVersion());

        // get a default user dictionary
        NaaccrDictionary defaultUserDictionary1 = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_140);
        NaaccrDictionary defaultUserDictionary2 = NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByUri(baseDictionary1.getDictionaryUri());
        Assert.assertEquals(defaultUserDictionary1.getDictionaryUri(), defaultUserDictionary2.getDictionaryUri());
        Assert.assertEquals(defaultUserDictionary1.getNaaccrVersion(), defaultUserDictionary2.getNaaccrVersion());
        Assert.assertEquals(defaultUserDictionary1.getItems().size(), defaultUserDictionary2.getItems().size());
        Assert.assertEquals(SpecificationVersion.SPEC_1_5, defaultUserDictionary1.getSpecificationVersion());

        // make sure N18 dictionary doesn't have any references to the long IDs
        for (String oldId : NaaccrXmlDictionaryUtils.getRenamedLongNaaccr18Ids().keySet())
            Assert.assertNull(NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_180).getItemByNaaccrId(oldId));

        // optional attributes are provided as empty string
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-blank-attributes.xml"))) {
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.readDictionary(reader);
            Assert.assertEquals(1, dictionary.getItems().size());
            Assert.assertEquals(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT, dictionary.getItems().get(0).getDataType());
            Assert.assertEquals(NaaccrXmlDictionaryUtils.NAACCR_PADDING_NONE, dictionary.getItems().get(0).getPadding());
            Assert.assertEquals(NaaccrXmlDictionaryUtils.NAACCR_TRIM_ALL, dictionary.getItems().get(0).getTrim());
        }

        // read a provided user dictionary
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140.xml"))) {
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.readDictionary(reader);
            Assert.assertEquals(SpecificationVersion.SPEC_1_0, dictionary.getSpecificationVersion());
            Assert.assertEquals(4, dictionary.getItems().size());
        }

        // try to read a user dictionary with an error (bad start column)
        boolean exception = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140-bad1.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        // try to read a user dictionary with another error (NPCR item definition redefines the NAACCR number)
        exception = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140-bad2.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        // try to read a user dictionary with another error (missing dictionaryUri attribute)
        exception = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140-bad3.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        // try to read a user dictionary with another error (bad attribute name)
        exception = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-140-bad4.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        // NAACCR 22 specs 1.5 dictionary with new dateLastModified attribute
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-220.xml"))) {
            NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.readDictionary(reader);
            Assert.assertEquals(SpecificationVersion.SPEC_1_5, dictionary.getSpecificationVersion());
            Assert.assertNotNull(dictionary.getDateLastModified());
        }

        // try to read a user dictionary with another error (bad root attribute)
        exception = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-220-bad1.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        // try to read a user dictionary with another error (allowUnlimitedText for specs 1.6)
        exception = false;
        try (Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/dictionary/testing-user-dictionary-220-bad2.xml"))) {
            NaaccrXmlDictionaryUtils.readDictionary(reader);
        }
        catch (IOException e) {
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testWriteDictionary() throws IOException {

        NaaccrDictionary dict = new NaaccrDictionary();
        dict.setNaaccrVersion("140");
        dict.setDictionaryUri("whatever");
        dict.setDescription("Another whatever");
        NaaccrDictionaryItem item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(10000);
        item.setRecordTypes("A,M,C,I");
        item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_NUMERIC);
        item.setLength(2);
        item.setStartColumn(2340);
        item.setNaaccrName("My Variable");
        item.setSourceOfStandard("ME");
        item.setPadding(NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_BLANK);
        item.setTrim(NaaccrXmlDictionaryUtils.NAACCR_TRIM_NONE);
        item.setRegexValidation("0[0-8]");
        dict.addItem(item);

        // write using a writer
        File file = TestingUtils.createFile("dict-write-test.xml", false);
        try (Writer writer = new FileWriter(file)) {
            NaaccrXmlDictionaryUtils.writeDictionary(dict, writer);
        }

        NaaccrDictionary newDict = NaaccrXmlDictionaryUtils.readDictionary(file);
        Assert.assertEquals("140", newDict.getNaaccrVersion());
        Assert.assertEquals("whatever", newDict.getDictionaryUri());
        Assert.assertEquals("Another whatever", newDict.getDescription());
        Assert.assertEquals(1, newDict.getItems().size());
        Assert.assertNotNull(newDict.getItemByNaaccrId("myVariable"));
        Assert.assertNotNull(newDict.getItemByNaaccrNum(10000));

        // write using a file
        NaaccrXmlDictionaryUtils.writeDictionary(dict, file);
        newDict = NaaccrXmlDictionaryUtils.readDictionary(file);
        Assert.assertEquals("140", newDict.getNaaccrVersion());
        Assert.assertEquals("whatever", newDict.getDictionaryUri());
        Assert.assertEquals("Another whatever", newDict.getDescription());
        Assert.assertEquals(1, newDict.getItems().size());
        Assert.assertNotNull(newDict.getItemByNaaccrId("myVariable"));
        Assert.assertNotNull(newDict.getItemByNaaccrNum(10000));

        dict.setNaaccrVersion(null);
        dict.setSpecificationVersion("1.2");
        dict.setDescription(null);
        item.setRegexValidation(null);
        try (Writer writer = new FileWriter(file)) {
            NaaccrXmlDictionaryUtils.writeDictionary(dict, writer);
        }
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.readDictionary(file).getDictionaryUri());
    }

    @Test
    public void testValidateUserDictionary() {

        NaaccrDictionary dict = new NaaccrDictionary();
        dict.setNaaccrVersion("160");
        dict.setDictionaryUri("whatever");
        dict.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);

        // validate good dictionary
        NaaccrDictionaryItem item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariable");
        item.setNaaccrName("My Variable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item.setNaaccrNum(10000);
        item.setLength(1);
        dict.setItems(Collections.singletonList(item));
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateUserDictionary(dict).isEmpty());

        // this one re-defines the NPCR field but with a different number, which is not allowed
        item = new NaaccrDictionaryItem();
        item.setNaaccrId("npcrSpecificField");
        item.setNaaccrName("NPCR Specific Field");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(10000);
        item.setLength(75);
        item.setRecordTypes("A,M,C,I");
        dict.setItems(Collections.singletonList(item));
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.validateUserDictionary(dict));

        // this one defines an item that has the same number as the base
        item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariable");
        item.setNaaccrName("My Variable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(240);
        item.setLength(1);
        dict.setItems(Collections.singletonList(item));
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.validateUserDictionary(dict));
        item.setNaaccrNum(999999);
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateUserDictionary(dict).isEmpty());

        // this one defines an item that is too long
        item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariableWithSomeVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongId!");
        item.setNaaccrName("My Variable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(10000);
        item.setLength(1);
        dict.setItems(Collections.singletonList(item));
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.validateUserDictionary(dict));
        item.setNaaccrId("myVariable");
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateUserDictionary(dict).isEmpty());

        // this one defines the allowUnlimitedText with a non text data type
        dict.setSpecificationVersion(SpecificationVersion.SPEC_1_5); // 1.6 and later don't support that attribute anymore
        item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariable");
        item.setNaaccrName("My Variable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(10000);
        item.setLength(1);
        item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
        item.setAllowUnlimitedText(true);
        dict.setItems(Collections.singletonList(item));
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.validateUserDictionary(dict));
        item.setAllowUnlimitedText(false);
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateUserDictionary(dict).isEmpty());
    }

    @Test
    public void testValidateDictionaries() {

        // base dictionary by itself is valid
        NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("160");
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, Collections.emptyList()).isEmpty());

        // add two valid user-defined dictionaries
        List<NaaccrDictionary> userDictionaries = new ArrayList<>();
        NaaccrDictionary userDictionary1 = new NaaccrDictionary();
        userDictionary1.setNaaccrVersion("160");
        userDictionary1.setDictionaryUri("whatever1");
        userDictionary1.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
        NaaccrDictionaryItem item1 = new NaaccrDictionaryItem();
        item1.setNaaccrId("myVariable1");
        item1.setNaaccrName("My Variable1");
        item1.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item1.setNaaccrNum(10000);
        item1.setLength(1);
        userDictionary1.setItems(Collections.singletonList(item1));
        userDictionaries.add(userDictionary1);
        NaaccrDictionary userDictionary2 = new NaaccrDictionary();
        userDictionary2.setDictionaryUri("whatever2");
        userDictionary2.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
        NaaccrDictionaryItem item2 = new NaaccrDictionaryItem();
        item2.setNaaccrId("myVariable2");
        item2.setNaaccrName("My Variable2");
        item2.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item2.setNaaccrNum(10001);
        item2.setLength(1);
        userDictionary2.setItems(Collections.singletonList(item2));
        userDictionaries.add(userDictionary2);
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());

        // NAACCR ID repeats a base one
        item2.setNaaccrId("vitalStatus");
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
        item2.setNaaccrId("myVariable2");

        // NAACCR ID repeats a user-defined ones (other attributes are different)
        item2.setNaaccrId("myVariable1");
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
        item2.setNaaccrId("myVariable2");

        // NAACCR number repeats a base one
        item2.setNaaccrNum(10);
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
        item2.setNaaccrNum(10001);

        // NAACCR number repeats a user-defined one (other attributes are different)
        item2.setNaaccrNum(10000);
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
        item2.setNaaccrNum(10001);

        // items overlap
        item1.setStartColumn(2340);
        item2.setStartColumn(2340);
        Assert.assertFalse(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());
        item1.setStartColumn(null);
        item2.setStartColumn(null);

        NaaccrDictionary userDictionary3 = new NaaccrDictionary();
        userDictionary3.setNaaccrVersion("160");
        userDictionary3.setDictionaryUri("whatever3");
        userDictionary3.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
        NaaccrDictionaryItem item3 = new NaaccrDictionaryItem();
        item3.setNaaccrId("myVariable1");
        item3.setNaaccrName("My Variable1");
        item3.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item3.setNaaccrNum(10000);
        item3.setLength(1);
        userDictionary3.setItems(Collections.singletonList(item3));
        userDictionaries.add(userDictionary3);

        // NAACCR ID repeats a user-defined ones (other attributes are the same)
        Assert.assertTrue(NaaccrXmlDictionaryUtils.validateDictionaries(baseDictionary, userDictionaries).isEmpty());

    }

    @Test
    public void testValidationRegex() {

        //  "alpha": uppercase letters, A-Z, no spaces, full length needs to be filled in
        Pattern pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_ALPHA);
        Assert.assertTrue(pattern.matcher("A").matches());
        Assert.assertTrue(pattern.matcher("AVALUE").matches());
        Assert.assertFalse(pattern.matcher("A VALUE").matches());
        Assert.assertFalse(pattern.matcher(" A").matches());
        Assert.assertFalse(pattern.matcher("A ").matches());
        Assert.assertFalse(pattern.matcher("a").matches());
        Assert.assertFalse(pattern.matcher("a value").matches());
        Assert.assertFalse(pattern.matcher("123").matches());
        Assert.assertFalse(pattern.matcher("A123").matches());
        Assert.assertFalse(pattern.matcher("123A").matches());
        Assert.assertFalse(pattern.matcher("A!").matches());

        // "digits": digits, 0-9, no spaces, full length needs to be filled in
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
        Assert.assertTrue(pattern.matcher("1").matches());
        Assert.assertTrue(pattern.matcher("123").matches());
        Assert.assertFalse(pattern.matcher("12 3").matches());
        Assert.assertFalse(pattern.matcher(" 1").matches());
        Assert.assertFalse(pattern.matcher("1 ").matches());
        Assert.assertFalse(pattern.matcher("a value").matches());
        Assert.assertFalse(pattern.matcher("1A23").matches());
        Assert.assertFalse(pattern.matcher("A123").matches());
        Assert.assertFalse(pattern.matcher("123A").matches());
        Assert.assertFalse(pattern.matcher("1!").matches());

        // "mixed": uppercase letters or digits, A-Z,0-9, no spaces, full length needs to be filled in
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_MIXED);
        Assert.assertTrue(pattern.matcher("A").matches());
        Assert.assertTrue(pattern.matcher("AVALUE").matches());
        Assert.assertFalse(pattern.matcher("A VALUE").matches());
        Assert.assertFalse(pattern.matcher(" A").matches());
        Assert.assertFalse(pattern.matcher("A ").matches());
        Assert.assertFalse(pattern.matcher("a").matches());
        Assert.assertFalse(pattern.matcher("a value").matches());
        Assert.assertTrue(pattern.matcher("123").matches());
        Assert.assertTrue(pattern.matcher("A123").matches());
        Assert.assertTrue(pattern.matcher("123A").matches());
        Assert.assertFalse(pattern.matcher("A!").matches());

        // "numeric": digits, 0-9 with optional period, no spaces but value can be smaller than the length
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_NUMERIC);
        Assert.assertTrue(pattern.matcher("1").matches());
        Assert.assertTrue(pattern.matcher("123").matches());
        Assert.assertFalse(pattern.matcher("12 3").matches());
        Assert.assertFalse(pattern.matcher(" 1").matches());
        Assert.assertFalse(pattern.matcher("1 ").matches());
        Assert.assertFalse(pattern.matcher("a value").matches());
        Assert.assertFalse(pattern.matcher("1A23").matches());
        Assert.assertFalse(pattern.matcher("A123").matches());
        Assert.assertFalse(pattern.matcher("123A").matches());
        Assert.assertFalse(pattern.matcher("1!").matches());
        Assert.assertTrue(pattern.matcher("1.0").matches());
        Assert.assertTrue(pattern.matcher("0.123").matches());
        Assert.assertFalse(pattern.matcher(".123").matches());
        Assert.assertFalse(pattern.matcher("1.").matches());

        // "text": no checking on this value
        Assert.assertNull(NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT));

        // "date": digits, YYYY or YYYYMM or YYYYMMDD
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DATE);
        Assert.assertTrue(pattern.matcher("20100615").matches());
        Assert.assertTrue(pattern.matcher("201006").matches());
        Assert.assertTrue(pattern.matcher("2010").matches());
        Assert.assertFalse(pattern.matcher("201006  ").matches());
        Assert.assertFalse(pattern.matcher("2010  15").matches());
        Assert.assertFalse(pattern.matcher("    0615").matches());
        Assert.assertFalse(pattern.matcher("0615").matches());
        Assert.assertFalse(pattern.matcher("15").matches());
        Assert.assertFalse(pattern.matcher("A").matches());
        Assert.assertFalse(pattern.matcher("20100615!").matches());
        Assert.assertFalse(pattern.matcher("17000615").matches());
        Assert.assertFalse(pattern.matcher("20101315").matches());
        Assert.assertFalse(pattern.matcher("20100632").matches());

        // "dateTime": HL7 FHIR date with optional timestamp portion
        pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DATE_TIME);

        Assert.assertTrue(pattern.matcher("2010").matches()); // year only
        Assert.assertTrue(pattern.matcher("2010-06").matches()); // year and month
        Assert.assertTrue(pattern.matcher("2010-06-15").matches()); // year, month and day (but not time)
        Assert.assertTrue(pattern.matcher("2010-06-15T13:45:30-04:00").matches()); // UTC minus 4 hours
        Assert.assertTrue(pattern.matcher("2010-06-15T13:45:30+05:30").matches()); // UTC plus 5 1/2 hours
        Assert.assertTrue(pattern.matcher("2010-06-15T13:45:30Z").matches()); // UTC

        Assert.assertFalse(pattern.matcher("2010-06-15T00:00:30.000Z").matches()); // UTC with milliseconds - second fractions not allowed anymore
        Assert.assertFalse(pattern.matcher("2010-06-15T13:45:30.001-05:00").matches()); // UTC minus 5 hours, with 1/1000 of seconds (milliseconds) - second fractions not allowed anymore
        Assert.assertFalse(pattern.matcher("2010-06-15T13:45:30.0-05:00").matches()); // UTC minus 5 hours, with 1/10 of seconds - second fractions not allowed anymore
        Assert.assertFalse(pattern.matcher("2010-06-15T13:45:30.000000001-05:00").matches()); // UTC minus 5 hours, with 1/10000000000 of seconds - second fractions not allowed anymore

        Assert.assertFalse(pattern.matcher("20100615").matches()); // dateTime uses dashes...
        Assert.assertFalse(pattern.matcher("2010-06-15T14:10").matches()); // timezone is required if time is provided
        Assert.assertFalse(pattern.matcher("2010-06-  ").matches()); // no missing parts allowed
        Assert.assertFalse(pattern.matcher("2010-  -15").matches()); // no missing parts allowed
        Assert.assertFalse(pattern.matcher("    -06-15").matches()); // no missing parts allowed
        Assert.assertFalse(pattern.matcher("06-15").matches()); // no missing parts allowed
        Assert.assertFalse(pattern.matcher("15").matches()); // no missing parts allowed
        Assert.assertFalse(pattern.matcher("A").matches()); // garbage
        Assert.assertFalse(pattern.matcher("2010-06-15!").matches()); // garbage
        Assert.assertFalse(pattern.matcher("2010-13-15").matches()); // invalid month
        Assert.assertFalse(pattern.matcher("2010-06-32").matches()); // invalid day
        Assert.assertFalse(pattern.matcher("2001-10-26T21:32").matches()); // missing seconds
        Assert.assertFalse(pattern.matcher("2001-10-26T25:32:52+02:00").matches()); // wrong hour
        Assert.assertFalse(pattern.matcher("2024-05-17T1:45:30.001Z").matches()); // hours need to be 2 digits
        Assert.assertFalse(pattern.matcher("01-10-26T21:32").matches()); // wrong year and missing seconds
        Assert.assertFalse(pattern.matcher("2001-10-26T21:32:52+2:00").matches()); // timezone requires 2 digits
        Assert.assertFalse(pattern.matcher("2024-05-17T13:45:30.-05:00").matches()); // if millisecond period is there, there must be at least one digit
        Assert.assertFalse(pattern.matcher("2024-05-17T13:45:30.0000000001-05:00").matches()); // only 9 digits are allowed for millisecond
    }

    @Test
    public void testCreateNaaccrIdFromItemName() {
        Assert.assertEquals("", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName(""));
        Assert.assertEquals("testTestTest", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("Test Test Test"));
        Assert.assertEquals("testSomeThingElse123", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("test: (ignored);   some_thing # else --123!!!"));

        Assert.assertEquals("phase1NumberOfFractions", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("Phase I Number of Fractions"));
        Assert.assertEquals("lnHeadAndNeckLevels6To7", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("LN Head and Neck Levels VI-VII"));
        Assert.assertEquals("headNeck", NaaccrXmlDictionaryUtils.createNaaccrIdFromItemName("Head&Neck"));
    }

    @Test
    public void testGetMergedDictionaries() {
        Assert.assertNotNull(NaaccrXmlDictionaryUtils.getMergedDictionaries(NaaccrFormat.NAACCR_VERSION_160));
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testWriteDictionaryToCsv() throws IOException {
        NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(NaaccrFormat.NAACCR_VERSION_LATEST);
        File file = new File(TestingUtils.getBuildDirectory(), "dictionary.csv");
        NaaccrXmlDictionaryUtils.writeDictionaryToCsv(dictionary, file);

        try (CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(file.toPath())) {
            Assert.assertFalse(reader.stream().toString().isEmpty());
        }
        finally {
            file.delete();
        }
    }

    @Test
    public void testStandardDictionaries() throws IOException {

        // this test is more like a lab; a quick verification that is used when a base dictionary is added/modified; it doesn't need to run during the integration build...
        if (!SystemUtils.IS_OS_WINDOWS)
            return;

        for (String version : NaaccrFormat.getSupportedVersions()) {
            Path path1 = Paths.get(TestingUtils.getWorkingDirectory() + "/src/main/resources/naaccr-dictionary-" + version + ".xml");
            Path path2 = Paths.get(TestingUtils.getWorkingDirectory() + "/build/tmp-dictionary-" + version + ".xml");
            NaaccrXmlDictionaryUtils.writeDictionary(NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(version), path2.toFile());
            if (!TestingUtils.readFileAsOneString(path1.toFile()).replace("\r", "").equals(TestingUtils.readFileAsOneString(path2.toFile()).replace("\r", "")))
                Assert.fail("Dictionary for version " + version + " needs to be re-created, it contains differences from what would be created by the library!");

            path1 = Paths.get(TestingUtils.getWorkingDirectory() + "/src/main/resources/user-defined-naaccr-dictionary-" + version + ".xml");
            if (path1.toFile().exists()) {
                path2 = Paths.get("build/tmp-dictionary-" + version + ".xml");
                NaaccrXmlDictionaryUtils.writeDictionary(NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(version), path2.toFile());
                if (!TestingUtils.readFileAsOneString(path1.toFile()).replace("\r", "").equals(TestingUtils.readFileAsOneString(path2.toFile()).replace("\r", "")))
                    Assert.fail("User dictionary for version " + version + " needs to be re-created, it contains differences from what would be created by the library!");
            }
        }
    }
}
