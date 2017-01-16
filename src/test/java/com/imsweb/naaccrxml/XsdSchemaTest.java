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

/**
 * The purpose of this test is to make sure that the built-in validation behaves the same way as the 3WC XSD schema one.
 */
public class XsdSchemaTest {

    @Test
    public void testXsdAgainstLibrary() {

        // regular file
        assertValidXmlFileForLibrary("standard-file.xml");
        assertValidXmlFileForXsd("standard-file.xml");

        // regular file that doesn't define a default namespace; not allowed by XSD
        assertValidXmlFileForLibrary("standard-file-no-default-namespace.xml");
        assertNotValidXmlFileForXsd("standard-file-no-default-namespace.xml");

        // a regular file that defines the namespace but doesn't use prefixes, should be valid in both
        assertValidXmlFileForLibrary("namespace-without-prefix.xml");
        assertValidXmlFileForXsd("namespace-without-prefix.xml");

        // a regular file that defines the namespace and uses prefix; we don't support that in the library...
        assertNotValidXmlFileForLibrary("namespace-with-prefix.xml");
        assertValidXmlFileForXsd("namespace-with-prefix.xml");

        // this file has no items, that's OK
        assertValidXmlFileForLibrary("no-items.xml");
        assertValidXmlFileForXsd("no-items.xml");

        // this file has no patient, that's OK
        assertValidXmlFileForLibrary("no-patients.xml");
        assertValidXmlFileForXsd("no-patients.xml");

        // this file has no tumors, that's OK
        assertValidXmlFileForLibrary("no-tumors.xml");
        assertValidXmlFileForXsd("no-tumors.xml");

        // extensions - not valid with XSD because doesn't define the namespace (and extensions wouldn't be valid anyway)
        assertValidXmlFileForLibrary("extension-missing-namespace.xml");
        assertNotValidXmlFileForXsd("extension-missing-namespace.xml");

        // this file has a root extension that should be ignored
        assertValidXmlFileForLibrary("extension-root.xml");
        assertValidXmlFileForXsd("extension-root.xml");

        // this file has a patient extension that should be ignored
        assertValidXmlFileForLibrary("extension-patient.xml");
        assertValidXmlFileForXsd("extension-patient.xml");

        // this file has a tumor extension that should be ignored
        assertValidXmlFileForLibrary("extension-tumor.xml");
        assertValidXmlFileForXsd("extension-tumor.xml");

    }

    @SuppressWarnings("ConstantConditions")
    private void assertValidXmlFileForXsd(String xmlFile) {
        try {
            URL schemaXsd = Thread.currentThread().getContextClassLoader().getResource("xsd/naaccr_data_1.0.xsd");
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
            URL schemaXsd = Thread.currentThread().getContextClassLoader().getResource("xsd/naaccr_data_1.0.xsd");
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

    private void assertValidXmlFileForLibrary(String xmlFile) {
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/data/xsdcomparison/" + xmlFile);
        try {
            NaaccrXmlUtils.readXmlFile(file, null, null, null);
        }
        catch (NaaccrIOException e) {
            Assert.fail("Was expected a valid file, but it was invalid: " + e.getMessage());
        }
    }

    private void assertNotValidXmlFileForLibrary(String xmlFile) {
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/data/xsdcomparison/" + xmlFile);
        try {
            NaaccrXmlUtils.readXmlFile(file, null, null, null);
        }
        catch (NaaccrIOException e) {
            return;
        }
        Assert.fail("Was expected an invalid file, but it was valid");
    }

}
