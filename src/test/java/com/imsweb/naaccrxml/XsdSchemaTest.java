package com.imsweb.naaccrxml;

import java.io.File;
import java.net.URL;

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
    public void testXsdAgainstLibrary() {

        // standard file
        assertValidXmlFileForXsd("standard-file.xml");
        assertValidXmlFileForLibrary("standard-file.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("standard-file.xml", RELAXED_NAMESPACE_MODE);

        // standard file with different attributes including an extra one (to be valid, it needs to be defined in a different namespace)
        assertValidXmlFileForXsd("standard-file-extra-attributes.xml");
        assertValidXmlFileForLibrary("standard-file-extra-attributes.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("standard-file-extra-attributes.xml", RELAXED_NAMESPACE_MODE);

        // standard file with an extra attribute not defining a namespace prefix, not allowed by XSD
        assertNotValidXmlFileForXsd("standard-file-extra-attributes-missing-namespace1.xml");
        assertNotValidXmlFileForLibrary("standard-file-extra-attributes-missing-namespace1.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("standard-file-extra-attributes-missing-namespace1.xml", RELAXED_NAMESPACE_MODE); // OK in relaxed namespace mode

        // standard file with an extra attribute defining a namespace prefix but that prefix is not properly defined, not allowed by XSD
        assertNotValidXmlFileForXsd("standard-file-extra-attributes-missing-namespace2.xml");
        assertNotValidXmlFileForLibrary("standard-file-extra-attributes-missing-namespace2.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("standard-file-extra-attributes-missing-namespace2.xml", RELAXED_NAMESPACE_MODE); // // OK in relaxed namespace mod

        // file that doesn't define any namespace; not allowed by XSD
        assertNotValidXmlFileForXsd("namespace-missing.xml");
        assertNotValidXmlFileForLibrary("namespace-missing.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("namespace-missing.xml", RELAXED_NAMESPACE_MODE); // OK in relaxed namespace mod

        // file that defines the NAACCR namespace and doesn't use a prefix (default namespace), should be valid in both
        assertValidXmlFileForXsd("namespace-without-prefix.xml");
        assertValidXmlFileForLibrary("namespace-without-prefix.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("namespace-without-prefix.xml", RELAXED_NAMESPACE_MODE);

        // file that defines the NAACCR namespace and uses a prefix; should be valid in both
        assertValidXmlFileForXsd("namespace-with-prefix.xml");
        assertValidXmlFileForLibrary("namespace-with-prefix.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("namespace-with-prefix.xml", RELAXED_NAMESPACE_MODE);

        // this file has no items, that's OK
        assertValidXmlFileForXsd("no-items.xml");
        assertValidXmlFileForLibrary("no-items.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("no-items.xml", RELAXED_NAMESPACE_MODE);

        // this file has no patient, that's OK
        assertValidXmlFileForXsd("no-patients.xml");
        assertValidXmlFileForLibrary("no-patients.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("no-patients.xml", RELAXED_NAMESPACE_MODE);

        // this file has no tumors, that's OK
        assertValidXmlFileForXsd("no-tumors.xml");
        assertValidXmlFileForLibrary("no-tumors.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("no-tumors.xml", RELAXED_NAMESPACE_MODE);

        // extensions - not valid because doesn't define the namespace
        assertNotValidXmlFileForXsd("extension-missing-namespace.xml");
        assertNotValidXmlFileForLibrary("extension-missing-namespace.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("extension-missing-namespace.xml", RELAXED_NAMESPACE_MODE); // OK in relaxed namespace mod

        // this file has a root extension that should be ignored
        assertValidXmlFileForXsd("extension-root.xml");
        assertValidXmlFileForLibrary("extension-root.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("extension-root.xml", RELAXED_NAMESPACE_MODE);

        // this file has a patient extension that should be ignored
        assertValidXmlFileForXsd("extension-patient.xml");
        assertValidXmlFileForLibrary("extension-patient.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("extension-patient.xml", RELAXED_NAMESPACE_MODE);

        // this file has a tumor extension that should be ignored
        assertValidXmlFileForXsd("extension-tumor.xml");
        assertValidXmlFileForLibrary("extension-tumor.xml", STRICT_NAMESPACE_MODE);
        assertValidXmlFileForLibrary("extension-tumor.xml", RELAXED_NAMESPACE_MODE);

    }

    @SuppressWarnings("ConstantConditions")
    private void assertValidXmlFileForXsd(String xmlFile) {
        try {
            URL schemaXsd = Thread.currentThread().getContextClassLoader().getResource("xsd/naaccr_data_1.1.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaXsd);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/xsdcomparison/" + xmlFile)));
        }
        catch (Exception e) {
            Assert.fail("Was expected a valid file, but it was invalid: " + e.getMessage());
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void assertNotValidXmlFileForXsd(String xmlFile) {
        try {
            URL schemaXsd = Thread.currentThread().getContextClassLoader().getResource("xsd/naaccr_data_1.1.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaXsd);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/xsdcomparison/" + xmlFile)));
        }
        catch (Exception e) {
            return;
        }
        Assert.fail("Was expected an invalid file, but it was valid");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertValidXmlFileForLibrary(String xmlFile, boolean useStrictNamespace) {
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/data/xsdcomparison/" + xmlFile);

        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(useStrictNamespace);

        NaaccrStreamConfiguration configuration = new NaaccrStreamConfiguration();
        configuration.setAllowedTagsForNamespacePrefix("other", "MyOuterTag", "MyInnerTag");
        configuration.setAllowedTagsForNamespacePrefix("naaccr", "NaaccrData", "Patient", "Tumor", "Item");

        NaaccrDictionary userDictionary = new NaaccrDictionary();
        userDictionary.setDictionaryUri("whatever");
        userDictionary.setNaaccrVersion("140");

        try {
            try (PatientXmlReader reader = new PatientXmlReader(NaaccrXmlUtils.createReader(file), options, userDictionary, configuration)) {
                reader.getRootData();
                Patient patient = reader.readPatient();
                while (patient != null)
                    patient = reader.readPatient();
            }
        }
        catch (NaaccrIOException e) {
            Assert.fail("Was expected a valid file, but it was invalid: " + e.getMessage());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertNotValidXmlFileForLibrary(String xmlFile, boolean useStrictNamespace) {
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/data/xsdcomparison/" + xmlFile);

        NaaccrOptions options = new NaaccrOptions();
        options.setUseStrictNamespaces(useStrictNamespace);

        NaaccrStreamConfiguration configuration = new NaaccrStreamConfiguration();
        configuration.setAllowedTagsForNamespacePrefix("other", "MyOuterTag", "MyInnerTag");
        configuration.setAllowedTagsForNamespacePrefix("naaccr", "NaaccrData", "Patient", "Tumor", "Item");

        NaaccrDictionary userDictionary = new NaaccrDictionary();
        userDictionary.setDictionaryUri("whatever");
        userDictionary.setNaaccrVersion("140");

        try {
            try (PatientXmlReader reader = new PatientXmlReader(NaaccrXmlUtils.createReader(file), options, userDictionary, configuration)) {
                reader.getRootData();
                Patient patient = reader.readPatient();
                while (patient != null)
                    patient = reader.readPatient();
            }
        }
        catch (NaaccrIOException e) {
            return;
        }
        Assert.fail("Was expected an invalid file, but it was valid");
    }

}
