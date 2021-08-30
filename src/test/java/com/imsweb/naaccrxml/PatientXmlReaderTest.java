/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;

public class PatientXmlReaderTest {

    @Test
    public void testReader() throws IOException {

        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(false);

        // one patient with no tumor
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-one-patient-no-tumor.xml")), options)) {
            Assert.assertEquals(1, reader.getRootData().getStartLineNumber().intValue());
            Assert.assertNull(reader.getRootData().getTimeGenerated());
            Assert.assertEquals(1, reader.getRootData().getValidationErrors().size());
            Patient patient = reader.readPatient();
            Assert.assertEquals(2, patient.getStartLineNumber().intValue());
            Assert.assertEquals(4, patient.getEndLineNumber().intValue());
            Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
            Assert.assertEquals(3, patient.getItem("patientIdNumber").getStartLineNumber().intValue());
            Assert.assertEquals(0, patient.getTumors().size());
            Assert.assertNull(reader.readPatient());
        }

        // one patient with one tumor
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-one-patient-one-tumor.xml")), options)) {
            Patient patient = reader.readPatient();
            Assert.assertTrue(patient.getAllValidationErrors().isEmpty());
            Assert.assertEquals(2, patient.getStartLineNumber().intValue());
            Assert.assertEquals(10, patient.getEndLineNumber().intValue());
            Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
            Assert.assertEquals(1, patient.getTumors().size());
            Assert.assertEquals(4, patient.getTumor(0).getStartLineNumber().intValue());
            Assert.assertEquals(9, patient.getTumor(0).getEndLineNumber().intValue());
            Assert.assertEquals("C123", patient.getTumor(0).getItem("primarySite").getValue());
            Assert.assertNull(reader.readPatient());
        }

        // one patient with two tumors
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-one-patient-two-tumors.xml")), options)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals(2, patient.getStartLineNumber().intValue());
            Assert.assertEquals(10, patient.getEndLineNumber().intValue());
            Assert.assertEquals("00000001", patient.getItem("patientIdNumber").getValue());
            Assert.assertEquals(2, patient.getTumors().size());
            Assert.assertEquals(4, patient.getTumor(0).getStartLineNumber().intValue());
            Assert.assertEquals(6, patient.getTumor(0).getEndLineNumber().intValue());
            Assert.assertEquals("C123", patient.getTumor(0).getItem("primarySite").getValue());
            Assert.assertEquals(7, patient.getTumor(1).getStartLineNumber().intValue());
            Assert.assertEquals(9, patient.getTumor(1).getEndLineNumber().intValue());
            Assert.assertEquals("C456", patient.getTumor(1).getItem("primarySite").getValue());
            Assert.assertNull(reader.readPatient());
        }

        // two patients with one tumor each
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-two-patients.xml")), options)) {
            Patient patient1 = reader.readPatient();
            Assert.assertEquals(3, patient1.getStartLineNumber().intValue());
            Assert.assertEquals(8, patient1.getEndLineNumber().intValue());
            Assert.assertEquals("00000001", patient1.getItem("patientIdNumber").getValue());
            Assert.assertEquals(1, patient1.getTumors().size());
            Assert.assertEquals(5, patient1.getTumor(0).getStartLineNumber().intValue());
            Assert.assertEquals(7, patient1.getTumor(0).getEndLineNumber().intValue());
            Assert.assertEquals("C123", patient1.getTumor(0).getItem("primarySite").getValue());
            Assert.assertEquals(6, patient1.getTumor(0).getItem("primarySite").getStartLineNumber().intValue());
            Patient patient2 = reader.readPatient();
            Assert.assertEquals(9, patient2.getStartLineNumber().intValue());
            Assert.assertEquals(14, patient2.getEndLineNumber().intValue());
            Assert.assertEquals("00000002", patient2.getItem("patientIdNumber").getValue());
            Assert.assertEquals(1, patient2.getTumors().size());
            Assert.assertEquals(11, patient2.getTumor(0).getStartLineNumber().intValue());
            Assert.assertEquals(13, patient2.getTumor(0).getEndLineNumber().intValue());
            Assert.assertEquals("C456", patient2.getTumor(0).getItem("primarySite").getValue());
            Assert.assertEquals(12, patient2.getTumor(0).getItem("primarySite").getStartLineNumber().intValue());
            Assert.assertNull(reader.readPatient());
        }

        // test the root data attributes
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-root-data-1.xml")), options)) {
            NaaccrData data = reader.getRootData();
            Assert.assertNotNull(data.getBaseDictionaryUri());
            Assert.assertTrue(data.getUserDictionaryUri().isEmpty());
            Assert.assertNotNull(data.getRecordType());
            Assert.assertNull(data.getTimeGenerated());
            Assert.assertEquals("1.0", data.getSpecificationVersion());
            Assert.assertEquals(0, data.getItems().size());
        }

        // another test with the root data and the new 1.1 specification
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-root-data-2.xml")), options)) {
            NaaccrData data = reader.getRootData();
            Assert.assertNotNull(data.getBaseDictionaryUri());
            Assert.assertTrue(data.getUserDictionaryUri().isEmpty());
            Assert.assertNotNull(data.getRecordType());
            Assert.assertNotNull(data.getTimeGenerated());
            Assert.assertEquals("1.1", data.getSpecificationVersion());
            Assert.assertEquals(1, data.getItems().size());
        }

        // by default, a value too long should be reported as an error but shouldn't be truncated
        NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(TestingUtils.getDataFile("dictionary/testing-user-dictionary.xml"));
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-too-long.xml")), options, dict)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("XX", patient.getItemValue("myVariable2"));
            Assert.assertFalse(patient.getAllValidationErrors().isEmpty());
            NaaccrValidationError error = patient.getAllValidationErrors().get(0);
            Assert.assertTrue(error.getMessage().contains("long"));
        }

        // multiple times the same item should throw an exception
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-duplicate-items.xml")), options)) {
            reader.readPatient();
            throw new AssertionError("Was expecting an exception here!");
        }
        catch (NaaccrIOException ex) {
            Assert.assertTrue(ex.getMessage().contains("primarySite"));
        }

        // test some special characters
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-special-characters.xml")), options)) {
            Tumor tumor = reader.readPatient().getTumor(0);
            Assert.assertEquals("& < > \" '", tumor.getItemValue("rxTextHormone"));
            Assert.assertEquals("& < > \" '", tumor.getItemValue("rxTextChemo"));

        }

        // test a data item that changed level (dateOfLastCancerStatus went from Patient to Tumor in N18 and N21)
        options.setTranslateRenamedStandardItemIds(true);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-switched-level.xml")), options)) {
            Patient patient = reader.readPatient();
            Assert.assertTrue(patient.getAllValidationErrors().isEmpty());
            Assert.assertEquals("20200101", patient.getTumor(0).getItemValue("dateOfLastCancerStatus"));
            patient = reader.readPatient();
            Assert.assertTrue(patient.getAllValidationErrors().isEmpty());
            Assert.assertEquals("10", patient.getTumor(0).getItemValue("dateOfLastCancerStatusFlag"));
            Assert.assertEquals("10", patient.getTumor(1).getItemValue("dateOfLastCancerStatusFlag"));
        }

        // bad time generated
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-bad-time-generated.xml")), options)) {
            Assert.assertFalse(reader.getRootData().getValidationErrors().get(0).getMessage().contains("{0}"));
        }
    }

    @Test
    public void testCachedRuntimeDictionary() throws IOException {

        // we are going to need all the constructor params for this test...
        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(false);
        NaaccrStreamConfiguration conf = NaaccrStreamConfiguration.getDefault();
        NaaccrDictionary dict = NaaccrXmlDictionaryUtils.readDictionary(TestingUtils.getDataFile("dictionary/testing-user-dictionary.xml"));

        // first, read the file once without a configuration
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-one-patient-no-tumor.xml")), options, dict, null)) {
            Assert.assertEquals("00000001", reader.readPatient().getItemValue("patientIdNumber"));
        }

        // then read the same file with the same data in a loop using a unique configuration (the runtime dictionary should be cached)
        for (int i = 0; i < 3; i++) {
            try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-one-patient-no-tumor.xml")), options, dict, conf)) {
                Assert.assertEquals("00000001", reader.readPatient().getItemValue("patientIdNumber"));
            }
        }
        Assert.assertNotNull(conf.getCachedDictionary());
    }

    @Test
    public void testOptions() throws IOException {

        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(false);

        // only valid items are validated, so setting it to false shouldn't affect the test...
        options.setValidateReadValues(false);

        // we want to process the unknown item
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_PROCESS);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-unk.xml")), options)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("X", patient.getItemValue("unknown"));
            Assert.assertTrue(patient.getValidationErrors().isEmpty());
        }

        // we want to ignore the unknown item
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_IGNORE);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-unk.xml")), options)) {
            Patient patient = reader.readPatient();
            Assert.assertNull(patient.getItemValue("unknown"));
            Assert.assertTrue(patient.getValidationErrors().isEmpty());
        }

        // we want to report an error for the unknown item
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_ERROR);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-unk.xml")), options)) {
            Patient patient = reader.readPatient();
            Assert.assertNull(patient.getItemValue("unknown"));
            Assert.assertFalse(patient.getValidationErrors().isEmpty());
            NaaccrValidationError error = patient.getValidationErrors().get(0);
            Assert.assertNotNull(error.getMessage());
            Assert.assertNotNull(error.getLineNumber());
            Assert.assertEquals("unknown", error.getNaaccrId());
            Assert.assertNull(error.getNaaccrNum());
            Assert.assertEquals("X", error.getValue());
        }

        // test option to auto-translate renamed IDs - option is OFF
        options.setTranslateRenamedStandardItemIds(false);
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_ERROR);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-translate-180.xml")), options)) {
            Tumor tumor = reader.readPatient().getTumor(0);
            Assert.assertNull(tumor.getItemValue("dateOfSentinelLymphNodeBiopsy")); // old ID
            Assert.assertNull(tumor.getItemValue("dateSentinelLymphNodeBiopsy")); // new ID
            Assert.assertFalse(tumor.getValidationErrors().isEmpty());
        }

        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_ERROR);

        // test option to auto-translate renamed IDs - option is ON
        options.setTranslateRenamedStandardItemIds(true);
        options.setItemIdsToTranslate(null);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-translate-180.xml")), options)) {
            Tumor tumor = reader.readPatient().getTumor(0);
            Assert.assertNull(tumor.getItemValue("dateOfSentinelLymphNodeBiopsy")); // old ID
            Assert.assertEquals("20190101", tumor.getItemValue("dateSentinelLymphNodeBiopsy")); // new ID
            Assert.assertTrue(tumor.getValidationErrors().isEmpty());
        }

        // test option to auto-translate renamed IDs - option is OFF but the standard item is being explicitly translated into something else
        options.setTranslateRenamedStandardItemIds(false);
        options.setItemIdsToTranslate(Collections.singletonMap("dateOfSentinelLymphNodeBiopsy", "dateSentinelLymphNodeBiopsy"));
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-translate-180.xml")), options)) {
            Tumor tumor = reader.readPatient().getTumor(0);
            Assert.assertNull(tumor.getItemValue("dateOfSentinelLymphNodeBiopsy")); // old ID
            Assert.assertEquals("20190101", tumor.getItemValue("dateSentinelLymphNodeBiopsy")); // new ID
            Assert.assertTrue(tumor.getValidationErrors().isEmpty());
        }

        // test option to auto-translate renamed IDs - option is ON but version is not 180 and so standard item should not be renamed
        options.setTranslateRenamedStandardItemIds(true);
        options.setItemIdsToTranslate(null);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-options-translate-160.xml")), options)) {
            Tumor tumor = reader.readPatient().getTumor(0);
            Assert.assertNull(tumor.getItemValue("dateOfSentinelLymphNodeBiopsy")); // old ID
            Assert.assertNull(tumor.getItemValue("dateSentinelLymphNodeBiopsy")); // new ID
            Assert.assertFalse(tumor.getValidationErrors().isEmpty());
        }

        // test option to throw an exception for missing dictionary
        options.setAllowMissingDictionary(false);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-1.xml")), options)) {
            reader.readPatient();
            Assert.fail("Should have been an exception here");
        }
        catch (NaaccrIOException e) {
            // expected
        }
    }

    @Test
    public void testUserDefinedDictionary() throws IOException {

        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(false);

        NaaccrDictionary dict = TestingUtils.createUserDictionary(SpecificationVersion.SPEC_1_1);
        Assert.assertNotNull(dict.getItemByNaaccrId("myVariable"));
        Assert.assertNotNull(dict.getItemByNaaccrNum(10000));

        // regular value for the extra variable
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-1.xml")), options, dict, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("01", patient.getTumor(0).getItemValue("myVariable"));
        }

        // padding (and trimming) only applies to reading from flat-file, not XML...
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-2.xml")), options, dict, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("1", patient.getTumor(0).getItemValue("myVariable"));
        }

        // create a list with two user dictionaries
        List<NaaccrDictionary> dictionaries = new ArrayList<>();
        NaaccrDictionary dict1 = new NaaccrDictionary();
        dict1.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
        dict1.setDictionaryUri("http://test.org/naaccrxml/test1.xml");
        NaaccrDictionaryItem item1 = new NaaccrDictionaryItem();
        item1.setNaaccrId("myVariable1");
        item1.setNaaccrNum(10001);
        item1.setNaaccrName("My Variable 1");
        item1.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item1.setRecordTypes("A,M,C,I");
        item1.setLength(1);
        dict1.addItem(item1);
        dictionaries.add(dict1);
        NaaccrDictionary dict2 = new NaaccrDictionary();
        dict2.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
        dict2.setDictionaryUri("http://test.org/naaccrxml/test2.xml");
        NaaccrDictionaryItem item2 = new NaaccrDictionaryItem();
        item2.setNaaccrId("myVariable2");
        item2.setNaaccrNum(10002);
        item2.setNaaccrName("My Variable 2");
        item2.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item2.setRecordTypes("A,M,C,I");
        item2.setLength(1);
        dict2.addItem(item2);
        dictionaries.add(dict2);

        // data file defines two user dictionaries -> all is good
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-mult-1.xml")), null, dictionaries)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("1", patient.getItemValue("myVariable1"));
            Assert.assertEquals("2", patient.getItemValue("myVariable2"));
            Assert.assertTrue(patient.getAllValidationErrors().isEmpty());
        }

        // data file defines two user dictionaries, but the library knows only about one of them
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_IGNORE);
        List<NaaccrDictionary> singleDictionary = Collections.singletonList(dict1);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-mult-1.xml")), options, singleDictionary)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("1", patient.getItemValue("myVariable1"));
            Assert.assertNull(patient.getItemValue("myVariable2"));
            Assert.assertTrue(patient.getAllValidationErrors().isEmpty());
        }

        // same test again, but the item handling is set to ignore unknown item, so there shouldn't an error anymore
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_ERROR);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-mult-1.xml")), options, singleDictionary)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("1", patient.getItemValue("myVariable1"));
            Assert.assertNull(patient.getItemValue("myVariable2"));
            Assert.assertFalse(patient.getAllValidationErrors().isEmpty());
        }

        // same test again, but the item handling is set to process the item, so it should still be loaded
        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_PROCESS);
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-mult-1.xml")), options, singleDictionary)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("1", patient.getItemValue("myVariable1"));
            Assert.assertEquals("2", patient.getItemValue("myVariable2"));
            Assert.assertTrue(patient.getAllValidationErrors().isEmpty());
        }

        options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_IGNORE);

        // data file defines two user dictionaries, but specs are only 1.1 so multiple dictionaries is not supported
        try (PatientXmlReader ignored = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-mult-2.xml")), null, dictionaries)) {
            Assert.fail("Was expecting an exception here");
        }
        catch (NaaccrIOException e) {
            // expected
        }

        NaaccrXmlDictionaryUtils.clearCachedDictionaries();

        // test translating a URI
        dict.setDictionaryUri("something-else");
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-1.xml")), options, dict, null)) {
            Patient patient = reader.readPatient();
            Assert.assertNull(patient.getTumor(0).getItemValue("myVariable")); // item should not be recognized
        }
        options.setDictionaryIdsToTranslate(Collections.singletonMap("http://test.org/naaccrxml/test.xml", "something-else"));
        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("xml-reader-user-dict-1.xml")), options, dict, null)) {
            Patient patient = reader.readPatient();
            Assert.assertEquals("01", patient.getTumor(0).getItemValue("myVariable"));
        }
    }

    @Test
    public void testExtensions() throws IOException {

        // to properly process extensions, we have to register them to the framework; this is done through a configuration object
        NaaccrStreamConfiguration conf = new NaaccrStreamConfiguration();
        conf.getXstream().autodetectAnnotations(true); // required only because we want to use annotation on the extension classes (it's more convenient)
        conf.registerNamespace("other", "http://whatever.org");
        conf.registerTag("other", "MyOuterTag", OuterTag.class);
        conf.registerTag("other", "MyEmbeddedEntity", EmbeddedEntity.class);
        conf.registerAttribute("other", "someAttribute", OuterTag.class, "_someAttribute", String.class);

        try (PatientXmlReader reader = new PatientXmlReader(new FileReader(TestingUtils.getDataFile("standard-file-extension.xml")), null, (NaaccrDictionary)null, conf)) {
            // there should be no error, 1 item and 2 extensions for the root
            Assert.assertTrue(reader.getRootData().getValidationErrors().isEmpty());
            Assert.assertEquals(1, reader.getRootData().getItems().size());
            Assert.assertEquals(2, reader.getRootData().getExtensions().size());
            OuterTag tag1 = ((OuterTag)reader.getRootData().getExtensions().get(0));
            Assert.assertEquals("XXX", tag1.getSomeAttribute());
            Assert.assertEquals("root-extension-1", tag1.getInnerTag());
            Assert.assertEquals(5, tag1.getStartLineNumber().intValue());
            Assert.assertNotNull(tag1.getEntity());
            Assert.assertEquals("root-extension-embedded-1", tag1.getEntity().getEntityTag());
            Assert.assertEquals(7, tag1.getEntity().getStartLineNumber().intValue());
            OuterTag tag2 = ((OuterTag)reader.getRootData().getExtensions().get(1));
            Assert.assertEquals("root-extension-2", tag2.getInnerTag());
            Assert.assertEquals(11, tag2.getStartLineNumber().intValue());

            // there should be no error, 1 item and 2 extensions for the unique patient
            Patient patient = reader.readPatient();
            Assert.assertTrue(patient.getValidationErrors().isEmpty());
            Assert.assertEquals(1, patient.getItems().size());
            Assert.assertEquals(2, patient.getExtensions().size());
            tag1 = ((OuterTag)patient.getExtensions().get(0));
            Assert.assertEquals("patient-extension-1", tag1.getInnerTag());
            Assert.assertEquals(16, tag1.getStartLineNumber().intValue());
            tag2 = ((OuterTag)patient.getExtensions().get(1));
            Assert.assertEquals("patient-extension-2", tag2.getInnerTag());
            Assert.assertEquals(19, tag2.getStartLineNumber().intValue());
            Assert.assertEquals(1, patient.getTumors().size());

            // there should be no error, 1 item and 2 extensions for the unique tumor
            Tumor tumor = patient.getTumor(0);
            Assert.assertTrue(tumor.getValidationErrors().isEmpty());
            Assert.assertEquals(1, tumor.getItems().size());
            Assert.assertEquals(2, tumor.getExtensions().size());
            tag1 = ((OuterTag)tumor.getExtensions().get(0));
            Assert.assertEquals("tumor-extension-1", tag1.getInnerTag());
            Assert.assertEquals(24, tag1.getStartLineNumber().intValue());
            tag2 = ((OuterTag)tumor.getExtensions().get(1));
            Assert.assertEquals("tumor-extension-2", tag2.getInnerTag());
            Assert.assertEquals(27, tag2.getStartLineNumber().intValue());
        }
    }

    @SuppressWarnings("unused")
    @XStreamAlias("MyOuterTag")
    private static class OuterTag extends AbstractNaaccrXmlExtension {

        @XStreamAlias("other:MyInnerTag")
        private String _innerTag;

        @XStreamAlias("other:MyEmbeddedEntity")
        private EmbeddedEntity _entity;

        @XStreamAsAttribute
        private String _someAttribute;

        public String getInnerTag() {
            return _innerTag;
        }

        public void setInnerTag(String innerTag) {
            _innerTag = innerTag;
        }

        public EmbeddedEntity getEntity() {
            return _entity;
        }

        public void setEntity(EmbeddedEntity entity) {
            _entity = entity;
        }

        public String getSomeAttribute() {
            return _someAttribute;
        }

        public void setSomeAttribute(String someAttribute) {
            this._someAttribute = someAttribute;
        }
    }

    @SuppressWarnings("unused")
    @XStreamAlias("MyEmbeddedEntity")
    private static class EmbeddedEntity extends AbstractNaaccrXmlExtension {

        @XStreamAlias("other:MyEntityTag")
        private String _entityTag;

        public String getEntityTag() {
            return _entityTag;
        }

        public void setEntityTag(String entityTag) {
            _entityTag = entityTag;
        }
    }
}
