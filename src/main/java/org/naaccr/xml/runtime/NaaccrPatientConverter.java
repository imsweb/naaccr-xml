/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.runtime;

import java.util.HashSet;
import java.util.Set;

import org.naaccr.xml.NaaccrOptions;
import org.naaccr.xml.NaaccrValidationError;
import org.naaccr.xml.NaaccrXmlDictionaryUtils;
import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.entity.AbstractEntity;
import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * This class is telling the library how to read/write patients from/to XML.
 */
public class NaaccrPatientConverter implements Converter {

    protected NaaccrStreamContext _context;

    /**
     * Sets the stream context; this method must be called prior to any reading/writing operation.
     * @param context stream context to set, cannot be null
     */
    public void setContext(NaaccrStreamContext context) {
        _context = context;
    }

    @Override
    public boolean canConvert(Class type) {
        return Patient.class.isAssignableFrom(type);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        if (!(source instanceof Patient))
            reportSyntaxError("Unexpected object type: " + source.getClass().getName());

        Patient patient = (Patient)source;
        for (Item item : patient.getItems())
            writeItem(item, writer);

        // TODO this would be the place to write the patient extension...

        for (Tumor tumor : patient.getTumors()) {
            writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            for (Item item : tumor.getItems())
                writeItem(item, writer);

            // TODO this would be the place to write the tumor extension...

            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        if (!NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(reader.getNodeName()))
            reportSyntaxError("unexpected tag: " + reader.getNodeName());

        Patient patient = new Patient();
        patient.setStartLineNumber(_context.getParser().getLineNumber());
        int patItemCount = 0, tumorCount = 0;
        boolean seenPatientExtension = false;
        Set<String> itemsAlreadySeen = new HashSet<>();
        while (reader.hasMoreChildren()) {
            reader.moveDown();

            // handel patient items
            if (NaaccrXmlUtils.NAACCR_XML_TAG_ITEM.equals(reader.getNodeName())) {
                if (tumorCount > 0 || seenPatientExtension)
                    reportSyntaxError("unexpected tag: " + reader.getNodeName());
                patItemCount++;
                String path = "/Patient/Item[" + patItemCount + "]";
                String rawId = reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID);
                String rawNum = reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM);
                readItem(patient, path, NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT, rawId, rawNum, reader.getValue());
                if (rawId != null && itemsAlreadySeen.contains(rawId))
                    reportSyntaxError("item '" + rawId + "' should be unique within the " + NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT + " tags");
                else
                    itemsAlreadySeen.add(rawId);
            }
            // handle tumors
            else if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(reader.getNodeName())) {
                Tumor tumor = new Tumor();
                tumor.setStartLineNumber(_context.getParser().getLineNumber());
                tumorCount++;
                int tumorItemCount = 0;
                boolean seenTumorExtension = false;
                itemsAlreadySeen.clear();
                while (reader.hasMoreChildren()) {
                    reader.moveDown();

                    // handle tumor items
                    if (NaaccrXmlUtils.NAACCR_XML_TAG_ITEM.equals(reader.getNodeName())) {
                        if (seenTumorExtension)
                            reportSyntaxError("unexpected tag: " + reader.getNodeName());
                        tumorItemCount++;
                        String path = "/Patient/Tumor[" + tumorCount + "]/Item[" + tumorItemCount + "]";
                        String rawId = reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID);
                        String rawNum = reader.getAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM);
                        readItem(tumor, path, NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR, rawId, rawNum, reader.getValue());
                        if (rawId != null && itemsAlreadySeen.contains(rawId))
                            reportSyntaxError("item '" + rawId + "' should be unique within the " + NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR + " tags");
                        else
                            itemsAlreadySeen.add(rawId);
                    }
                    // handle tumor extension
                    else {
                        // TODO this would be the place to read the tumor extension; for now it's ignored...
                        reader.moveUp();
                        reader.moveDown();
                    }

