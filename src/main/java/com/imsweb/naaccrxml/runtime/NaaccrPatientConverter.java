/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.runtime;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import com.imsweb.naaccrxml.NaaccrErrorUtils;
import com.imsweb.naaccrxml.NaaccrIOException;
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
                writeItem(patient, item, writer);

            // TODO [EXTENSIONS] this would be the place to write the patient extension...

            for (Tumor tumor : patient.getTumors()) {
                writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
                for (Item item : tumor.getItems())
                    writeItem(tumor, item, writer);

                // TODO [EXTENSIONS] this would be the place to write the tumor extension...

                writer.endNode();
            }
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        try {
            if (!NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(_context.extractTag(reader.getNodeName())))
                reportSyntaxError("unexpected tag: " + _context.extractTag(reader.getNodeName()));

            Patient patient = new Patient();
            patient.setStartLineNumber(_context.getLineNumber());
            int patItemCount = 0, tumorCount = 0;
            boolean seenPatientExtension = false;
            Set<String> itemsAlreadySeen = new HashSet<>();
            while (reader.hasMoreChildren()) {
                reader.moveDown();

                // handel patient items
                if (NaaccrXmlUtils.NAACCR_XML_TAG_ITEM.equals(_context.extractTag(reader.getNodeName()))) {
                    if (tumorCount > 0 || seenPatientExtension)
                        reportSyntaxError("unexpected tag: " + _context.extractTag(reader.getNodeName()));
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
                else if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(_context.extractTag(reader.getNodeName()))) {
                    Tumor tumor = new Tumor();
                    tumor.setStartLineNumber(_context.getLineNumber());
                    tumorCount++;
                    int tumorItemCount = 0;
                    boolean seenTumorExtension = false;
                    itemsAlreadySeen.clear();
                    while (reader.hasMoreChildren()) {
                        reader.moveDown();

                        // handle tumor items
                        if (NaaccrXmlUtils.NAACCR_XML_TAG_ITEM.equals(_context.extractTag(reader.getNodeName()))) {
                            if (seenTumorExtension)
                                reportSyntaxError("unexpected tag: " + _context.extractTag(reader.getNodeName()));
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
                    patient.addTumor(tumor);
                }
                // handle patient extension
                else {
                    if (tumorCount > 0)
                        reportSyntaxError("unexpected tag: " + _context.extractTag(reader.getNodeName()));
                    // TODO [EXTENSIONS] this would be the place to read the patient extension; for now it's ignored...
                    reader.moveUp();
                    reader.moveDown();
                }

                reader.moveUp();
            }

            return patient;
        }
        catch (NaaccrIOException e) {
            reportSyntaxError(e.getMessage());
            return null;
        }
    }

    public void writeItem(AbstractEntity entity, Item item, HierarchicalStreamWriter writer) {

        // don't bother if the item has no value!
        if (item.getValue() == null || item.getValue().isEmpty())
            return;

        // get the item definition
        if (item.getNaaccrId() == null)
            reportSyntaxError("NAACCR ID is required when writing an item");
        if (!_context.getOptions().processItem(item.getNaaccrId()))
            return;
        RuntimeNaaccrDictionaryItem itemDef = _context.getDictionary().getItemByNaaccrId(item.getNaaccrId());
        if (itemDef == null)
            reportSyntaxError("unable to find item definition for NAACCR ID " + item.getNaaccrId());
        else {
            if (item.getNaaccrNum() != null && !item.getNaaccrNum().equals(itemDef.getNaaccrNum()))
                reportSyntaxError("provided NAACCR Number '" + item.getNaaccrNum() + "' doesn't correspond to the provided NAACCR ID '" + item.getNaaccrId() + "'");

            // write the item
            writer.startNode(NaaccrXmlUtils.NAACCR_XML_TAG_ITEM);
            writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID, itemDef.getNaaccrId());
            if (itemDef.getNaaccrNum() != null && _context.getOptions().getWriteItemNumber())
                writer.addAttribute(NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_NUM, itemDef.getNaaccrNum().toString());

            String value = item.getValue();

            // handle the padding
            if (Boolean.TRUE.equals(_context.getOptions().getApplyPaddingRules()) && itemDef.getLength() != null && itemDef.getPadding() != null && value.length() < itemDef.getLength()) {
                if (NaaccrXmlDictionaryUtils.NAACCR_PADDING_LEFT_BLANK.equals(itemDef.getPadding()))
                    value = StringUtils.leftPad(value, itemDef.getLength(), ' ');
                else if (NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_BLANK.equals(itemDef.getPadding()))
                    value = StringUtils.rightPad(value, itemDef.getLength(), ' ');
                else if (NaaccrXmlDictionaryUtils.NAACCR_PADDING_LEFT_ZERO.equals(itemDef.getPadding()))
                    value = StringUtils.leftPad(value, itemDef.getLength(), '0');
                else if (NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_ZERO.equals(itemDef.getPadding()))
                    value = StringUtils.rightPad(value, itemDef.getLength(), '0');
                else
                    throw new RuntimeException("Unknown padding option: " + itemDef.getPadding());
            }

            // do we need to truncate the value?
            if (value.length() > itemDef.getLength() && !Boolean.TRUE.equals(itemDef.getAllowUnlimitedText())) {
                if (_context.getOptions().getReportValuesTooLong())
                    reportError(item, null, null, itemDef, value, NaaccrErrorUtils.CODE_VAL_TOO_LONG, itemDef.getLength(), value.length());
                value = value.substring(0, itemDef.getLength());
            }

            writer.setValue(value);
            writer.endNode();
        }
    }

    public void readItem(AbstractEntity entity, String currentPath, String parentTag, String rawId, String rawNum, String value) {
        int lineNumber = _context.getLineNumber();

        // if there is no value at all, don't bother
        if (value == null || value.isEmpty())
            return;

        // create the item
        Item item = new Item();
        item.setValue(value);
        item.setStartLineNumber(lineNumber);

        // the NAACCR ID is required
        if (rawId == null)
            reportSyntaxError("attribute '" + NaaccrXmlUtils.NAACCR_XML_ITEM_ATT_ID + "' is required");
        else
            rawId = rawId.trim();
        if (!_context.getOptions().processItem(rawId))
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
                            reportError(item, lineNumber, currentPath, null, null, NaaccrErrorUtils.CODE_BAD_NAACCR_NUM, rawNum, def.getNaaccrId());
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
            if (item.getValue() != null) {
                if (item.getValue().length() > def.getLength() && (!Boolean.TRUE.equals(def.getAllowUnlimitedText())))
                    reportError(item, lineNumber, currentPath, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_TOO_LONG, def.getLength(), item.getValue().length());
                if (_context.getOptions().getValidateReadValues()) {
                    if (NaaccrXmlDictionaryUtils.isFullLengthRequiredForType(def.getDataType()) && item.getValue().length() != def.getLength())
                        reportError(item, lineNumber, currentPath, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_TOO_SHORT, def.getLength(), item.getValue().length());
                    else if (def.getDataType() != null && !NaaccrXmlDictionaryUtils.getDataTypePattern(def.getDataType()).matcher(item.getValue()).matches())
                        reportError(item, lineNumber, currentPath, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_DATA_TYPE, def.getDataType());
                    else if (def.getRegexValidation() != null && !def.getRegexValidation().matcher(item.getValue()).matches())
                        reportError(item, lineNumber, currentPath, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_DATA_TYPE, def.getRegexValidation());
                }
            }
        }

        entity.addItem(item);
    }

    protected void reportError(Object obj, Integer line, String path, RuntimeNaaccrDictionaryItem def, String value, String code, Object... msgValues) {
        NaaccrValidationError error = new NaaccrValidationError(code, msgValues);
        error.setLineNumber(line);
        error.setPath(path);
        if (def != null) {
            error.setNaaccrId(def.getNaaccrId());
            error.setNaaccrNum(def.getNaaccrNum());
        }
        if (value != null && !value.isEmpty())
            error.setValue(value);

        if (obj instanceof AbstractEntity)
            ((AbstractEntity)obj).addValidationError(error);
        else if (obj instanceof Item)
            ((Item)obj).setValidationError(error);
        else
            throw new RuntimeException("Unsupported type: " + obj.getClass().getName());
    }

    protected void reportSyntaxError(String message) throws ConversionException {
        ConversionException ex = new ConversionException(message);
        ex.add("message", message);
        throw ex;
    }
}
