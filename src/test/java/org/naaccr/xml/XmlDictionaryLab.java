/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.NaaccrDictionaryItem;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class XmlDictionaryLab {

    public static void main(String[] args) throws IOException {
        final NaaccrDictionary dictionary = NaaccrXmlUtils.getBaseDictionary("140");
        System.out.println("Read " + dictionary.getItems().size() + " items from standard CSV dictionary...");

        XStream xstream = new XStream();
        xstream.alias("NaaccrDictionary", NaaccrDictionary.class);
        xstream.alias("ItemDef", NaaccrDictionaryItem.class);
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "naaccrId", "naaccrId");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "naaccrNum", "naaccrNum");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "naaccrName", "naaccrName");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "startColumn", "startColumn");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "length", "length");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "recordTypes", "recordTypes");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "sourceOfStandard", "sourceOfStandard");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "parentXmlElement", "parentXmlElement");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "dataType", "dataType");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "regexValidation", "regexValidation");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "padding", "padding");
        xstream.aliasAttribute(NaaccrDictionaryItem.class, "trim", "trim");
        xstream.addImplicitCollection(NaaccrDictionary.class, "items", NaaccrDictionaryItem.class);

        File outputFile = new File(System.getProperty("user.dir") + "/build/naaccr-dictionary-140.xml");
        FileWriter writer = new FileWriter(outputFile);

        PrettyPrintWriter prettyWriter = new PrettyPrintWriter(writer) {
            // why isn't the internal writer protected instead of private??? I hate when people do that!
            private QuickWriter _internalWriter;

            private String _currentItemId;

            @Override
            protected void writeAttributeValue(QuickWriter writer, String text) {
                super.writeAttributeValue(writer, text);
                if (_internalWriter == null)
                    _internalWriter = writer;
            }

            @Override
            public void startNode(String name) {
                super.startNode(name);
                _currentItemId = null;
                if ("NaaccrDictionary".equals(name)) {
                    addAttribute("naaccrVersion", "140");
                    addAttribute("description", "NAACCR 14 data dictionary.");
                }
            }

            @Override
            public void addAttribute(String key, String value) {
                if ("padding".equals(key) && NaaccrXmlUtils.NAACCR_PADDING_RIGHT_BLANK.equals(value))
                    return;
                if ("trim".equals(key) && NaaccrXmlUtils.NAACCR_TRIM_ALL.equals(value))
                    return;
                super.addAttribute(key, value);
                if ("naaccrId".equals(key))
                    _currentItemId = value;
                if (!"naaccrVersion".equals(key) && !"description".equals(key) && !isLastAttribute(key))
                    _internalWriter.write("\r\n     ");
            }

            private boolean isLastAttribute(String attribute) {
                NaaccrDictionaryItem item = dictionary.getItemByNaaccrId(_currentItemId);
                if (item == null)
                    return false;

                if (item.getTrim() != null && !NaaccrXmlUtils.NAACCR_TRIM_ALL.equals(item.getTrim()))
                    return "trim".equals(attribute);
                if (item.getPadding() != null && !NaaccrXmlUtils.NAACCR_PADDING_RIGHT_BLANK.equals(item.getPadding()))
                    return "padding".equals(attribute);
                if (item.getRegexValidation() != null)
                    return "regexValidation".equals(attribute);
                if (item.getDataType() != null)
                    return "dataType".equals(attribute);
                if (item.getParentXmlElement() != null)
                    return "parentXmlElement".equals(attribute);
                return false;
            }
        };

        //xstream.toXML(dictionary, writer);
        //writer.close();

        try {
            xstream.marshal(dictionary, prettyWriter);
        }
        finally {
            writer.flush();
        }

        System.out.println("Wrote " + outputFile.getPath());
    }

}
