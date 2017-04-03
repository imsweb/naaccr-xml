/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public class CreateSamplesZipFile {

    public static void main(String[] args) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(Paths.get("docs/naaccr-xml-samples-v160.zip").toFile()))) {
            Path dir = Paths.get("src/test/resources/data/validity");
            Files.newDirectoryStream(dir.resolve("valid")).forEach(path -> addToZip(path.toFile(), zos));
            Files.newDirectoryStream(dir.resolve("invalid")).forEach(path -> addToZip(path.toFile(), zos));
            Files.newDirectoryStream(dir.resolve("invalid_relaxed")).forEach(path -> addToZip(path.toFile(), zos));
            Files.newDirectoryStream(dir.resolve("invalid_library_only")).forEach(path -> addToZip(path.toFile(), zos));
        }
    }

    // helper
    private static void addToZip(File file, ZipOutputStream zos) {
        try {
            zos.putNextEntry(new ZipEntry(file.getName()));
            try (FileInputStream fis = new FileInputStream(file)) {
                IOUtils.copy(fis, zos);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
