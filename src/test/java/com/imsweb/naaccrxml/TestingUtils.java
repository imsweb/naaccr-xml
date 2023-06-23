package com.imsweb.naaccrxml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class TestingUtils {

    /**
     * Returns the project root folder.
     */
    public static String getWorkingDirectory() {
        return System.getProperty("user.dir").replace(".idea\\modules", ""); // this will make it work in IntelliJ and outside of it...
    }

    /**
     * Returns the project root folder.
     */
    public static File getBuildDirectory() {
        File file = new File(getWorkingDirectory() + "/build");
        if (!file.exists() && !file.mkdir())
            throw new RuntimeException("Unable to create build folder");
        return file;
    }

    /**
     * Create a user-defined dictionary that can be used in various unit tests.
     */
    public static NaaccrDictionary createUserDictionary() {
        return createUserDictionary(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);
    }

    /**
     * Create a user-defined dictionary that can be used in various unit tests.
     */
    public static NaaccrDictionary createUserDictionary(String specifications) {
        NaaccrDictionary dict = new NaaccrDictionary();
        dict.setSpecificationVersion(specifications);
        dict.setNaaccrVersion("160");
        dict.setDictionaryUri("http://test.org/naaccrxml/test.xml");
        dict.setDescription("Another whatever...");

        NaaccrDictionaryItem item = new NaaccrDictionaryItem();
        item.setNaaccrId("myVariable");
        item.setParentXmlElement(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        item.setNaaccrNum(10000);
        item.setRecordTypes("A,M,C,I");
        item.setDataType(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_NUMERIC);
        item.setLength(2);
        item.setStartColumn(2340);
        item.setNaaccrName("My Variable");
        item.setPadding(NaaccrXmlDictionaryUtils.NAACCR_PADDING_NONE);
        if (SpecificationVersion.compareSpecifications(specifications, SpecificationVersion.SPEC_1_2) < 0)
            item.setRegexValidation("0[0-8]");
        dict.addItem(item);

        return dict;
    }

    /**
     * Returns the testing data file with the requested name.
     * @param name file name
     * @return corresponding data file
     */
    public static File getDataFile(String name) {
        File file = new File(getWorkingDirectory() + "/src/test/resources/data/" + name);
        if (!file.exists())
            throw new RuntimeException("Unable to find testing data file " + file.getPath());
        return file;
    }

    /**
     * Returns an empty record as a string buffer, for the given NAACCR version and record type.
     * @param version NAACCR version
     * @param recType record type
     * @param patientIdNumber patient ID number
     * @return corresponding empty record
     */
    public static StringBuilder createEmptyRecord(String version, String recType, String patientIdNumber) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < NaaccrFormat.getInstance(version, recType).getLineLength(); i++)
            buf.append(" ");
        buf.replace(0, 1, recType);
        buf.replace(16, 19, version);
        if (patientIdNumber != null)
            buf.replace(41, 49, patientIdNumber);
        return buf;
    }

    /**
     * Creates a file in a "test-tmp" folder in the build folder.
     * @param filename name of the file to create
     * @return created file
     */
    public static File createFile(String filename) throws IOException {
        return createFile(filename, true);
    }

    /**
     * Creates a file in a "test-tmp" folder in the build folder.
     * @param filename name of the file to create
     * @return created file
     */
    public static File createFile(String filename, boolean autoDelete) throws IOException {

        // create the tmp folder
        File tmpDir = new File(getBuildDirectory(), "test-tmp");
        if (!tmpDir.exists() && !tmpDir.mkdirs())
            throw new IOException("Unable to create tmp dir...");

        // there is no need to physically create the file, it will be created when something is written to it...
        File file = new File(tmpDir, filename);
        if (autoDelete)
            file.deleteOnExit();
        return file;
    }

    /**
     * Creates a testing file with the given name and populates it with the given lines.
     * @param filename name of the file to crate
     * @param records records (as lines) to add to the file
     * @return the created file
     */
    public static File createAndPopulateFile(String filename, StringBuilder... records) throws IOException {
        File file = createFile(filename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (StringBuilder rec : records) {
                writer.write(rec.toString());
                writer.newLine();
            }
        }
        return file;
    }

    /**
     * Reads the lines from the given file.
     */
    public static List<String> readFile(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        }
        return lines;
    }

    /**
     * Reads the content of the given file as one big string.
     */
    public static String readFileAsOneString(File file) throws IOException {
        try (FileReader reader = new FileReader(file); StringWriter writer = new StringWriter()) {
            IOUtils.copy(reader, writer);
            return writer.toString();
        }
    }

    /**
     * Writes the given content to the given file.
     */
    public static void writeFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    /**
     * Copies the given file to the given directory.
     */
    public static File copyFile(File source, File targetDir) throws IOException {
        File target = new File(targetDir, source.getName());
        FileUtils.copyFile(source, target);
        return target;
    }
}
