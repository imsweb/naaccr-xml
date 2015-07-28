/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.lab;

import java.io.File;
import java.io.FileInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.xml.internal.messaging.saaj.packaging.mime.util.LineInputStream;

public class BatchProcessorReportParser {

    public static void main(String[] args) throws Exception {
        File reportFile = new File("C:\\dev\\report.txt");

        //Pattern p1 = Pattern.compile("created size: (\\d+(\\.\\d)?) (MB|KB)");
        //Pattern p1 = Pattern.compile("processing time: (((\\d+) minute(s)?, )?((\\d+) second(s)?)|< 1 second)");
        Pattern p1 = Pattern.compile("((Patient value not consistent among tumors: ((\\d|,)+) cases)|no warning found)");
        Pattern p2 = Pattern.compile("(involved item\\(s\\): \\d+ \\[(.+)\\])");

        try (LineInputStream is = new LineInputStream(new FileInputStream(reportFile))) {
            String line = is.readLine();
            while (line != null) {
                int idx = 0;
                Matcher m1 = p1.matcher(line);
                while (m1.find(idx)) {

                    /**
                     // num rec
                     System.out.println(m1.group(1));
                     idx = m1.end(1);
                     */

                    /**
                     // size
                     Float f = Float.valueOf(m1.group(1));
                     if ("KB".equals(m1.group(3)))
                     f = f / 1000;
                     System.out.printf("%.1f\n", f);
                     idx = m1.end(3);
                     */

                    /**
                     // time
                     if (m1.group(1).contains("<"))
                     System.out.println(0);
                     else if (m1.group(1).contains("minute"))
                     System.out.println(Integer.valueOf(m1.group(3)) * 60 + Integer.valueOf(m1.group(6)));
                     else
                     System.out.println(m1.group(6));
                     idx = m1.end(1);
                     */

                    if (!m1.group(1).contains("no warning")) {
                        System.out.println(m1.group(3).replace(",", ""));
                    }
                    else
                        System.out.println("0");
                    idx = m1.end(1);
                }
                line = is.readLine();
            }
        }
    }

}
