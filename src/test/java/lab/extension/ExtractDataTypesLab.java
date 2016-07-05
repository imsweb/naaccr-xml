/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package lab.extension;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractDataTypesLab {
    
    public static void main(String[] args) throws Exception {

        File file = new File(System.getProperty("user.dir") + "/build/data-types.csv");
        
        Pattern p1 = Pattern.compile("itemNo=\"(\\d+)\"");
        Pattern p2 = Pattern.compile("datatype=\"(.+?)\"");
        Pattern p3 = Pattern.compile("regex=\"(.+?)\"");
        
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("docs/roger/naaccr7dict.xml");
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(is))) {
            try (FileWriter writer = new FileWriter(file)) {
                String currentItemNum = null;
                String line = reader.readLine();
                while (line != null) {
                    Matcher m1 = p1.matcher(line);
                    if (m1.find())
                        currentItemNum = m1.group(1);
                    else {
                        Matcher m2 = p2.matcher(line);
                        if (m2.find())
                            writer.write(currentItemNum + "," + (m2.group(1).equals("@D") ? "code" : m2.group(1)) + ",\n");
                        else {
                            Matcher m3 = p3.matcher(line);
                            if (m3.find())
                                writer.write(currentItemNum + ",string," + m3.group(1) + "\n");
                        }
                    }

                    line = reader.readLine();
                }
            }
        }
        
    } 
}
