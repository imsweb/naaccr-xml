package com.imsweb.naaccrxml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestingUtils {

    /**
     * Returns the testing data file with the requested name.
     * @param name file name
     * @return corresponding data file
     */
    public static File getDataFile(String name) {
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/data/" + name);
        if (!file.exists())
            throw new RuntimeException("Unable to find testing data file '" + name + "'");
        return file;
    }

    /**
     * Returns an empty record as a string buffer, for the given NAACCR version and record type.
     * @param version NAACCR version
     * @param recType record type
     * @param patientIdNumber
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
     * @throws IOException
     */
    public static File createFile(String filename) throws IOException {

        // create the tmp folder
        File tmpDir = new File(System.getProperty("user.dir") + "/build/test-tmp");
        if (!tmpDir.exists() && !tmpDir.mkdirs())
            throw new IOException("Unable to create tmp dir...");

        // there is no need to physically create the file, it will be created when something is written to it...
        File file = new File(tmpDir, filename);
        file.deleteOnExit();
        return file;
    }

    /**
     * Creates a testing file with the given name and populates it with the given lines.
     * @param filename name of the file to crate
     * @param records records (as lines) to add to the file
     * @return the created file
     * @throws IOException
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
        StringBuilder buf = new StringBuilder();
        for (String line : readFile(file))
            buf.append(line).append("\n");
        return buf.toString();
    }
}
