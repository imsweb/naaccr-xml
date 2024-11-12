/*
 * Copyright (C) 2020 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.util.Objects;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class EvaluateNaaccr21Changes {

    public static void main(String[] args) throws Exception {
        File file = new File("...\\Selected fields 20200715.csv");

        NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.getMergedDictionaries("210");

        try (CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(file.toPath())) {
            reader.stream().forEach(line -> {
                String id = line.getField(3);
                String num = line.getField(0);

                // error in the spreadsheet...
                if ("phase1RadiationToDrainingLymphNodes".equals(id))
                    id = "phase1RadiationToDrainingLN";

                NaaccrDictionaryItem item = dictionary.getItemByNaaccrId(id);
                if (item == null)
                    System.out.println("!!! " + id);
                else if (!Objects.equals(line.getField(0), item.getNaaccrNum().toString()))
                    System.out.println("bad num for " + num + ": " + line.getField(0));
                else if (!Objects.equals(line.getField(1), item.getLength().toString()))
                    System.out.println("bad length for " + num + ": " + line.getField(1));
                else if (!Objects.equals(line.getField(2), item.getNaaccrName()))
                    System.out.println("bad name for " + num + ": " + line.getField((2)));
            });
        }
    }
}
