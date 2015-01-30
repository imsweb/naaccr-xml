/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class XmlDictionaryLab {

    public static void main(String[] args) throws IOException {
        NaaccrDictionary dictionary = NaaccrXmlUtils.getStandardDictionary();
        System.out.println("Read " + dictionary.getItems().size() + " items from standard CSV dictionary...");

        XStream xstream = new XStream();
        xstream.alias("NaaccrDictionary", NaaccrDictionary.class);
        xstream.alias("ItemDef", NaaccrDictionaryItem.class);
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "naaccrId", "naaccrId");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "number", "number");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "naaccrName", "naaccrName");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "startColumn", "startColumn");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "length", "length");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "section", "section");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "recordTypes", "recordTypes");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "sourceOfStandard", "sourceOfStandard");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "parentXmlElement", "parentXmlElement");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "regexValidation", "regexValidation");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "dataType", "dataType");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "groupNaaccrId", "groupNaaccrId");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "isGroup", "isGroup");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "retiredVersion", "retiredVersion");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "implementedVersion", "implementedVersion");
        xstream.addImplicitCollection(NaaccrDictionary.class, "items", NaaccrDictionaryItem.class);

        File outputFile = new File(System.getProperty("user.dir") + "/build/naaccr-dictionary-140.xml");
        FileWriter writer = new FileWriter(outputFile);


        PrettyPrintWriter prettyWriter = new PrettyPrintWriter(writer) {
            // why isn't the internal writer protected instead of private??? I hate when people do that!
            private QuickWriter _internalWriter;

            @Override
            protected void writeAttributeValue(QuickWriter writer, String text) {
                super.writeAttributeValue(writer, text);
                if (_internalWriter == null)
                    _internalWriter = writer;
            }
            
            @Override
            public void startNode(String name) {
                super.startNode(name);
                if ("NaaccrDictionary".equals(name)) {
                    addAttribute("naaccrVersion", "140");
                    addAttribute("description", "NAACCR 14 data dictionary.");
                    addAttribute("releaseDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                }
            }

            @Override
            public void addAttribute(String key, String value) {
                super.addAttribute(key, value);
                if (!"naaccrVersion".equals(key) && !"description".equals(key) && !"releaseDate".equals(key))
                    _internalWriter.write("\r\n     "); // this is hack, will need to find a better way!
            }
        };
        
        //xstream.toXML(dictionary, writer);
        //writer.close();
        
        try {
            xstream.marshal(dictionary, prettyWriter);
        } finally {
            writer.flush();
        }
        
        System.out.println("Wrote " + outputFile.getPath());
    }

}
