/*
 * Copyright (C) 2022 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import com.imsweb.naaccrxml.TestingUtils;

/**
 * FD - I created this class after a NAACCR XML meeting where it was mentioned the NAACCR website doesn't back up the user-defined dictionaries that
 *  registry post. This class needs to be run manually, it downloads the dictionary from the NAACCR website and create a ZIP backup that it saves
 *  in the "docs/user-defined-dictionaries" folder.
 */
public class UserDefinedDictionariesBackup {

    public static void main(String[] args) throws Exception {

        File dir = new File(TestingUtils.getWorkingDirectory() + "/docs/user-defined-dictionaries");
        if (!dir.exists())
            throw new IllegalStateException("Unable to find config directory");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US);
        File outputFile = new File(dir, "user-defined-dictionaries-backup-" + dateTimeFormatter.format(LocalDate.now()) + ".zip");

        URL url = new URI("https://www.naaccr.org/xml-user-dictionary/#1595253894728-6781cbdf-8c50").toURL();

        String fullContent;
        try (InputStream is = url.openStream()) {
            fullContent = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();
        }

        //System.out.println(fullContent);

        //<td class="column-1">Alaska</td><td class="column-2"><a href="https://www.naaccr.org/wp-content/uploads/ninja-forms/21/alaska-naaccr-dictionary-220-v1.xml" rel="noopener noreferrer" target="_blank"> XML User Dictionary</a><br />
        Pattern pattern = Pattern.compile("<td class=\"column-1\">(.+?)</td><td class=\"column-2\"><a href=\"(.+?)\".+XML User Dictionary</a>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

        Matcher mather = pattern.matcher(fullContent);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputFile.toPath()))) {
            while (mather.find()) {
                String registry = mather.group(1);
                String dictionaryUrl = mather.group(2);
                String filename = dictionaryUrl.substring(dictionaryUrl.lastIndexOf("/") + 1);

                String dictionaryContent;
                try (InputStream is = new URI(dictionaryUrl).toURL().openStream()) {
                    dictionaryContent = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();
                }

                ZipEntry entry = new ZipEntry(outputFile.getName().replace(".zip", "") + "/" + registry + "_" + filename);
                entry.setComment(registry);
                zos.putNextEntry(entry);
                try (Reader reader = new StringReader(dictionaryContent)) {
                    IOUtils.copy(reader, zos, StandardCharsets.UTF_8);
                    zos.flush();
                }
            }
        }
    }
}
