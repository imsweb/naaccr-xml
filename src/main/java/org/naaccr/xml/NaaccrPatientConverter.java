/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.AbstractEntity;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionaryItem;
import org.xmlpull.v1.XmlPullParser;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class NaaccrPatientConverter implements Converter {

    private RuntimeNaaccrDictionary _dictionary;

    private NaaccrXmlOptions _options;

    private XmlPullParser _parser;

    public NaaccrPatientConverter(RuntimeNaaccrDictionary dictionary, NaaccrXmlOptions options) {
        _dictionary = dictionary;
        _options = options;
    }

    public void setParser(XmlPullParser parser) {
        _parser = parser;
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(Patient.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {

        // TODO FPD add validation

        Item item = (Item)source;
        if (item.getNum() != null)
            writer.addAttribute("naaccrNum", item.getNum().toString());
        if (item.getId() != null)
            writer.addAttribute("naaccrId", item.getId());
        if (item.getValue() != null)
            writer.setValue(item.getValue());
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

        if (!NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(reader.getNodeName()))
            createValidationException("Unexpected tag: " + reader.getNodeName());

        Patient patient = new Patient();
        int patItemCount = 0, tumorCount = 0;
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            if (NaaccrXmlUtils.NAACCR_XML_TAG_ITEM.equals(reader.getNodeName())) {
                if (tumorCount > 0)
                    throw new RuntimeException("Tumors should come after items...");
                patItemCount++;
                String path = "Patient/Item[" + patItemCount + "]";
                patient.getItems().add(createItem(patient, _parser.getLineNumber(), path, "Patient", reader.getAttribute("naaccrId"), reader.getAttribute("naaccrNum"), reader.getValue()));
            }
            else if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(reader.getNodeName())) {
                Tumor tumor = new Tumor();
                tumorCount++;
                int tumorItemCount = 0;
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    tumorItemCount++;
                    if (NaaccrXmlUtils.NAACCR_XML_TAG_ITEM.equals(reader.getNodeName())) {
                        String path = "Patient/Tumor[ + " + tumorCount + "]/Item[" + tumorItemCount + "]";
                        tumor.getItems().add(createItem(tumor, _parser.getLineNumber(), path, "Tumor", reader.getAttribute("naaccrId"), reader.getAttribute("naaccrNum"), reader.getValue()));
                    }
                    else
                        throw createValidationException("Unexpected tag: " + reader.getNodeName());
                    reader.moveUp();
                }
                patient.getTumors().add(tumor);
            }
            else
                throw createValidationException("Unexpected tag: " + reader.getNodeName());
            reader.moveUp();
        }

        return patient;
    }

    private Item createItem(AbstractEntity entity, int lineNumber, String currentPath, String parentTag, String rawId, String rawNum, String value) {
        Item item = new Item();

        if (rawId != null) {
            rawId = rawId.trim();
            if (!rawId.isEmpty())
                item.setId(rawId);
        }

        if (rawNum != null) {
            rawNum = rawNum.trim();
            if (!rawNum.isEmpty()) {
                try {
                    item.setNum(Integer.valueOf(rawNum));
                }
                catch (NumberFormatException e) {
                    entity.getValidationErrors().add(new NaaccrValidationError("invalid 'naaccrNum' attribute value: " + rawNum, lineNumber, currentPath));
                }
            }
        }

        item.setValue(value);

        // item should have either the naaccrId or the naaccrNum
        if (item.getId() == null && item.getNum() == null)
            entity.getValidationErrors().add(new NaaccrValidationError("'naaccrId' and 'naaccrNum' attributes cannot be both missing", lineNumber, currentPath));

        // item should be found
        RuntimeNaaccrDictionaryItem itemDef;
        if (item.getId() != null) {
            itemDef = _dictionary.getItemByNaaccrId(item.getId());
            if (itemDef == null)
                entity.getValidationErrors().add(new NaaccrValidationError("invalid 'naaccrId' attribute value: " + item.getId(), lineNumber, currentPath));
        }
        else {
            itemDef = _dictionary.getItemByNaaccrNum(item.getNum());
            if (itemDef == null)
                entity.getValidationErrors().add(new NaaccrValidationError("invalid 'naaccrNum' attribute value: " + item.getNum(), lineNumber, currentPath));
        }

        if (itemDef != null) {
            
            // item should be under the proper patient level
            if (!parentTag.equals(itemDef.getParentXmlElement()))
                entity.getValidationErrors().add(new NaaccrValidationError("invalid parent XML tag; was expecting '" + itemDef.getParentXmlElement() + "' but got '" + parentTag + "'", lineNumber,
                        currentPath));

            // item should be in the proper record type
            if (!itemDef.getRecordTypes().contains(_dictionary.getFormat().getRecordType()))
                entity.getValidationErrors().add(new NaaccrValidationError("item '" + itemDef.getNaaccrId() + "' is not allowed for this record type", lineNumber, currentPath));

            // value should be valid
            // TODO FPD use regex for value validation; also need to use the options...
        }

        return item;
    }

    private ConversionException createValidationException(String message) {
        ConversionException ex = new ConversionException(message);
        ex.add("message", message);
        return ex;
    }
}
