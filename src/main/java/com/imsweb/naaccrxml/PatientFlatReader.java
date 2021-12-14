/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.imsweb.naaccrxml.entity.AbstractEntity;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;
import com.imsweb.naaccrxml.runtime.RuntimeNaaccrDictionary;
import com.imsweb.naaccrxml.runtime.RuntimeNaaccrDictionaryItem;

/**
 * This class can be used to wrap a generic reader into a patient reader handling the NAACCR flat-file format.
 */
public class PatientFlatReader implements PatientReader {

    // the underlined line reader
    protected LineNumberReader _reader;

    // the root data (determined by the first line of the reader)
    protected NaaccrData _rootData;

    // the options requested to use when reading the patients
    protected NaaccrOptions _options;

    // the runtime dictionary (combination of base and user-defined dictionaries)
    protected RuntimeNaaccrDictionary _dictionary;

    // the cached items that need to be used to know whether tumors belong to the same patient
    protected List<RuntimeNaaccrDictionaryItem> _groupingItems;

    // the NAACCR format to use
    protected NaaccrFormat _format;

    // reference to the previous data line in the reader
    protected String _previousLine;

    /**
     * Constructor
     * @param reader required underlined reader
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatReader(Reader reader) throws NaaccrIOException {
        this(reader, null, (NaaccrDictionary)null, null);
    }

    /**
     * Constructor
     * @param reader required underlined reader
     * @param options optional options
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatReader(Reader reader, NaaccrOptions options) throws NaaccrIOException {
        this(reader, options, (NaaccrDictionary)null, null);
    }

    /**
     * Constructor
     * @param reader required underlined reader
     * @param options optional options
     * @param userDictionary optional user-defined dictionary
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatReader(Reader reader, NaaccrOptions options, NaaccrDictionary userDictionary) throws NaaccrIOException {
        this(reader, options, Collections.singletonList(userDictionary), null);
    }

    /**
     * Constructor
     * @param reader required underlined reader
     * @param options optional options
     * @param userDictionaries optional user-defined dictionaries (can be null or empty)
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatReader(Reader reader, NaaccrOptions options, List<NaaccrDictionary> userDictionaries) throws NaaccrIOException {
        this(reader, options, userDictionaries, null);
    }

    /**
     * Constructor
     * @param reader required underlined reader
     * @param options optional options
     * @param userDictionary optional user-defined dictionary
     * @param conf optional stream configuration
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatReader(Reader reader, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrStreamConfiguration conf) throws NaaccrIOException {
        this(reader, options, Collections.singletonList(userDictionary), conf);
    }

    /**
     * Constructor
     * @param reader required underlined reader
     * @param options optional options
     * @param userDictionaries optional user-defined dictionaries (can be null or empty)
     * @param conf optional stream configuration
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatReader(Reader reader, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrStreamConfiguration conf) throws NaaccrIOException {
        _reader = new LineNumberReader(reader);
        _options = options == null ? new NaaccrOptions() : options;

        try {
            _previousLine = _reader.readLine();
        }
        catch (IOException e) {
            throw new NaaccrIOException("unable to read first line");
        }

        if (_previousLine == null || _previousLine.isEmpty())
            throw new NaaccrIOException("first line is empty");

        // make sure the NAACCR version is valid
        String version = _previousLine.length() < 19 ? "" : _previousLine.substring(16, 19).trim();
        if (version.isEmpty())
            throw new NaaccrIOException("unable to get NAACCR version from first record");
        if (!NaaccrFormat.isVersionSupported(version))
            throw new NaaccrIOException("invalid/unsupported NAACCR version on first record: " + version);

        // make sure the record type is valid
        String type = _previousLine.substring(0, 1).trim();
        if (type.isEmpty())
            throw new NaaccrIOException("unable to get record type on first record");
        if (!NaaccrFormat.isRecordTypeSupported(type))
            throw new NaaccrIOException("invalid/unsupported record type on first record: " + type);

        _format = NaaccrFormat.getInstance(version, type);

        // try to use the cached runtime dictionary, create one if there isn't a cached one...
        _dictionary = conf == null ? null : conf.getCachedDictionary();
        if (_dictionary == null) {
            NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(_format.getNaaccrVersion());
            _dictionary = new RuntimeNaaccrDictionary(_format.getRecordType(), baseDictionary, userDictionaries);
        }
        _rootData = new NaaccrData(_format.toString());
        _rootData.setSpecificationVersion(NaaccrXmlUtils.CURRENT_SPECIFICATION_VERSION);

        // make sure first line has the correct length
        if (_previousLine.length() != _format.getLineLength())
            throw new NaaccrIOException("invalid line length for first record, expected " + _format.getLineLength() + " but got " + _previousLine.length());

        // read the root items
        for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems())
            if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(itemDef.getParentXmlElement()))
                addItemFromLine(_rootData, _previousLine, _reader.getLineNumber(), itemDef);

        // let's cache the grouping items, we are going to need them a lot...
        _groupingItems = new ArrayList<>();
        if (_options.getTumorGroupingItems() != null) {
            for (String id : _options.getTumorGroupingItems()) {
                RuntimeNaaccrDictionaryItem item = _dictionary.getItemByNaaccrId(id);
                if (item != null)
                    _groupingItems.add(item);
            }
        }
    }

    @Override
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
                boolean samePatient = !firstLineGroupingValues.isEmpty() && firstLineGroupingValues.equals(extractGroupingValues(_previousLine, _reader.getLineNumber(), _groupingItems));
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

    @Override
    public NaaccrData getRootData() {
        return _rootData;
    }

    @Override
    public void closeAndKeepAlive() {
        // does nothing
    }

    @Override
    public void close() throws NaaccrIOException {
        closeAndKeepAlive();
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

    protected Patient createPatientFromLines(List<String> lines, List<Integer> lineNumbers) {
        Patient patient = new Patient();
        patient.setStartLineNumber(lineNumbers.get(0));
        patient.setEndLineNumber(lineNumbers.get(0)); // for flat, start and end are the same

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Integer lineNumber = lineNumbers.get(i);

            if (line.length() != _format.getLineLength())
                reportError(patient, lineNumber, null, null, NaaccrErrorUtils.CODE_BAD_LINE_LENGTH, _format.getLineLength(), line.length());

            Tumor tumor = new Tumor();
            tumor.setStartLineNumber(lineNumber);
            tumor.setEndLineNumber(lineNumber); // for flat, start and end are the same
            for (RuntimeNaaccrDictionaryItem def : _dictionary.getItems()) {
                if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(def.getParentXmlElement())) {
                    if (_options.getReportLevelMismatch()) {
                        Item currentTumorItem = createItemFromLine(null, line, lineNumber, def);
                        String rootValue = _rootData.getItemValue(def.getNaaccrId());
                        String tumorValue = currentTumorItem == null ? null : currentTumorItem.getValue();
                        boolean same = rootValue == null ? tumorValue == null : rootValue.equals(tumorValue);
                        if (!same)
                            reportError(tumor, lineNumber, def, null, NaaccrErrorUtils.CODE_VAL_ROOT_VS_TUM, def.getNaaccrId());
                    }
                }
                else if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(def.getParentXmlElement())) {
                    if (i == 0)
                        addItemFromLine(patient, line, lineNumber, def);
                    else if (_options.getReportLevelMismatch()) {
                        Item currentTumorItem = createItemFromLine(null, line, lineNumber, def);
                        String patValue = patient.getItemValue(def.getNaaccrId());
                        String tumorValue = currentTumorItem == null ? null : currentTumorItem.getValue();
                        boolean same = patValue == null ? tumorValue == null : patValue.equals(tumorValue);
                        if (!same)
                            reportError(tumor, lineNumber, def, null, NaaccrErrorUtils.CODE_VAL_PAT_VS_TUM, def.getNaaccrId());
                    }
                }
                else if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(def.getParentXmlElement()))
                    addItemFromLine(tumor, line, lineNumber, def);
            }
            patient.addTumor(tumor);
        }

        return patient;
    }

    protected void addItemFromLine(AbstractEntity entity, String line, Integer lineNumber, RuntimeNaaccrDictionaryItem def) {

        // as of spec 1.1, the start column is optional for user-defined items, so let's ignore those
        if (def.getStartColumn() == null)
            return;

        Item item = createItemFromLine(entity, line, lineNumber, def);
        if (item != null && NaaccrOptions.processItem(_options, def.getNaaccrId()))
            entity.addItem(item);
    }

    protected Item createItemFromLine(AbstractEntity entity, String line, Integer lineNumber, RuntimeNaaccrDictionaryItem def) {
        Item item = null;

        int start = def.getStartColumn() - 1; // dictionary is 1-based; Java substring is 0-based...
        int end = start + def.getLength();

        if (end <= line.length()) {
            String value = line.substring(start, end);

            // apply trimming rule (no trimming rule means trim all)
            String trimmedValue = value.trim();
            if (trimmedValue.isEmpty() || def.getTrim() == null || NaaccrXmlDictionaryUtils.NAACCR_TRIM_ALL.equals(def.getTrim()))
                value = trimmedValue;

            if (!value.isEmpty()) {
                // create the item
                item = new Item(def.getNaaccrId(), def.getNaaccrNum(), value, lineNumber);

                // validate the value
                if (entity != null) {
                    if (item.getValue().length() > def.getLength())
                        reportError(entity, lineNumber, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_TOO_LONG, def.getLength(), item.getValue().length());
                    if (_options.getValidateReadValues()) {
                        if (NaaccrXmlDictionaryUtils.isFullLengthRequiredForType(def.getDataType()) && item.getValue().length() < def.getLength())
                            reportError(entity, lineNumber, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_TOO_SHORT, def.getLength(), item.getValue().length());
                        else if (def.getDataType() != null) {
                            Pattern pattern = NaaccrXmlDictionaryUtils.getDataTypePattern(def.getDataType());
                            if (pattern != null && !pattern.matcher(item.getValue()).matches())
                                reportError(entity, lineNumber, def, item.getValue(), NaaccrErrorUtils.CODE_VAL_DATA_TYPE, def.getDataType());
                        }
                    }
                }
            }
        }

        return item;
    }

    protected void reportError(AbstractEntity entity, int line, RuntimeNaaccrDictionaryItem def, String value, String code, Object... msgValues) {
        NaaccrValidationError error = new NaaccrValidationError(code, msgValues);
        error.setLineNumber(line);
        if (def != null) {
            error.setNaaccrId(def.getNaaccrId());
            error.setNaaccrNum(def.getNaaccrNum());
        }
        if (value != null && !value.isEmpty())
            error.setValue(value);
        entity.addValidationError(error);
    }
}
