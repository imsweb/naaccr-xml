package com.imsweb.naaccrxml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestingUtils {

    /**
     * Returns an empty record as a string buffer, for the given NAACCR version and record type.
     * @param version NAACCR version
     * @param recType record type
     * @return corresponding empty record
     */
    public static StringBuilder createEmptyRecord(String version, String recType) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < NaaccrFormat.getInstance(version, recType).getLineLength(); i++)
            buf.append(" ");
        buf.replace(0, 1, recType);
        buf.replace(16, 19, version);
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
    public static File createAndPopulateFile(String filename, String... records) throws IOException {
        File file = createFile(filename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String rec : records) {
                writer.write(rec);
                writer.newLine();
            }
        }
        return file;
    }
}
