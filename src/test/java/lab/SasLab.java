/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class SasLab {

    public static void main(String[] args) throws IOException {
        NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.getMergedDictionaries("180");

        //System.out.println(createSasFlatMappings(dictionary, false));

        //System.out.println(createSasXmlMapper(dictionary));

        System.out.println(createSasReadMacroAttributes(dictionary, false));

        //        long start = System.currentTimeMillis();
        //        try (PatientXmlReader r = new PatientXmlReader(NaaccrXmlUtils.createReader(new File("C:\\Users\\depryf\\Desktop\\sas\\synthetic-data_naaccr-18-incidence_10000000-recs.xml.gz")))) {
        //            try (PatientFlatWriter w = new PatientFlatWriter(NaaccrXmlUtils.createWriter(new File("C:\\Users\\depryf\\Desktop\\sas\\synthetic-data_naaccr-18-incidence_10000000-recs-copy.txt.gz")),
        //                    r.getRootData())) {
        //                Patient pat = r.readPatient();
        //                while (pat != null) {
        //                    w.writePatient(pat);
        //                    pat = r.readPatient();
        //                }
        //            }
        //        }
        //        System.out.println(System.currentTimeMillis() - start);

        //        long start = System.currentTimeMillis();
        //        try (PatientXmlReader r = new PatientXmlReader(NaaccrXmlUtils.createReader(new File("C:\\Users\\depryf\\Desktop\\sas\\synthetic-data_naaccr-18-incidence_10000000-recs.xml.gz")))) {
        //            Patient pat = r.readPatient();
        //            while (pat != null)
        //                pat = r.readPatient();
        //        }
        //        System.out.println(System.currentTimeMillis() - start);

        //        long start = System.currentTimeMillis();
        //        try (BufferedReader r = new BufferedReader(NaaccrXmlUtils.createReader(new File("C:\\Users\\depryf\\Desktop\\sas\\synthetic-data_naaccr-18-incidence_10000000-recs.xml.gz")))) {
        //            String line = r.readLine();
        //            while (line != null)
        //                line = r.readLine();
        //        }
        //        System.out.println(System.currentTimeMillis() - start);

        //        long start = System.currentTimeMillis();
        //        try (BufferedReader r = new BufferedReader(NaaccrXmlUtils.createReader(new File("C:\\Users\\depryf\\Desktop\\sas\\synthetic-data_naaccr-18-incidence_10000000-recs.xml.gz")))) {
        //            char[] bytes = new char[8192]; // default buffer size
        //            int n = r.read(bytes);
        //            while (n > 0)
        //                n = r.read(bytes);
        //        }
        //        System.out.println(System.currentTimeMillis() - start);

//        long start = System.currentTimeMillis();
//        try (BufferedReader r = new BufferedReader(NaaccrXmlUtils.createReader(new File("C:\\Users\\depryf\\Desktop\\sas\\data\\synthetic-data_naaccr-18-incidence_10000000-recs.txt.gz")))) {
//            String line = r.readLine();
//            while (line != null)
//                line = r.readLine();
//        }
//        System.out.println(System.currentTimeMillis() - start);
    }

    private static String createSasFlatMappings(NaaccrDictionary dictionary, boolean forInput) {
        List<NaaccrDictionaryItem> items = new ArrayList<>(dictionary.getItems());
        items.sort(Comparator.comparing(NaaccrDictionaryItem::getStartColumn));

        Map<String, AtomicInteger> counters = new HashMap<>();

        StringBuilder buf = new StringBuilder();

        int count = 0;
        for (NaaccrDictionaryItem item : items) {
            String id = item.getNaaccrId();
            if (id.length() > 32) {
                String prefix = id.substring(0, 30);
                int counter = counters.computeIfAbsent(prefix, k -> new AtomicInteger()).getAndIncrement();
                id = prefix + "_" + counter;
            }

            if (item.getRecordTypes().contains("I")) {
                if (forInput) {
                    buf.append("          @");
                    buf.append(item.getStartColumn());
                    buf.append(" ");
                    buf.append(id);
                    buf.append(" $char");
                    buf.append(item.getLength());
                    buf.append(".\n");
                }
                else {
                    buf.append("        @");
                    buf.append(item.getStartColumn());
                    buf.append(" ");
                    buf.append(id);
                    buf.append(" $char");
                    buf.append(item.getLength());
                    buf.append(".\n");
                }
                count++;
            }
        }
        System.out.println(count + " items...");

        return buf.toString();
    }

    private static String createSasReadMacroAttributes(NaaccrDictionary dictionary, boolean calls) {
        List<NaaccrDictionaryItem> items = new ArrayList<>(dictionary.getItems());
        items.sort(Comparator.comparing(NaaccrDictionaryItem::getStartColumn));

        Map<String, AtomicInteger> counters = new HashMap<>();

        StringBuilder buf = new StringBuilder();

        int count = 0;
        for (NaaccrDictionaryItem item : items) {
            String id = item.getNaaccrId();
            if (id.length() > 32) {
                String prefix = id.substring(0, 30);
                int counter = counters.computeIfAbsent(prefix, k -> new AtomicInteger()).getAndIncrement();
                id = prefix + "_" + counter;
            }

            if (item.getRecordTypes().contains("I")) {
                if (calls) {
                    buf.append("        j1.callStringMethod('getValue', '");
                    buf.append(id);
                    buf.append("', ");
                    buf.append(id);
                    buf.append(");\n");
                }
                else {
                    buf.append("        ");
                    buf.append(id);
                    buf.append(" length = $");
                    buf.append(item.getLength());
                    buf.append("\n");
                }
                count++;
            }
        }
        System.out.println(count + " items...");

        return buf.toString();
    }

    private static String createSasXmlMapper(NaaccrDictionary dictionary) {
        StringBuilder buf = new StringBuilder();

        String version;
        try (FileInputStream is = new FileInputStream(System.getProperty("user.dir") + File.separator + "VERSION")) {
            version = IOUtils.readLines(is, StandardCharsets.US_ASCII).get(0);
        }
        catch (IOException e) {
            version = "?";
        }

        buf.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>\r\n");
        buf.append("<!-- ############################################################ -->\r\n");
        buf.append("<!-- SAS XML Libname Engine Map -->\r\n");
        buf.append("<!-- Generated by NAACCR XML Java library v").append(version).append(" -->\r\n");
        buf.append("<!-- ############################################################ -->\r\n");
        buf.append("<SXLEMAP description=\"NAACCR XML v16 mapping\" name=\"naaccr_xml_v16_map\" version=\"2.1\">\r\n");
        buf.append("\r\n");
        buf.append("    <NAMESPACES count=\"0\"/>\r\n");
        addLevelInfo(dictionary, NaaccrXmlUtils.NAACCR_XML_TAG_ROOT, buf, "NAACCR Data data set", "naaccrdata", "/NaaccrData");
        addLevelInfo(dictionary, NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT, buf, "Patients data set", "patients", "/NaaccrData/Patient");
        addLevelInfo(dictionary, NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR, buf, "Tumors data set", "tumors", "/NaaccrData/Patient/Tumor");
        buf.append("</SXLEMAP>\r\n");

        return buf.toString();
    }

    private static void addLevelInfo(NaaccrDictionary dictionary, String level, StringBuilder buf, String desc, String name, String path) {

        buf.append("\r\n");
        buf.append("    <!-- ############################################################ -->\r\n");
        buf.append("    <TABLE description=\"").append(desc).append("\" name=\"").append(name).append("\">\r\n");
        buf.append("        <TABLE-PATH syntax=\"XPath\">").append(path).append("</TABLE-PATH>\r\n");
        buf.append("\r\n");
        buf.append("        <COLUMN class=\"ORDINAL\" name=\"NaaccrDataKey\" retain=\"YES\">\r\n");
        buf.append("            <INCREMENT-PATH beginend=\"BEGIN\" syntax=\"XPath\">/NaaccrData</INCREMENT-PATH>\r\n");
        buf.append("            <TYPE>numeric</TYPE>\r\n");
        buf.append("            <DATATYPE>integer</DATATYPE>\r\n");
        buf.append("        </COLUMN>\r\n");
        if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(level) || NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(level)) {
            buf.append("\r\n");
            buf.append("        <COLUMN class=\"ORDINAL\" name=\"PatientKey\" retain=\"YES\">\r\n");
            buf.append("            <INCREMENT-PATH beginend=\"BEGIN\" syntax=\"XPath\">/NaaccrData/Patient</INCREMENT-PATH>\r\n");
            buf.append("            <TYPE>numeric</TYPE>\r\n");
            buf.append("            <DATATYPE>integer</DATATYPE>\r\n");
            buf.append("        </COLUMN>\r\n");
        }
        if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(level)) {
            buf.append("\r\n");
            buf.append("        <COLUMN class=\"ORDINAL\" name=\"TumorKey\" retain=\"YES\">\r\n");
            buf.append("            <INCREMENT-PATH beginend=\"BEGIN\" syntax=\"XPath\">/NaaccrData/Patient/Tumor</INCREMENT-PATH>\r\n");
            buf.append("            <TYPE>numeric</TYPE>\r\n");
            buf.append("            <DATATYPE>integer</DATATYPE>\r\n");
            buf.append("        </COLUMN>\r\n");
        }

        for (NaaccrDictionaryItem item : dictionary.getItems()) {
            if (level.equals(item.getParentXmlElement())) {
                buf.append("\r\n");
                buf.append("        <COLUMN name=\"").append(item.getNaaccrId()).append("\">\r\n");
                buf.append("            <PATH syntax=\"XPath\">").append(createXpath(item)).append("</PATH>\r\n");
                buf.append("            <DESCRIPTION>")
                        .append(item.getNaaccrName()
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("&", "&amp;"))
                        .append(" [Item #").append(item.getNaaccrNum()).append("]</DESCRIPTION>\r\n");
                buf.append("            <TYPE>character</TYPE>\r\n");
                buf.append("            <DATATYPE>string</DATATYPE>\r\n");
                buf.append("            <LENGTH>").append(item.getLength()).append("</LENGTH>\r\n");
                buf.append("        </COLUMN>\r\n");
            }
        }

        buf.append("    </TABLE>\r\n");
    }

    private static String createXpath(NaaccrDictionaryItem item) {
        switch (item.getParentXmlElement()) {
            case NaaccrXmlUtils.NAACCR_XML_TAG_ROOT:
                return "/NaaccrData/Item[@naaccrId=\"" + item.getNaaccrId() + "\"]";
            case NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT:
                return "/NaaccrData/Patient/Item[@naaccrId=\"" + item.getNaaccrId() + "\"]";
            case NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR:
                return "/NaaccrData/Patient/Tumor/Item[@naaccrId=\"" + item.getNaaccrId() + "\"]";
            default:
                throw new IllegalStateException("Unsupported parent XML element: " + item.getParentXmlElement());
        }
    }

}
