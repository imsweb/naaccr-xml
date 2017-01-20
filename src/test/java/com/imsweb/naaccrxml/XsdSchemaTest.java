package com.imsweb.naaccrxml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Assert;
import org.junit.Test;

import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
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
        Path dir = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "data", "validity");

        Files.newDirectoryStream(dir.resolve("valid")).forEach(path -> {
            assertValidXmlFileForXsd(path.toFile());
            assertValidXmlFileForLibrary(path.toFile(), STRICT_NAMESPACE_MODE);
            assertValidXmlFileForLibrary(path.toFile(), RELAXED_NAMESPACE_MODE);
        });

        Files.newDirectoryStream(dir.resolve("invalid_relaxed")).forEach(path -> {
            assertNotValidXmlFileForXsd(path.toFile());
            assertNotValidXmlFileForLibrary(path.toFile(), STRICT_NAMESPACE_MODE);
            assertValidXmlFileForLibrary(path.toFile(), RELAXED_NAMESPACE_MODE);
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void assertValidXmlFileForXsd(File xmlFile) {
        try (FileReader reader = new FileReader(xmlFile)) {
            URL schemaXsd = Thread.currentThread().getContextClassLoader().getResource("xsd/naaccr_data_1.1.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaXsd);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(reader));
        }
        catch (Exception e) {
            Assert.fail("Was expected a valid file for '" + xmlFile.getName() + "', but it was invalid: " + e.getMessage());
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void assertNotValidXmlFileForXsd(File xmlFile) {
        try (FileReader reader = new FileReader(xmlFile)) {
            URL schemaXsd = Thread.currentThread().getContextClassLoader().getResource("xsd/naaccr_data_1.1.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaXsd);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(reader));
        }
        catch (Exception e) {
            return;
        }
        Assert.fail("Was expected an invalid file for '" + xmlFile.getName() + "', but it was valid");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertValidXmlFileForLibrary(File xmlFile, boolean useStrictNamespace) {

        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(useStrictNamespace);

        NaaccrStreamConfiguration configuration = new NaaccrStreamConfiguration();
        configuration.setAllowedTagsForNamespacePrefix("other", "MyOuterTag", "MyInnerTag");
        configuration.setAllowedTagsForNamespacePrefix("naaccr", "NaaccrData", "Patient", "Tumor", "Item");

        NaaccrDictionary userDictionary = new NaaccrDictionary();
        userDictionary.setDictionaryUri("whatever");
        userDictionary.setNaaccrVersion("140");

        try {
            try (PatientXmlReader reader = new PatientXmlReader(NaaccrXmlUtils.createReader(xmlFile), options, userDictionary, configuration)) {
                reader.getRootData();
                Patient patient = reader.readPatient();
                while (patient != null)
                    patient = reader.readPatient();
            }
        }
        catch (NaaccrIOException e) {
            Assert.fail("Was expected a valid file for '" + xmlFile.getName() + "'" + " (" + useStrictNamespace + "), but it was invalid: " + e.getMessage());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertNotValidXmlFileForLibrary(File xmlFile, boolean useStrictNamespace) {

        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(useStrictNamespace);

        NaaccrStreamConfiguration configuration = new NaaccrStreamConfiguration();
        configuration.setAllowedTagsForNamespacePrefix("other", "MyOuterTag", "MyInnerTag");
        configuration.setAllowedTagsForNamespacePrefix("naaccr", "NaaccrData", "Patient", "Tumor", "Item");

        NaaccrDictionary userDictionary = new NaaccrDictionary();
        userDictionary.setDictionaryUri("whatever");
        userDictionary.setNaaccrVersion("140");

        try {
            try (PatientXmlReader reader = new PatientXmlReader(NaaccrXmlUtils.createReader(xmlFile), options, userDictionary, configuration)) {
                reader.getRootData();
                Patient patient = reader.readPatient();
                while (patient != null)
                    patient = reader.readPatient();
            }
        }
        catch (NaaccrIOException e) {
            return;
        }
        Assert.fail("Was expected an invalid file for '" + xmlFile.getName() + "'" + " (" + useStrictNamespace + "), but it was valid");
    }

}
