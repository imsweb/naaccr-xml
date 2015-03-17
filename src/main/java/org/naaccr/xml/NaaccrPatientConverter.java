/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import org.naaccr.xml.entity.AbstractEntity;
import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionaryItem;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class NaaccrPatientConverter implements Converter {

    protected NaaccrStreamContext _context;

    /**
     * Sets the stream context; this method must be called prior to any reading/writing operation.
     * @param context sream context to set, cannot be null
     */
    public void setContext(NaaccrStreamContext context) {
        _context = context;
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(Patient.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        if (!(source instanceof Patient))
            throw reportSyntaxError("Unexpected object type: " + source.getClass().getName());

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

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        if (!NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(reader.getNodeName()))
            throw reportSyntaxError("Unexpected tag: " + reader.getNodeName());

        Patient patient = new Patient();
        int patItemCount = 0, tumorCount = 0;

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            // handel patient items
            if (NaaccrXmlUtils.NAACCR_XML_TAG_ITEM.equals(reader.getNodeName())) {
                if (tumorCount > 0)
                    throw reportSyntaxError("Tumors should come after items...");
                patItemCount++;
                String path = "/Patient/Item[" + patItemCount + "]";
                patient.getItems().add(readItem(patient, path, "Patient", reader.getAttribute("naaccrId"), reader.getAttribute("naaccrNum"), reader.getValue()));
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
                        tumor.getItems().add(readItem(tumor, path, "Tumor", reader.getAttribute("naaccrId"), reader.getAttribute("naaccrNum"), reader.getValue()));
                    }
                    else
                        throw reportSyntaxError("Unexpected tag: " + reader.getNodeName());
                    reader.moveUp();
                }
                patient.getTumors().add(tumor);
            }
            else
                throw reportSyntaxError("Unexpected tag: " + reader.getNodeName());
            reader.moveUp();
        }

        return patient;
    }


    protected void writeItem(Item item, HierarchicalStreamWriter writer) {

        // don't bother if the item has no value!
        if (item.getValue() == null || item.getValue().isEmpty())
            return;

        // get the item definition
        if (item.getId() == null)
            throw reportSyntaxError("NAACCR ID is required when writing an item");
        RuntimeNaaccrDictionaryItem itemDef = _context.getDictionary().getItemByNaaccrId(item.getId());
        if (itemDef == null)
            throw reportSyntaxError("Unable to find item definition for NAACCR ID " + item.getId());
        if (item.getNum() != null && !item.getNum().equals(itemDef.getNaaccrNum()))
            throw reportSyntaxError("Provided NAACCR Number '" + item.getNum() + "' doesn't correspond to the provided NAACCR ID '" + item.getId() + "'");

        // write the item
        writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM);
        writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID, itemDef.getNaaccrId());
        if (itemDef.getNaaccrNum() != null && _context.getOptions().getWriteItemNumber())
            writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM, itemDef.getNaaccrNum().toString());
        writer.setValue(item.getValue());
        writer.endNode();
    }

    protected Item readItem(AbstractEntity entity, String currentPath, String parentTag, String rawId, String rawNum, String value) {
        int lineNumber = _context.getParser().getLineNumber();

        // the NAACCR ID is required
        if (rawId == null)
            throw reportSyntaxError("attribute 'naaccrId' is required");
        rawId = rawId.trim();
        RuntimeNaaccrDictionaryItem itemDef = _context.getDictionary().getItemByNaaccrId(rawId);
        if (itemDef == null)
            reportError(entity, lineNumber, currentPath, "unknown 'naaccrId' attribute value: " + rawId);

        // validate the NAACCR Number if provided
        if (rawNum != null) {
            rawNum = rawNum.trim();
            if (!rawNum.isEmpty()) {
                try {
                    if (!Integer.valueOf(rawNum).equals(itemDef.getNaaccrNum()))
                        reportError(entity, lineNumber, currentPath, "invalid 'naaccrNum' attribute value: " + rawNum);
                }
                catch (NumberFormatException e) {
                    reportError(entity, lineNumber, currentPath, "invalid 'naaccrNum' attribute value: " + rawNum);
                }
            }
        }

        // crate the item
        Item item = new Item();
        item.setId(itemDef.getNaaccrId());
        item.setNum(itemDef.getNaaccrNum());
        item.setValue(value);

        // the rest of the validation will happen only if we actually find the item definition...
        if (itemDef != null) {

            // item should be under the proper patient level
            if (!parentTag.equals(itemDef.getParentXmlElement()))
                reportError(entity, lineNumber, currentPath, "invalid parent XML tag; was expecting '" + itemDef.getParentXmlElement() + "' but got '" + parentTag + "'", itemDef);

            // item should be in the proper record type
            if (!itemDef.getRecordTypes().contains(_context.getDictionary().getRecordType()))
                reportError(entity, lineNumber, currentPath, "item '" + itemDef.getNaaccrId() + "' is not allowed for this record type", itemDef);

            // value should be valid
            if (item.getValue() != null) {
                if (item.getValue().length() > itemDef.getLength())
                    reportError(entity, lineNumber, currentPath, "value too long, expected at most " + itemDef.getLength() + " character(s) but got " + item.getValue().length(), itemDef,
                            item.getValue());
                else if (exactLengthRequired(itemDef.getDataType()) && item.getValue().length() != itemDef.getLength())
                    reportError(entity, lineNumber, currentPath, "invalid value, expected exactly " + itemDef.getLength() + " character(s) but got " + item.getValue().length(), itemDef,
                            item.getValue());
                else if (itemDef.getDataType() != null && !NaaccrDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(itemDef.getDataType()).matcher(item.getValue()).matches())
                    reportError(entity, lineNumber, currentPath, "invalid value according to the definition of data type '" + itemDef.getDataType() + "'", itemDef, item.getValue());
                else if (itemDef.getRegexValidation() != null && !itemDef.getRegexValidation().matcher(item.getValue()).matches())
                    reportError(entity, lineNumber, currentPath, "invalid value according to specific item validation", itemDef, item.getValue());
            }

        }

        return item;
    }

    protected boolean exactLengthRequired(String type) {
        boolean result = NaaccrDictionaryUtils.NAACCR_DATA_TYPE_ALPHA.equals(type);
        result |= NaaccrDictionaryUtils.NAACCR_DATA_TYPE_DIGITS.equals(type);
        result |= NaaccrDictionaryUtils.NAACCR_DATA_TYPE_MIXED.equals(type);
        return result;
    }

    protected void reportError(AbstractEntity entity, int line, String path, String msg) {
        reportError(entity, line, path, msg, null, null);
    }


    protected void reportError(AbstractEntity entity, int line, String path, String msg, RuntimeNaaccrDictionaryItem def) {
        reportError(entity, line, path, msg, def, null);
    }

    protected void reportError(AbstractEntity entity, int line, String path, String msg, RuntimeNaaccrDictionaryItem def, String value) {
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

    protected ConversionException reportSyntaxError(String message) {
        ConversionException ex = new ConversionException(message);
        ex.add("message", message);
        return ex;
    }
}
