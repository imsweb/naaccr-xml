/*
 * Copyright (C) 2016 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import com.imsweb.datagenerator.naaccr.NaaccrDataGenerator;
import com.imsweb.datagenerator.naaccr.NaaccrDataGeneratorOptions;
import com.imsweb.layout.LayoutFactory;
import com.imsweb.layout.record.fixed.naaccr.NaaccrLayout;

public class SyntheticDataLab {

    private static final Path _TARGET_DIR = Paths.get(System.getProperty("user.dir"), "build");

    private static final String _NAACCR_VERSION = LayoutFactory.LAYOUT_ID_NAACCR_16_ABSTRACT;

    private static final List<Integer> _NUM_RECORDS = Arrays.asList(2, 5, 25, 250, 2500, 25000);

    public static void main(String[] args) throws IOException {
        if (!Files.exists(_TARGET_DIR))
            Files.createDirectory(_TARGET_DIR);

        // set the options
        NaaccrDataGeneratorOptions options = new NaaccrDataGeneratorOptions();
        options.setConstantValuesPostProcessing(Collections.singletonMap("registryId", "0000000000"));
        options.setState("MD");

        // create the generator
        NaaccrDataGenerator generator = new NaaccrDataGenerator((NaaccrLayout)LayoutFactory.getLayout(_NAACCR_VERSION));

        // create the files
        try (ZipOutputStream os = new ZipOutputStream(new FileOutputStream(new File(_TARGET_DIR.toFile(), _NAACCR_VERSION + ".zip")))) {
            for (Integer numRecords : _NUM_RECORDS) {
                File file = new File(_TARGET_DIR.toFile(), _NAACCR_VERSION + "_" + numRecords + ".txt");
                generator.generateFile(file, numRecords, options);
                try (FileInputStream is = new FileInputStream(file)) {
                    os.putNextEntry(new ZipEntry(file.getName()));
                    IOUtils.copy(is, os);
                }
                if (!file.delete())
                    System.err.println("Unable to delete " + file.getPath());
                else
                    System.out.println("Created " + file.getPath());
            }
        }

    }

}