                    reader.moveUp();
                }
                patient.getTumors().add(tumor);
            }
            // handle patient extension
            else {
                if (tumorCount > 0)
                    reportSyntaxError("unexpected tag: " + reader.getNodeName());
                // TODO this would be the place to read the patient extension; for now it's ignored...
                reader.moveUp();
                reader.moveDown();
            }

            reader.moveUp();
        }

        return patient;
    }

    public void writeItem(Item item, HierarchicalStreamWriter writer) {

        // don't bother if the item has no value!
        if (item.getValue() == null || item.getValue().isEmpty())
            return;

        // get the item definition
        if (item.getNaaccrId() == null)
            reportSyntaxError("NAACCR ID is required when writing an item");
        if (_context.getOptions().getItemsToExclude().contains(item.getNaaccrId()))
            return;
        RuntimeNaaccrDictionaryItem itemDef = _context.getDictionary().getItemByNaaccrId(item.getNaaccrId());
        if (itemDef == null)
            reportSyntaxError("unable to find item definition for NAACCR ID " + item.getNaaccrId());
        if (item.getNaaccrNum() != null && !item.getNaaccrNum().equals(itemDef.getNaaccrNum()))
            reportSyntaxError("provided NAACCR Number '" + item.getNaaccrNum() + "' doesn't correspond to the provided NAACCR ID '" + item.getNaaccrId() + "'");

        // write the item
        writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM);
        writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID, itemDef.getNaaccrId());
        if (itemDef.getNaaccrNum() != null && _context.getOptions().getWriteItemNumber())
            writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM, itemDef.getNaaccrNum().toString());
        writer.setValue(item.getValue());
        writer.endNode();
    }

    public void readItem(AbstractEntity entity, String currentPath, String parentTag, String rawId, String rawNum, String value) {
        int lineNumber = _context.getParser().getLineNumber();

        // if there is no value at all, don't bother
        if (value == null || value.isEmpty())
            return;

        // create the item
        Item item = new Item();
        item.setValue(value);

        // the NAACCR ID is required
        if (rawId == null)
            reportSyntaxError("attribute '" + NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID + "' is required");
        rawId = rawId.trim();
        if (_context.getOptions().getItemsToExclude().contains(rawId))
            return;
        RuntimeNaaccrDictionaryItem itemDef = _context.getDictionary().getItemByNaaccrId(rawId);
        if (itemDef != null) {
            item.setNaaccrId(itemDef.getNaaccrId());
            item.setNaaccrNum(itemDef.getNaaccrNum());
        }
        else {
            if (NaaccrOptions.ITEM_HANDLING_PROCESS.equals(_context.getOptions().getUnknownItemHandling()))
                item.setNaaccrId(rawId);
            else if (NaaccrOptions.ITEM_HANDLING_ERROR.equals(_context.getOptions().getUnknownItemHandling())) {
                reportError(entity, lineNumber, currentPath, "unknown '" + NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID + "' attribute value: " + rawId);
                return;
            }
            else if (NaaccrOptions.ITEM_HANDLING_IGNORE.equals(_context.getOptions().getUnknownItemHandling()))
                return;
            else
                throw new RuntimeException("Unknown option: " + _context.getOptions().getUnknownItemHandling());
        }

        // validate the NAACCR Number if provided
        if (rawNum != null) {
            rawNum = rawNum.trim();
            if (!rawNum.isEmpty()) {
                try {
                    if (itemDef != null) {
                        if (!Integer.valueOf(rawNum).equals(itemDef.getNaaccrNum()))
                            reportError(entity, lineNumber, currentPath, "invalid '" + NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM + "' attribute value: " + rawNum);
                    }
                    else
                        item.setNaaccrNum(Integer.valueOf(rawNum));
                }
                catch (NumberFormatException e) {
                    reportSyntaxError("invalid '" + NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM + "' attribute value: " + rawNum);
                }
            }
        }

        // the rest of the validation will happen only if we actually find the item definition...
        if (itemDef != null) {

            // item should be under the proper patient level
            if (!parentTag.equals(itemDef.getParentXmlElement()))
                reportSyntaxError("invalid parent XML tag; was expecting '" + itemDef.getParentXmlElement() + "' but got '" + parentTag + "'");

            // value should be valid
            if (item.getValue() != null && _context.getOptions().getValidateValues()) {
                if (item.getValue().length() > itemDef.getLength())
                    reportError(entity, lineNumber, currentPath, "value too long, expected at most " + itemDef.getLength() + " character(s) but got " + item.getValue().length(), itemDef,
                            item.getValue());
                else if (exactLengthRequired(itemDef.getDataType()) && item.getValue().length() != itemDef.getLength())
                    reportError(entity, lineNumber, currentPath, "invalid value, expected exactly " + itemDef.getLength() + " character(s) but got " + item.getValue().length(), itemDef,
                            item.getValue());
                else if (itemDef.getDataType() != null && !NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(itemDef.getDataType()).matcher(item.getValue()).matches())
                    reportError(entity, lineNumber, currentPath, "invalid value for data type '" + itemDef.getDataType() + "'", itemDef, item.getValue());
                else if (itemDef.getRegexValidation() != null && !itemDef.getRegexValidation().matcher(item.getValue()).matches())
                    reportError(entity, lineNumber, currentPath, "invalid value", itemDef, item.getValue());
            }

        }

        entity.getItems().add(item);
    }

    protected boolean exactLengthRequired(String type) {
        boolean result = NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_ALPHA.equals(type);
        result |= NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS.equals(type);
        result |= NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_MIXED.equals(type);
        return result;
    }

    protected void reportError(AbstractEntity entity, int line, String path, String msg) {
        reportError(entity, line, path, msg, null, null);
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

    protected void reportSyntaxError(String message) {
        ConversionException ex = new ConversionException(message);
        ex.add("message", message);
        throw ex;
    }
}
