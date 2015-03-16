/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

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

    protected RuntimeNaaccrDictionaryItem _patientIdNumberItem;

    protected String _previousLine;

    public PatientFlatReader(Reader reader, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws IOException {
        _reader = new LineNumberReader(reader);
        _options = options == null ? new NaaccrXmlOptions() : options;

        // TODO FPD add better validation
        
        _previousLine = _reader.readLine();
        NaaccrFormat naaccrFormat = NaaccrFormat.getInstance(NaaccrXmlUtils.getFormatFromFlatFile(_previousLine));
        NaaccrDictionary baseDictionary = NaaccrDictionaryUtils.getBaseDictionaryByVersion(naaccrFormat.getNaaccrVersion());
        _dictionary = new RuntimeNaaccrDictionary(naaccrFormat.getRecordType(), baseDictionary, userDictionary);
        _rootData = new NaaccrData(naaccrFormat.toString());

        // read the root items
        if (_previousLine != null)
            for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems())
                if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(itemDef.getParentXmlElement()) && itemDef.getRecordTypes().contains(_dictionary.getRecordType()))
                    _rootData.getItems().addAll(createItemsFromLine(_previousLine, itemDef));

        // TODO FPD I think we want to allow another property to be used for the grouping...
        
        // let's cache the patient ID number item, we are going to need them a lot...
        for (RuntimeNaaccrDictionaryItem item : _dictionary.getItems()) {
            if (item.getNaaccrNum() != null && item.getNaaccrNum().equals(20)) {
                _patientIdNumberItem = item;
                break;
            }
        }
    }

    public Patient readPatient() throws IOException {
        List<String> lines = new ArrayList<>();

        if (_previousLine == null) {
            _previousLine = _reader.readLine();
            if (_previousLine == null) // would be an empty file...
                return null;
        }

        lines.add(_previousLine);
        String currentPatId = getPatientIdNumber(_previousLine);
        int lineNumber = _reader.getLineNumber() - 1;

        _previousLine = _reader.readLine();
        while (_previousLine != null) {
            boolean samePatient = currentPatId != null && currentPatId.equals(getPatientIdNumber(_previousLine));
            if (samePatient) {
                lines.add(_previousLine);
                _previousLine = _reader.readLine();
            }
            else
                break;
        }

        return lines.isEmpty() ? null : createPatientFromLines(lineNumber, lines);
    }

    @Override
    public void close() throws IOException {
        _reader.close();
    }

    public NaaccrData getRootData() {
        return _rootData;
    }

    protected String getPatientIdNumber(String line) {
        if (_patientIdNumberItem == null)
            return null;

        int start = _patientIdNumberItem.getStartColumn();
        int end = start + _patientIdNumberItem.getLength() - 1;

        if (line.length() < end)
            return null;

        String result = line.substring(start, end).trim();
        if (result.isEmpty())
            return null;

        return result;
    }
    
    protected Patient createPatientFromLines(int lineNumber, List<String> lines) throws IOException {

        // create the patient using the first line only (other lines are supposed to be identical for patient items)
        Patient patient = new Patient();
        for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems()) {
            if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentXmlElement()) && itemDef.getRecordTypes().contains(_dictionary.getRecordType())) {
                // TODO FPD do we want to throw an error if not all the patient-level fields have the same value?
                patient.getItems().addAll(createItemsFromLine(lines.get(0), itemDef));
            }
        }

        // create the tumors, one per line
        for (String line : lines) {
            Tumor tumor = new Tumor();
            for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems())
                if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentXmlElement()) && itemDef.getRecordTypes().contains(_dictionary.getRecordType()))
                    tumor.getItems().addAll(createItemsFromLine(line, itemDef));
            patient.getTumors().add(tumor);
        }

        return patient;
    }

    protected List<Item> createItemsFromLine(String line, RuntimeNaaccrDictionaryItem itemDef) {
        List<Item> items = new ArrayList<>();

        int start = itemDef.getStartColumn() - 1; // dictionary is 1-based; Java substring is 0-based...
        int end = start + itemDef.getLength();

        if (end <= line.length()) {
            String value = line.substring(start, end);
            String trimmedValue = value.trim();

            // apply trimming rule
            if (trimmedValue.isEmpty() || itemDef.getTrim() == null || NaaccrDictionaryUtils.NAACCR_TRIM_ALL.equals(itemDef.getTrim()))
                value = trimmedValue;

            if (!value.isEmpty()) {
                Item item = new Item();
                item.setId(itemDef.getNaaccrId());
                item.setNum(itemDef.getNaaccrNum());
                item.setValue(value);
                items.add(item);
            }
        }

        return items;
    }
}
