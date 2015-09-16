/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import com.imsweb.naaccrxml.NaaccrErrorUtils;
import com.imsweb.naaccrxml.NaaccrOptions;
import com.imsweb.naaccrxml.NaaccrValidationError;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.AbstractEntity;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;

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
        else {
            Patient patient = (Patient)source;
            for (Item item : patient.getItems())
                writeItem(item, writer);

            // TODO [EXTENSIONS] this would be the place to write the patient extension...

            for (Tumor tumor : patient.getTumors()) {
                writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
                for (Item item : tumor.getItems())
                    writeItem(item, writer);

                // TODO [EXTENSIONS] this would be the place to write the tumor extension...

                writer.endNode();
            }
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
                    // TODO [EXTENSIONS] this would be the place to read the tumor extension; for now it's ignored...

                    reader.moveUp();
                }
                patient.getTumors().add(tumor);
            }
            // handle patient extension
            else {
                if (tumorCount > 0)
                    reportSyntaxError("unexpected tag: " + reader.getNodeName());
                // TODO [EXTENSIONS] this would be the place to read the patient extension; for now it's ignored...
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
        RuntimeNaaccrDictionaryItem def = _context.getDictionary().getItemByNaaccrId(item.getNaaccrId());
        if (def == null)
            reportSyntaxError("unable to find item definition for NAACCR ID " + item.getNaaccrId());
        else {
            if (item.getNaaccrNum() != null && !item.getNaaccrNum().equals(def.getNaaccrNum()))
                reportSyntaxError("provided NAACCR Number '" + item.getNaaccrNum() + "' doesn't correspond to the provided NAACCR ID '" + item.getNaaccrId() + "'");

            // write the item
            writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM);
            writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID, def.getNaaccrId());
            if (def.getNaaccrNum() != null && _context.getOptions().getWriteItemNumber())
                writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM, def.getNaaccrNum().toString());
            writer.setValue(item.getValue());
            writer.endNode();
        }
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
        else
            rawId = rawId.trim();
        if (_context.getOptions().getItemsToExclude().contains(rawId))
            return;
        RuntimeNaaccrDictionaryItem def = _context.getDictionary().getItemByNaaccrId(rawId);
        if (def != null) {
            item.setNaaccrId(def.getNaaccrId());
            item.setNaaccrNum(def.getNaaccrNum());
        }
        else {
            if (NaaccrOptions.ITEM_HANDLING_PROCESS.equals(_context.getOptions().getUnknownItemHandling()))
                item.setNaaccrId(rawId);
            else if (NaaccrOptions.ITEM_HANDLING_ERROR.equals(_context.getOptions().getUnknownItemHandling())) {
                reportError(entity, lineNumber, currentPath, null, null, NaaccrErrorUtils.CODE_BAD_NAACCR_ID, rawId);
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
                    if (def != null) {
                        if (!Integer.valueOf(rawNum).equals(def.getNaaccrNum()))
                            reportError(entity, lineNumber, currentPath, null, null, NaaccrErrorUtils.CODE_BAD_NAACCR_NUM, rawNum, def.getNaaccrId());
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
        if (def != null) {

            // item should be under the proper patient level
            if (!parentTag.equals(def.getParentXmlElement()))
                reportSyntaxError("invalid parent XML tag; was expecting '" + def.getParentXmlElement() + "' but got '" + parentTag + "'");

            // value should be valid
            if (item.getValue() != null && _context.getOptions().getValidateReadValues()) {
                if (item.getValue().length() > def.getLength())
                    reportError(entity, lineNumber, currentPath, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_TOO_LONG, def.getLength(), item.getValue().length());
                else if (NaaccrXmlDictionaryUtils.isFullLengthRequiredForType(def.getDataType()) && item.getValue().length() != def.getLength())
                    reportError(entity, lineNumber, currentPath, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_TOO_SHORT, def.getLength(), item.getValue().length());
                else if (def.getDataType() != null && !NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(def.getDataType()).matcher(item.getValue()).matches())
                    reportError(entity, lineNumber, currentPath, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_DATA_TYPE, def.getDataType());
                else if (def.getRegexValidation() != null && !def.getRegexValidation().matcher(item.getValue()).matches())
                    reportError(entity, lineNumber, currentPath, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_DATA_TYPE, def.getRegexValidation());
            }

        }

        entity.getItems().add(item);
    }

    protected void reportError(AbstractEntity entity, int line, String path, RuntimeNaaccrDictionaryItem def, String value, String code, Object... msgValues) {
        NaaccrValidationError error = new NaaccrValidationError(code, msgValues);
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

    protected void reportSyntaxError(String message) throws ConversionException {
        ConversionException ex = new ConversionException(message);
        ex.add("message", message);
        throw ex;
    }
}
