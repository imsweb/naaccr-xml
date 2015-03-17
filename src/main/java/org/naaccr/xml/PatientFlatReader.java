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
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionaryItem;

public class PatientFlatReader implements AutoCloseable {

    protected LineNumberReader _reader;

    protected NaaccrData _rootData;

    protected NaaccrXmlOptions _options;

    protected RuntimeNaaccrDictionary _dictionary;

    protected List<RuntimeNaaccrDictionaryItem> _groupingItems;

    protected String _previousLine;

    public PatientFlatReader(Reader reader, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws IOException {
        _reader = new LineNumberReader(reader);
        _options = options == null ? new NaaccrXmlOptions() : options;

        // TODO FPD add better validation

        _previousLine = _reader.readLine();
        NaaccrFormat naaccrFormat = NaaccrFormat.getInstance(NaaccrXmlUtils.getFormatFromFlatFileLine(_previousLine));
        NaaccrDictionary baseDictionary = NaaccrDictionaryUtils.getBaseDictionaryByVersion(naaccrFormat.getNaaccrVersion());
        _dictionary = new RuntimeNaaccrDictionary(naaccrFormat.getRecordType(), baseDictionary, userDictionary);
        _rootData = new NaaccrData(naaccrFormat.toString());

        // read the root items
        if (_previousLine != null)
            for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems())
                if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(itemDef.getParentXmlElement()) && !_options.getItemsToExclude().contains(itemDef.getNaaccrId()))
                    addItemFromLine(_rootData, _previousLine, itemDef);

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
     * @throws IOException
     */
    public Patient readPatient() throws IOException {
        List<String> lines = new ArrayList<>();
        List<Integer> lineNumbers = new ArrayList<>();

        if (_previousLine == null) {
            _previousLine = _reader.readLine();
            if (_previousLine == null) // would be an empty file...
                return null;
        }

        Map<String, String> firstLineGroupingValues = extractGroupingValues(_previousLine, _groupingItems);
        lines.add(_previousLine);
        lineNumbers.add(_reader.getLineNumber());
        _previousLine = _reader.readLine();
        while (_previousLine != null) {
            boolean samePatient = firstLineGroupingValues.equals(extractGroupingValues(_previousLine, _groupingItems));
            if (samePatient) {
                lines.add(_previousLine);
                lineNumbers.add(_reader.getLineNumber());
                _previousLine = _reader.readLine();
            }
            else
                break;
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
    public void close() throws IOException {
        _reader.close();
    }

    protected Map<String, String> extractGroupingValues(String line, List<RuntimeNaaccrDictionaryItem> itemDefs) {
        Map<String, String> values = new HashMap<>();

        for (RuntimeNaaccrDictionaryItem itemDef : itemDefs) {
            Item item = createItemFromLine(line, itemDef);
            if (item != null)
                values.put(item.getNaaccrId(), item.getValue());
        }

        return values;
    }

    protected Patient createPatientFromLines(List<String> lines, List<Integer> lineNumbers) throws IOException {
        Patient patient = new Patient();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            Tumor tumor = new Tumor();
            for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems()) {
                if (_options.getItemsToExclude().contains(itemDef.getNaaccrId()))
                    continue;
                if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentXmlElement())) {
                    if (i == 0)
                        addItemFromLine(patient, line, itemDef);
                    else if (_options.getReportLevelMismatch()) {
                        Item currentTumorItem = createItemFromLine(line, itemDef);
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
                    addItemFromLine(tumor, line, itemDef);
            }
            patient.getTumors().add(tumor);
        }

        return patient;
    }

    protected void addItemFromLine(AbstractEntity entity, String line, RuntimeNaaccrDictionaryItem itemDef) {
        Item item = createItemFromLine(line, itemDef);
        if (item != null)
            entity.getItems().add(item);
    }

    protected Item createItemFromLine(String line, RuntimeNaaccrDictionaryItem itemDef) {
        Item item = null;

        int start = itemDef.getStartColumn() - 1; // dictionary is 1-based; Java substring is 0-based...
        int end = start + itemDef.getLength();

        if (end <= line.length()) {
            String value = line.substring(start, end);
            String trimmedValue = value.trim();

            // apply trimming rule (no trimming rule means trim all)
            if (trimmedValue.isEmpty() || itemDef.getTrim() == null || NaaccrDictionaryUtils.NAACCR_TRIM_ALL.equals(itemDef.getTrim()))
                value = trimmedValue;

            if (!value.isEmpty()) {
                item = new Item();
                item.setNaaccrId(itemDef.getNaaccrId());
                item.setNaaccrNum(itemDef.getNaaccrNum());
                item.setValue(value);
            }
        }

        return item;
    }
}
