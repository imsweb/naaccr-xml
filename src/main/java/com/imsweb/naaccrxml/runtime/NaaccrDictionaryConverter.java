/*
 * Copyright (C) 2021 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryGroupedItem;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class NaaccrDictionaryConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
        return NaaccrDictionary.class.isAssignableFrom(type);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        NaaccrDictionary dictionary = (NaaccrDictionary)source;

        if (dictionary.getDictionaryUri() != null)
            writer.addAttribute("dictionaryUri", dictionary.getDictionaryUri());
        if (dictionary.getNaaccrVersion() != null)
            writer.addAttribute("naaccrVersion", dictionary.getNaaccrVersion());
        if (dictionary.getSpecificationVersion() != null)
            writer.addAttribute("specificationVersion", dictionary.getSpecificationVersion());
        if (dictionary.getDateLastModified() != null)
            writer.addAttribute("dateLastModified", NaaccrXmlUtils.formatIso8601Date(dictionary.getDateLastModified()));
        if (!StringUtils.isBlank(dictionary.getDescription()))
            writer.addAttribute("description", dictionary.getDescription());

        writer.addAttribute("xmlns", NaaccrXmlUtils.NAACCR_XML_NAMESPACE);

        writer.startNode("ItemDefs");

        for (NaaccrDictionaryItem item : dictionary.getItems()) {
            writer.startNode("ItemDef");
            writeItem(writer, item);
            writer.endNode();
        }

        writer.endNode();

        if (!dictionary.getGroupedItems().isEmpty()) {
            writer.startNode("GroupedItemDefs");

            for (NaaccrDictionaryGroupedItem groupedItem : dictionary.getGroupedItems()) {
                writer.startNode("GroupedItemDef");
                writeItem(writer, groupedItem);
                writer.endNode();
            }

            writer.endNode();
        }
    }

    private void writeItem(HierarchicalStreamWriter writer, NaaccrDictionaryItem item) {

        if (item.getNaaccrId() != null)
            writer.addAttribute("naaccrId", item.getNaaccrId());
        if (item.getNaaccrNum() != null)
            writer.addAttribute("naaccrNum", item.getNaaccrNum().toString());
        if (item.getNaaccrName() != null)
            writer.addAttribute("naaccrName", item.getNaaccrName());
        if (item.getStartColumn() != null)
            writer.addAttribute("startColumn", item.getStartColumn().toString());
        if (item.getLength() != null)
            writer.addAttribute("length", item.getLength().toString());
        if (item.getAllowUnlimitedText() != null)
            writer.addAttribute("allowUnlimitedText", item.getAllowUnlimitedText().toString().toLowerCase());
        if (item.getRecordTypes() != null)
            writer.addAttribute("recordTypes", item.getRecordTypes());
        if (item.getSourceOfStandard() != null)
            writer.addAttribute("sourceOfStandard", item.getSourceOfStandard());
        if (item.getParentXmlElement() != null)
            writer.addAttribute("parentXmlElement", item.getParentXmlElement());
        if (item.getDataType() != null)
            writer.addAttribute("dataType", item.getDataType());
        if (item.getRegexValidation() != null)
            writer.addAttribute("regexValidation", item.getRegexValidation());
        if (item.getPadding() != null)
            writer.addAttribute("padding", item.getPadding());
        if (item.getTrim() != null)
            writer.addAttribute("trim", item.getTrim());
        if (item instanceof NaaccrDictionaryGroupedItem)
            if (((NaaccrDictionaryGroupedItem)item).getContains() != null)
                writer.addAttribute("contains", ((NaaccrDictionaryGroupedItem)item).getContains());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        if (!"NaaccrDictionary".equals(reader.getNodeName()))
            throw new RuntimeException("Expected 'NaaccrDictionary' element, got " + reader.getNodeName());

        NaaccrDictionary dictionary = new NaaccrDictionary();

        Iterator iter = reader.getAttributeNames();
        boolean sawDefaultNameSpace = false;
        while (iter.hasNext()) {
            String name = (String)iter.next();
            String value = reader.getAttribute(name);
            switch (name) {
                case "dictionaryUri":
                    dictionary.setDictionaryUri(stringToString(value));
                    break;
                case "naaccrVersion":
                    dictionary.setNaaccrVersion(stringToString(value));
                    break;
                case "specificationVersion":
                    dictionary.setSpecificationVersion(stringToString(value));
                    break;
                case "dateLastModified":
                    try {
                        dictionary.setDateLastModified(NaaccrXmlUtils.parseIso8601Date(reader.getAttribute("dateLastModified")));
                    }
                    catch (IOException e) {
                        throw new RuntimeException("Invalid ISO 8601 date: " + reader.getAttribute("dateLastModified"));
                    }
                    break;
                case "description":
                    dictionary.setDescription(stringToString(value));
                    break;
                case "xmlns":
                    if (!NaaccrXmlUtils.NAACCR_XML_NAMESPACE.equals(value))
                        throw new RuntimeException("Default namespace must be set to " + NaaccrXmlUtils.NAACCR_XML_NAMESPACE);
                    sawDefaultNameSpace = true;
                    break;
                default:
                    throw new RuntimeException("Invalid root attribute: " + name);
            }
        }
        if (!sawDefaultNameSpace)
            throw new RuntimeException("Default namespace must be provided and set to " + NaaccrXmlUtils.NAACCR_XML_NAMESPACE);

        reader.moveDown();

        if (!"ItemDefs".equals(reader.getNodeName()))
            throw new RuntimeException("Expected 'ItemDefs' element, got " + reader.getNodeName());

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            if (!"ItemDef".equals(reader.getNodeName()))
                throw new RuntimeException("Expected 'ItemDef' element, got " + reader.getNodeName());

            NaaccrDictionaryItem item = new NaaccrDictionaryItem();
            readItem(reader, item);
            dictionary.addItem(item);

            reader.moveUp();
        }

        reader.moveUp();

        if (reader.hasMoreChildren()) {
            reader.moveDown();
            if ("GroupedItemDefs".equals(reader.getNodeName())) {
                while (reader.hasMoreChildren()) {
                    reader.moveDown();

                    if (!"GroupedItemDef".equals(reader.getNodeName()))
                        throw new RuntimeException("Expected 'GroupedItemDef' element, got " + reader.getNodeName());

                    NaaccrDictionaryGroupedItem item = new NaaccrDictionaryGroupedItem();
                    readItem(reader, item);
                    dictionary.addGroupedItem(item);

                    reader.moveUp();
                }
            }
            reader.moveUp();
        }

        if (!"NaaccrDictionary".equals(reader.getNodeName()))
            throw new RuntimeException("Expected 'NaaccrDictionary' end element, got " + reader.getNodeName());

        return dictionary;
    }

    @SuppressWarnings("rawtypes")
    protected void readItem(HierarchicalStreamReader reader, NaaccrDictionaryItem item) {
        Iterator iter = reader.getAttributeNames();
        while (iter.hasNext()) {
            String name = (String)iter.next();
            String value = reader.getAttribute(name);
            switch (name) {
                case "naaccrId":
                    item.setNaaccrId(stringToString(value));
                    break;
                case "naaccrNum":
                    item.setNaaccrNum(stringToInt(value, name));
                    break;
                case "naaccrName":
                    item.setNaaccrName(stringToString(value));
                    break;
                case "startColumn":
                    item.setStartColumn(stringToInt(value, name));
                    break;
                case "length":
                    item.setLength(stringToInt(value, name));
                    break;
                case "recordTypes":
                    item.setRecordTypes(stringToString(value));
                    break;
                case "sourceOfStandard":
                    item.setSourceOfStandard(stringToString(value));
                    break;
                case "parentXmlElement":
                    item.setParentXmlElement(stringToString(value));
                    break;
                case "dataType":
                    item.setDataType(stringToString(value));
                    break;
                case "regexValidation":
                    item.setRegexValidation(stringToString(value));
                    break;
                case "padding":
                    item.setPadding(stringToString(value));
                    break;
                case "trim":
                    item.setTrim(stringToString(value));
                    break;
                case "allowUnlimitedText":
                    item.setAllowUnlimitedText(stringToBool(value, name));
                    break;
                case "contains":
                    if (item instanceof NaaccrDictionaryGroupedItem)
                        ((NaaccrDictionaryGroupedItem)item).setContains(stringToString(value));
                    else
                        throw new RuntimeException("Invalid attribute for 'ItemDef': " + name);
                    break;
                default:
                    throw new RuntimeException("Invalid attribute for 'ItemDef': " + name);
            }
        }
    }

    private String stringToString(String value) {
        return StringUtils.trimToNull(value);
    }

    private Integer stringToInt(String value, String attribute) {
        if (NumberUtils.isDigits(value))
            return Integer.valueOf(value);

        if (!StringUtils.isBlank(value))
            throw new RuntimeException("Invalid value for '" + attribute + "': " + value);

        return null;
    }

    private Boolean stringToBool(String value, String attribute) {
        if ("true".equals(value) || "false".equals(value))
            return Boolean.valueOf(value);

        if (!StringUtils.isBlank(value))
            throw new RuntimeException("Invalid value for '" + attribute + "': " + value);

        return null;
    }
}
