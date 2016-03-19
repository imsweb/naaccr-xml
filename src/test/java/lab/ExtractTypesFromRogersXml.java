/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package lab;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class ExtractTypesFromRogersXml {

    @SuppressWarnings("SuspiciousMethodCalls")
    public static void main(String[] args) throws IOException {

        Map<String, String> oldTypes = new HashMap<>();
        Pattern pattern1 = Pattern.compile("dataType=\"(.+?)\" startColumn=\"(.+?)\""); // parentXmlElement="NaaccrData" dataType="code" startColumn="2" length="1"
        Pattern pattern2 = Pattern.compile("startColumn=\"(.+?)\""); // parentXmlElement="NaaccrData" regexValidation="^[ICAM]$" startColumn="1" length="1"
        File file = new File(System.getProperty("user.dir") + "/docs/NaaccrDict14.xml");
        try (LineNumberReader reader = new LineNumberReader(new FileReader(file))) {
            String line = reader.readLine();
            while (line != null) {
                Matcher matcher1 = pattern1.matcher(line);
                if (matcher1.find())
                    oldTypes.put(matcher1.group(2), matcher1.group(1));
                else {
                    Matcher matcher2 = pattern2.matcher(line);
                    if (matcher2.find())
                        oldTypes.put(matcher2.group(1), "<blank>");
                }
                line = reader.readLine();
            }
        }

        File csv = new File(System.getProperty("user.dir") + "/build/types-comparison.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csv))) {
            writer.write("Item Num,Item ID,Old Type,New Type\r\n");
            for (NaaccrDictionaryItem item : NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion("140").getItems()) {
                String oldType = oldTypes.get(item.getStartColumn().toString());
                if (oldType != null)
                    writer.write(item.getNaaccrNum() + "," + item.getNaaccrId() + "," + oldType + "," + (item.getDataType() == null ? "<blank>" : item.getDataType()) + "\r\n");
                else
                    System.err.println("Can't get old type for " + item.getNaaccrId());
            }
        }

    }
}
