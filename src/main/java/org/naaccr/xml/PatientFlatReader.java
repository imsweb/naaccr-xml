/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.naaccr.xml.entity.AbstractEntity;
import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.runtime.RuntimeNaaccrDictionary;
import org.naaccr.xml.runtime.RuntimeNaaccrDictionaryItem;

/**
 * This class can be used to wrap a generic reader into a patient reader handling the NAACCR flat-file format.
 */
public class PatientFlatReader implements AutoCloseable {

    protected LineNumberReader _reader;

    protected NaaccrData _rootData;

    protected NaaccrOptions _options;

    protected RuntimeNaaccrDictionary _dictionary;

    protected List<RuntimeNaaccrDictionaryItem> _groupingItems;

    protected String _previousLine;

    public PatientFlatReader(Reader reader, NaaccrOptions options, NaaccrDictionary userDictionary) throws NaaccrIOException {
        _reader = new LineNumberReader(reader);
        _options = options == null ? new NaaccrOptions() : options;

        try {
            _previousLine = _reader.readLine();
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }
        NaaccrFormat naaccrFormat = NaaccrFormat.getInstance(NaaccrXmlUtils.getFormatFromFlatFileLine(_previousLine));
        NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(naaccrFormat.getNaaccrVersion());
        _dictionary = new RuntimeNaaccrDictionary(naaccrFormat.getRecordType(), baseDictionary, userDictionary);
        _rootData = new NaaccrData(naaccrFormat.toString());

        // read the root items
        if (_previousLine != null)
            for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems())
                if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(itemDef.getParentXmlElement()))
                    addItemFromLine(_rootData, _previousLine, _reader.getLineNumber(), itemDef);

        // let's cache the grouping items, we are going to need them a lot...
        _groupingItems = new ArrayList<>();
        for (String id : _options.getTumorGroupingItems()) {
            RuntimeNaaccrDictionaryItem item = _dictionary.getItemByNaaccrId(id);
            if (item != null)
                _groupingItems.add(item);
        }
    }

    /**
     * Reads the next patient on this stream.
     * @return the next available patient, null if not such patient
     * @throws NaaccrIOException
     */
    public Patient readPatient() throws NaaccrIOException {
        List<String> lines = new ArrayList<>();
        List<Integer> lineNumbers = new ArrayList<>();

        try {
            if (_previousLine == null) {
                _previousLine = _reader.readLine();
                if (_previousLine == null) // would be an empty file...
                    return null;
            }

            Map<String, String> firstLineGroupingValues = extractGroupingValues(_previousLine, _reader.getLineNumber(), _groupingItems);
            lines.add(_previousLine);
            lineNumbers.add(_reader.getLineNumber());
            _previousLine = _reader.readLine();
            while (_previousLine != null) {
                boolean samePatient = !_groupingItems.isEmpty() && firstLineGroupingValues.equals(extractGroupingValues(_previousLine, _reader.getLineNumber(), _groupingItems));
                if (samePatient) {
                    lines.add(_previousLine);
                    lineNumbers.add(_reader.getLineNumber());
                    _previousLine = _reader.readLine();
                }
                else
                    break;
            }
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }

        return lines.isEmpty() ? null : createPatientFromLines(lines, lineNumbers);
    }

    /**
     * Returns the "root" data; it includes root attributes and the root items.
     * @return the root data, never null
     */
    public NaaccrData getRootData() {
        return _rootData;
    }

    @Override
    public void close() throws NaaccrIOException {
        try {
            _reader.close();
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }
    }

    protected Map<String, String> extractGroupingValues(String line, Integer linNumber, List<RuntimeNaaccrDictionaryItem> itemDefs) {
        Map<String, String> values = new HashMap<>();

        for (RuntimeNaaccrDictionaryItem itemDef : itemDefs) {
            Item item = createItemFromLine(null, line, linNumber, itemDef);
            if (item != null)
                values.put(item.getNaaccrId(), item.getValue());
        }

        return values;
    }

    protected Patient createPatientFromLines(List<String> lines, List<Integer> lineNumbers) throws NaaccrIOException {
        Patient patient = new Patient();
        patient.setStartLineNumber(lineNumbers.get(0));

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Integer lineNumber = lineNumbers.get(i);

            Tumor tumor = new Tumor();
            tumor.setStartLineNumber(lineNumber);
            for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems()) {
                if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentXmlElement())) {
                    if (i == 0)
                        addItemFromLine(patient, line, lineNumber, itemDef);
                    else if (_options.getReportLevelMismatch()) {
                        Item currentTumorItem = createItemFromLine(null, line, lineNumber, itemDef);
                        String patValue = patient.getItemValue(itemDef.getNaaccrId());
                        String tumorValue = currentTumorItem == null ? null : currentTumorItem.getValue();
                        boolean same = patValue == null ? tumorValue == null : patValue.equals(tumorValue);
                        if (!same) {
                            NaaccrValidationError error = new NaaccrValidationError();
                            error.setMessage("item '" + itemDef.getNaaccrId() + "' is defined at the patient level but has different values for the tumors");
                            error.setLineNumber(lineNumbers.get(i));
                            error.setNaaccrId(itemDef.getNaaccrId());
                            error.setNaaccrNum(itemDef.getNaaccrNum());
                            patient.getValidationErrors().add(error);
                        }
                    }
                }
                else if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentXmlElement()))
                    addItemFromLine(tumor, line, lineNumber, itemDef);
            }
            patient.getTumors().add(tumor);
        }

        return patient;
    }

    protected void addItemFromLine(AbstractEntity entity, String line, Integer lineNumber, RuntimeNaaccrDictionaryItem itemDef) {
        Item item = createItemFromLine(entity, line, lineNumber, itemDef);
        if (item != null && !_options.getItemsToExclude().contains(itemDef.getNaaccrId()))
            entity.getItems().add(item);
    }

    protected Item createItemFromLine(AbstractEntity entity, String line, Integer lineNumber, RuntimeNaaccrDictionaryItem itemDef) {
        Item item = null;

        int start = itemDef.getStartColumn() - 1; // dictionary is 1-based; Java substring is 0-based...
        int end = start + itemDef.getLength();

        if (end <= line.length()) {
            String value = line.substring(start, end);
            String trimmedValue = value.trim();

            // apply trimming rule (no trimming rule means trim all)
            if (trimmedValue.isEmpty() || itemDef.getTrim() == null || NaaccrXmlDictionaryUtils.NAACCR_TRIM_ALL.equals(itemDef.getTrim()))
                value = trimmedValue;

            if (!value.isEmpty()) {
                item = new Item();
                item.setNaaccrId(itemDef.getNaaccrId());
                item.setNaaccrNum(itemDef.getNaaccrNum());
                item.setValue(value);

                if (entity != null && _options.getValidateValues()) {
                    if (item.getValue().length() > itemDef.getLength())
                        reportError(entity, lineNumber, "value too long, expected at most " + itemDef.getLength() + " character(s) but got " + item.getValue().length(), itemDef,
                                item.getValue());
                    else if (exactLengthRequired(itemDef.getDataType()) && item.getValue().length() != itemDef.getLength())
                        reportError(entity, lineNumber, "invalid value, expected exactly " + itemDef.getLength() + " character(s) but got " + item.getValue().length(), itemDef,
                                item.getValue());
                    else if (itemDef.getDataType() != null && !NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPES_REGEX.get(itemDef.getDataType()).matcher(item.getValue()).matches())
                        reportError(entity, lineNumber, "invalid value for data type '" + itemDef.getDataType() + "'", itemDef, item.getValue());
                    else if (itemDef.getRegexValidation() != null && !itemDef.getRegexValidation().matcher(item.getValue()).matches())
                        reportError(entity, lineNumber, "invalid value", itemDef, item.getValue());
                }
            }
        }

        return item;
    }

    protected boolean exactLengthRequired(String type) {
        boolean result = NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_ALPHA.equals(type);
        result |= NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS.equals(type);
        result |= NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_MIXED.equals(type);
        return result;
    }

    protected void reportError(AbstractEntity entity, int line, String msg, RuntimeNaaccrDictionaryItem def, String value) {
        NaaccrValidationError error = new NaaccrValidationError();
        error.setMessage(msg);
        error.setLineNumber(line);
        if (def != null) {
            error.setNaaccrId(def.getNaaccrId());
            error.setNaaccrNum(def.getNaaccrNum());
        }
        if (value != null && !value.isEmpty())
            error.setValue(value);
        entity.getValidationErrors().add(error);
    }
}
