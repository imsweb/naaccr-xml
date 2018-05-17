package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStreamException;

import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;

/**
 * The purpose of this test is to make sure that the built-in validation behaves the same way as the 3WC XSD schema one.
 */
public class XsdSchemaTest {

    // used for readability
    public static final boolean STRICT_NAMESPACE_MODE = true;
    public static final boolean RELAXED_NAMESPACE_MODE = false;

    @Test
    public void testXsdAgainstLibrary() throws IOException {
        Path dir = Paths.get(TestingUtils.getWorkingDirectory() + "/src/test/resources/data/validity");

        // files in this folder are supposed to be valid according to the current specs
        Files.newDirectoryStream(dir.resolve("valid")).forEach(path -> {
            assertValidXmlFileForXsd(path.toFile());
            assertValidXmlFileForLibrary(path.toFile(), STRICT_NAMESPACE_MODE);
            assertValidXmlFileForLibrary(path.toFile(), RELAXED_NAMESPACE_MODE);
        });

        // files in this folder are supposed to be invalid in strict mode, but still valid in relaxed mode
        Files.newDirectoryStream(dir.resolve("invalid_relaxed")).forEach(path -> {
            assertNotValidXmlFileForXsd(path.toFile());
            assertNotValidXmlFileForLibrary(path.toFile(), STRICT_NAMESPACE_MODE);
            assertValidXmlFileForLibrary(path.toFile(), RELAXED_NAMESPACE_MODE);
        });

        // files in this folder are supposed to be invalid according to the current specs
        Files.newDirectoryStream(dir.resolve("invalid")).forEach(path -> {
            assertNotValidXmlFileForXsd(path.toFile());
            assertNotValidXmlFileForLibrary(path.toFile(), STRICT_NAMESPACE_MODE);
            assertNotValidXmlFileForLibrary(path.toFile(), RELAXED_NAMESPACE_MODE);
        });

        // files in this folder are supposed to be invalid but only for the library (the errors can't be detected with just an XSD)
        Files.newDirectoryStream(dir.resolve("invalid_library_only")).forEach(path -> {
            assertNotValidXmlFileForLibrary(path.toFile(), STRICT_NAMESPACE_MODE);
            assertNotValidXmlFileForLibrary(path.toFile(), RELAXED_NAMESPACE_MODE);
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void assertValidXmlFileForXsd(File xmlFile) {
        try (FileReader reader = new FileReader(xmlFile)) {
            createSchema().newValidator().validate(new StreamSource(reader));
        }
        catch (Exception e) {
            Assert.fail("Was expected a valid file for '" + xmlFile.getName() + "', but it was invalid: " + e.getMessage());
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void assertNotValidXmlFileForXsd(File xmlFile) {
        try (FileReader reader = new FileReader(xmlFile)) {
            createSchema().newValidator().validate(new StreamSource(reader));
        }
        catch (Exception e) {
            return;
        }
        Assert.fail("Was expected an invalid file for '" + xmlFile.getName() + "', but it was valid");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertValidXmlFileForLibrary(File xmlFile, boolean useStrictNamespace) {
        try {
            String libraryError = null;
            try (PatientXmlReader reader = new PatientXmlReader(NaaccrXmlUtils.createReader(xmlFile), createOptions(useStrictNamespace), createUserDictionary(), createConfiguration())) {
                reader.getRootData();
                Patient patient = reader.readPatient();
                while (patient != null) {
                    if (!patient.getAllValidationErrors().isEmpty())
                        libraryError = patient.getAllValidationErrors().iterator().next().getMessage();
                    patient = reader.readPatient();
                }
            }
            if (libraryError != null)
                Assert.fail("Was expected a valid file for '" + xmlFile.getName() + "' but found a an error on one of the patients:\n  " + libraryError);
        }
        catch (XStreamException | NaaccrIOException e) {
            Assert.fail("Was expected a valid file for '" + xmlFile.getName() + "'  with strict namespace set to " + useStrictNamespace + ", but it was invalid:\n  " + e.getMessage());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertNotValidXmlFileForLibrary(File xmlFile, boolean useStrictNamespace) {
        String libraryError = null;
        try {
            try (PatientXmlReader reader = new PatientXmlReader(NaaccrXmlUtils.createReader(xmlFile), createOptions(useStrictNamespace), createUserDictionary(), createConfiguration())) {
                reader.getRootData();
                Patient patient = reader.readPatient();
                while (patient != null) {
                    if (!patient.getAllValidationErrors().isEmpty())
                        libraryError = patient.getAllValidationErrors().iterator().next().getMessage();
                    patient = reader.readPatient();
                }
            }
        }
        catch (XStreamException | NaaccrIOException e) {
            return;
        }
        if (libraryError == null)
            Assert.fail("Was expected an invalid file for '" + xmlFile.getName() + "' with strict namespace set to " + useStrictNamespace + ", but it was valid");
    }

    @SuppressWarnings("ConstantConditions")
    private Schema createSchema() throws SAXException {
        return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(Thread.currentThread().getContextClassLoader().getResource("xsd/naaccr_data_1.1.xsd"));
    }

    private NaaccrStreamConfiguration createConfiguration() {
        NaaccrStreamConfiguration configuration = new NaaccrStreamConfiguration();

        configuration.registerNamespace("other", "http://whatever.org");
        configuration.registerTag("other", "MyOuterTag", MyOuterTag.class);
        configuration.registerTag("other", "MyInnerTag", MyOuterTag.class, "_myInnerTag", String.class);

        configuration.registerNamespace("naaccr", "http://naaccr.org/naaccrxml");
        configuration.registerTag("naaccr", "NaaccrData", NaaccrData.class);
        configuration.registerTag("naaccr", "Patient", Patient.class);
        configuration.registerTag("naaccr", "Tumor", Tumor.class);
        configuration.registerTag("naaccr", "Item", Item.class);

        return configuration;
    }

    private NaaccrOptions createOptions(boolean useStrictNamespace) {
        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(useStrictNamespace);
        return options;
    }

    private NaaccrDictionary createUserDictionary() {
        NaaccrDictionary userDictionary = new NaaccrDictionary();
        userDictionary.setSpecificationVersion(SpecificationVersion.SPEC_1_1);
        userDictionary.setDictionaryUri("whatever");

        NaaccrDictionaryItem item = new NaaccrDictionaryItem();
        item.setNaaccrId("whateverItem");
        item.setNaaccrName("Whatever Item");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        item.setNaaccrNum(10000);
        item.setRecordTypes("A,M,C,I");
        item.setLength(1);
        userDictionary.addItem(item);

        return userDictionary;
    }

    public static class MyOuterTag {

        public String _myInnerTag;

    }
}
