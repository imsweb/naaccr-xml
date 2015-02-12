/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.AbstractEntity;
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
        if (!(source instanceof Patient))
            throw createValidationException("Unexpected object type: " + source.getClass().getName());
        
        Patient patient = (Patient)source;
        for (Item item : patient.getItems())
            writeItem(item, writer);
        for (Tumor tumor : patient.getTumors()) {
            writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            for (Item item : tumor.getItems())
                writeItem(item, writer);
            writer.endNode();
        }
    }
    
    protected void writeItem(Item item, HierarchicalStreamWriter writer) {
        RuntimeNaaccrDictionaryItem itemDef = _dictionary.getItem(item);
        if (itemDef == null) {
            if (item.getId() != null)
                throw createValidationException("Unable to find item definition for NAACCR ID " + item.getId());
            else if (item.getNum() != null)
                throw createValidationException("Unable to find item definition for NAACCR Number " + item.getNum());
            else
                throw createValidationException("Unable to find item definition; both NAACCR ID and NAACCR Number are missing");
        }

        writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM);
        writer.addAttribute("naaccrId", itemDef.getNaaccrId());
        if (item.getValue() != null && !item.getValue().isEmpty())
            writer.setValue(item.getValue());
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        if (!NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(reader.getNodeName()))
            throw createValidationException("Unexpected tag: " + reader.getNodeName());

        Patient patient = new Patient();
        int patItemCount = 0, tumorCount = 0;

        while (reader.hasMoreChildren()) {
            reader.moveDown();
            
            // handel patient items
            if (NaaccrXmlUtils.NAACCR_XML_TAG_ITEM.equals(reader.getNodeName())) {
                if (tumorCount > 0)
                    throw new RuntimeException("Tumors should come after items...");
                patItemCount++;
                String path = "/Patient/Item[" + patItemCount + "]";
                patient.getItems().add(readItem(patient, _parser.getLineNumber(), path, "Patient", reader.getAttribute("naaccrId"), reader.getAttribute("naaccrNum"), reader.getValue()));
            }
            // handle tumors
            else if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(reader.getNodeName())) {
                Tumor tumor = new Tumor();
                tumorCount++;
                int tumorItemCount = 0;
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    tumorItemCount++;
                    
                    // handle tumor items
                    if (NaaccrXmlUtils.NAACCR_XML_TAG_ITEM.equals(reader.getNodeName())) {
                        String path = "/Patient/Tumor[" + tumorCount + "]/Item[" + tumorItemCount + "]";
                        tumor.getItems().add(readItem(tumor, _parser.getLineNumber(), path, "Tumor", reader.getAttribute("naaccrId"), reader.getAttribute("naaccrNum"), reader.getValue()));
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

    protected Item readItem(AbstractEntity entity, int lineNumber, String currentPath, String parentTag, String rawId, String rawNum, String value) {
        Item item = new Item();

        // assign the NAACCR ID
        if (rawId != null) {
            rawId = rawId.trim();
            if (!rawId.isEmpty()) {
                item.setId(rawId);
                if (_dictionary.getItemByNaaccrId(item.getId()) == null)
                    addError(entity, lineNumber, currentPath, "unknown 'naaccrId' attribute value: " + rawId);
            }
        }

        // assign the NAACCR Number
        if (rawNum != null) {
            rawNum = rawNum.trim();
            if (!rawNum.isEmpty()) {
                try {
                    item.setNum(Integer.valueOf(rawNum));
                    if (_dictionary.getItemByNaaccrNum(item.getNum()) == null)
                        addError(entity, lineNumber, currentPath, "unknown 'naaccrNum' attribute value: " + rawNum);
                }
                catch (NumberFormatException e) {
                    addError(entity, lineNumber, currentPath, "invalid 'naaccrNum' attribute value: " + rawNum);
                }
            }
        }

        // assign the value
        if (value != null && !value.isEmpty())
            item.setValue(value);

        // get the item definition
        RuntimeNaaccrDictionaryItem itemDef = null;
        if (item.getId() != null)
            itemDef = _dictionary.getItemByNaaccrId(item.getId());
        else if (item.getNum() != null)
            itemDef = _dictionary.getItemByNaaccrNum(item.getNum());
        else
            addError(entity, lineNumber, currentPath, "'naaccrId' and 'naaccrNum' attributes cannot be both missing");

        // the rest of the validation will happen only if we actually find the item definition...
        if (itemDef != null) {

            // item should be under the proper patient level
            if (!parentTag.equals(itemDef.getParentXmlElement()))
                addError(entity, lineNumber, currentPath, "invalid parent XML tag; was expecting '" + itemDef.getParentXmlElement() + "' but got '" + parentTag + "'", itemDef);

            // item should be in the proper record type
            if (!itemDef.getRecordTypes().contains(_dictionary.getFormat().getRecordType()))
                addError(entity, lineNumber, currentPath, "item '" + itemDef.getNaaccrId() + "' is not allowed for this record type", itemDef);

            // value should be valid
            if (item.getValue() != null) {
                if (item.getValue().length() > itemDef.getLength())
                    addError(entity, lineNumber, currentPath, "value too long, expected at most " + itemDef.getLength() + " character(s) but got " + item.getValue().length(), itemDef, item.getValue
                            ());
                else if (exactLengthRequired(itemDef.getDataType()) && item.getValue().length() != itemDef.getLength())
                    addError(entity, lineNumber, currentPath, "invalid value, expected exactly " + itemDef.getLength() + " character(s) but got " + item.getValue().length(), itemDef, item.getValue());
                else if (!NaaccrXmlUtils.NAACCR_DATA_TYPES_REGEX.get(itemDef.getDataType()).matcher(item.getValue()).matches())
                    addError(entity, lineNumber, currentPath, "invalid value according to the definition of data type '" + itemDef.getDataType() + "'", itemDef, item.getValue());
                else if (itemDef.getRegexValidation() != null && !itemDef.getRegexValidation().matcher(item.getValue()).matches())
                    addError(entity, lineNumber, currentPath, "invalid value according to specific item validation", itemDef, item.getValue());
            }

        }

        return item;
    }

    protected void addError(AbstractEntity entity, int line, String path, String msg) {
        addError(entity, line, path, msg, null, null);
    }


    protected void addError(AbstractEntity entity, int line, String path, String msg, RuntimeNaaccrDictionaryItem def) {
        addError(entity, line, path, msg, def, null);
    }
    
    protected void addError(AbstractEntity entity, int line, String path, String msg, RuntimeNaaccrDictionaryItem def, String value) {
        NaaccrValidationError error = new NaaccrValidationError();
        error.setMessage(msg);
        error.setLineNumber(line);
        error.setPath(path);
        if (def != null) {
            error.setNaaccrId(def.getNaaccrId());
            error.setNaaccrNum(def.getNaaccrNum());
        }
        if (value != null && !value.isEmpty())
            error.setValue(value);
        entity.getValidationErrors().add(error);
    }

    protected boolean exactLengthRequired(String type) {
        boolean result = NaaccrXmlUtils.NAACCR_DATA_TYPE_CODE.equals(type);
        result |= NaaccrXmlUtils.NAACCR_DATA_TYPE_CODE_WITH_BLANK.equals(type);
        result |= NaaccrXmlUtils.NAACCR_DATA_TYPE_ALPHA.equals(type);
        result |= NaaccrXmlUtils.NAACCR_DATA_TYPE_ALPHA_WITH_BLANK.equals(type);
        result |= NaaccrXmlUtils.NAACCR_DATA_TYPE_DATE.equals(type);
        result |= NaaccrXmlUtils.NAACCR_DATA_TYPE_INTEGER_WITH_ZERO.equals(type);
        return result;
    }

    protected ConversionException createValidationException(String message) {
        ConversionException ex = new ConversionException(message);
        ex.add("message", message);
        return ex;
    }
}
