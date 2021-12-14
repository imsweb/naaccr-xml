/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.imsweb.naaccrxml.entity.AbstractEntity;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;
import com.imsweb.naaccrxml.runtime.RuntimeNaaccrDictionary;
import com.imsweb.naaccrxml.runtime.RuntimeNaaccrDictionaryItem;

import static com.imsweb.naaccrxml.NaaccrOptions.NEW_LINE_CRLF;
import static com.imsweb.naaccrxml.NaaccrOptions.NEW_LINE_LF;

/**
 * This class can be used to wrap a generic writer into a patient writer handling the NAACCR flat-file format.
 */
public class PatientFlatWriter implements PatientWriter {

    // the underlined writer
    protected BufferedWriter _writer;

    // the root data to write for each line
    protected NaaccrData _rootData;

    // the options requested to use when writing the patients
    protected NaaccrOptions _options;

    // the runtime dictionary (combination of base and user-defined dictionaries)
    protected RuntimeNaaccrDictionary _dictionary;

    // cached special data items
    protected RuntimeNaaccrDictionaryItem _naaccrVersionItem, _recordTypeItem;

    // cached value for new line character(s)
    protected String _newLine;

    // cached pattern for new lines
    private static final Pattern _NEW_LINES_PATTERN = Pattern.compile("(\r\n|\n|\r)");

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param data required root data
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatWriter(Writer writer, NaaccrData data) throws NaaccrIOException {
        this(writer, data, null, (NaaccrDictionary)null, null);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param data required root data
     * @param options optional options
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatWriter(Writer writer, NaaccrData data, NaaccrOptions options) throws NaaccrIOException {
        this(writer, data, options, (NaaccrDictionary)null, null);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param data required root data
     * @param options optional options
     * @param userDictionary optional user-defined dictionary
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatWriter(Writer writer, NaaccrData data, NaaccrOptions options, NaaccrDictionary userDictionary) throws NaaccrIOException {
        this(writer, data, options, Collections.singletonList(userDictionary), null);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param data required root data
     * @param options optional options
     * @param userDictionaries optional user-defined dictionaries (can be null or empty)
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatWriter(Writer writer, NaaccrData data, NaaccrOptions options, List<NaaccrDictionary> userDictionaries) throws NaaccrIOException {
        this(writer, data, options, userDictionaries, null);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param data required root data
     * @param options optional options
     * @param userDictionary optional user-defined dictionary
     * @param conf optional stream configuration
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatWriter(Writer writer, NaaccrData data, NaaccrOptions options, NaaccrDictionary userDictionary, NaaccrStreamConfiguration conf) throws NaaccrIOException {
        this(writer, data, options, Collections.singletonList(userDictionary), conf);
    }

    /**
     * Constructor.
     * @param writer required underlined writer
     * @param data required root data
     * @param options optional options
     * @param userDictionaries optional user-defined dictionaries (can be null or empty)
     * @param conf optional stream configuration
     * @throws NaaccrIOException if there is problem creating the stream
     */
    public PatientFlatWriter(Writer writer, NaaccrData data, NaaccrOptions options, List<NaaccrDictionary> userDictionaries, NaaccrStreamConfiguration conf) throws NaaccrIOException {
        _writer = new BufferedWriter(writer);
        _rootData = data;
        _options = options == null ? new NaaccrOptions() : options;
        _newLine = NEW_LINE_LF.equals(_options.getNewLine()) ? "\n" : NEW_LINE_CRLF.equals(_options.getNewLine()) ? "\r\n" : System.getProperty("line.separator");

        // there should be better validation here...

        // try to use the cached runtime dictionary, create one if there isn't a cached one...
        _dictionary = conf == null ? null : conf.getCachedDictionary();
        if (_dictionary == null) {
            NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByUri(data.getBaseDictionaryUri());
            _dictionary = new RuntimeNaaccrDictionary(data.getRecordType(), baseDictionary, userDictionaries);
        }

        if (_dictionary.getLineLength() == null || _dictionary.getLineLength() == -1)
            throw new NaaccrIOException("version " + _dictionary.getNaaccrVersion() + " does not support start columns and cannot be used with this flat writer");

        // let's cache the record type and naaccr version items; we are going to use them a lot...
        for (RuntimeNaaccrDictionaryItem item : _dictionary.getItems()) {
            if (item.getNaaccrId().equals(NaaccrXmlUtils.FLAT_FILE_FORMAT_ITEM_REC_TYPE))
                _recordTypeItem = item;
            if (item.getNaaccrId().equals(NaaccrXmlUtils.FLAT_FILE_FORMAT_ITEM_NAACCR_VERSION))
                _naaccrVersionItem = item;
            if (_recordTypeItem != null && _naaccrVersionItem != null)
                break;
        }
    }

    @Override
    public void writePatient(Patient patient) throws NaaccrIOException {
        for (String line : createLinesFromPatient(_rootData, patient)) {
            try {
                _writer.write(line);
                _writer.write(_newLine);
            }
            catch (IOException e) {
                throw new NaaccrIOException(e.getMessage());
            }
        }
    }

    @Override
    public void closeAndKeepAlive() throws NaaccrIOException {
        try {
            _writer.flush();
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }
    }

    @Override
    public void close() throws NaaccrIOException {
        closeAndKeepAlive();
        try {
            _writer.close();
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }
    }

    /**
     * Returns the new line character(s) this writer uses.
     */
    public String getNewLine() {
        return _newLine;
    }

    protected List<String> createLinesFromPatient(NaaccrData root, Patient patient) throws NaaccrIOException {
        List<String> lines = new ArrayList<>();

        // it's possible to have a patient without any tumor; in that case, we will want to output a line...
        List<Tumor> tumors = new ArrayList<>(patient.getTumors());
        if (tumors.isEmpty())
            tumors.add(new Tumor());

        for (Tumor tumor : tumors) {
            int currentIndex = 1;
            StringBuilder line = new StringBuilder();
            for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems()) {
                if (!NaaccrOptions.processItem(_options, itemDef.getNaaccrId()))
                    continue;
                // as of spec 1.1, the start column is optional for user-defined items, so let's ignore those
                if (itemDef.getStartColumn() == null)
                    continue;
                if (itemDef.getParentXmlElement() != null && itemDef.getStartColumn() != null && itemDef.getLength() != null) {
                    int start = itemDef.getStartColumn();
                    int length = itemDef.getLength();
                    int end = start + length - 1;

                    // adjust for the "leading" gap
                    if (start > currentIndex)
                        for (int i = 0; i < start - currentIndex; i++)
                            line.append(' ');
                    currentIndex = start;

                    String value = getValueForItem(itemDef, root, patient, tumor, Boolean.TRUE.equals(_options.getApplyZeroPaddingRules()));
                    if (value != null) {
                        line.append(value);
                        currentIndex = start + value.length();
                    }

                    // adjust for the "trailing" gap
                    if (currentIndex <= end)
                        for (int i = 0; i < end - currentIndex + 1; i++)
                            line.append(' ');
                    currentIndex = end + 1;
                }
            }

            // adjust for the final "trailing" gap
            if (currentIndex <= _dictionary.getLineLength())
                for (int i = 0; i < _dictionary.getLineLength() - currentIndex + 1; i++)
                    line.append(' ');

            // always use the format to write the NAACCR version and record type
            if (_dictionary.getRecordType() != null && _recordTypeItem != null)
                line.replace(_recordTypeItem.getStartColumn() - 1, _recordTypeItem.getStartColumn() + _recordTypeItem.getLength() - 1, _dictionary.getRecordType());
            if (_dictionary.getNaaccrVersion() != null && _naaccrVersionItem != null)
                line.replace(_naaccrVersionItem.getStartColumn() - 1, _naaccrVersionItem.getStartColumn() + _naaccrVersionItem.getLength() - 1, _dictionary.getNaaccrVersion());

            lines.add(line.toString());
        }

        return lines;
    }

    protected String getValueForItem(RuntimeNaaccrDictionaryItem itemDef, NaaccrData root, Patient patient, Tumor tumor, boolean applyZeroPadding) throws NaaccrIOException {
        String value;

        AbstractEntity entityToUse;
        if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(itemDef.getParentXmlElement()))
            entityToUse = root;
        else if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentXmlElement()))
            entityToUse = patient;
        else if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentXmlElement()))
            entityToUse = tumor;
        else
            throw new NaaccrIOException("unsupported parent element: " + itemDef.getParentXmlElement());
        value = entityToUse.getItemValue(itemDef.getNaaccrId());

        // handle the padding (always apply the space padding because it's an attribute of the format more than the data)
        if (value != null && !value.isEmpty() && itemDef.getLength() != null && itemDef.getPadding() != null && value.length() < itemDef.getLength()) {
            if (NaaccrXmlDictionaryUtils.NAACCR_PADDING_LEFT_BLANK.equals(itemDef.getPadding()))
                value = StringUtils.leftPad(value, itemDef.getLength(), ' ');
            else if (NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_BLANK.equals(itemDef.getPadding()))
                value = StringUtils.rightPad(value, itemDef.getLength(), ' ');
            else if (NaaccrXmlDictionaryUtils.NAACCR_PADDING_LEFT_ZERO.equals(itemDef.getPadding())) {
                if (applyZeroPadding)
                    value = StringUtils.leftPad(value, itemDef.getLength(), '0');
            }
            else if (NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_ZERO.equals(itemDef.getPadding())) {
                if (applyZeroPadding)
                    value = StringUtils.rightPad(value, itemDef.getLength(), '0');
            }
            else
                throw new RuntimeException("Unknown padding option: " + itemDef.getPadding());
        }

        // handle new lines (can't have that in flat files)
        if (value != null && !value.isEmpty())
            value = _NEW_LINES_PATTERN.matcher(value).replaceAll(" ");

        // for flat-file values, we always have to truncate, so the "allowUnlimitedText" is used only to know if we have to report an error
        if (value != null && value.length() > itemDef.getLength()) {
            if (!Boolean.TRUE.equals(itemDef.getAllowUnlimitedText()) && _options.getReportValuesTooLong())
                reportError(entityToUse, itemDef, value, NaaccrErrorUtils.CODE_VAL_TOO_LONG, itemDef.getLength(), value.length());
            value = value.substring(0, itemDef.getLength());
        }

        return value;
    }

    @SuppressWarnings("SameParameterValue")
    protected void reportError(AbstractEntity entity, RuntimeNaaccrDictionaryItem def, String value, String code, Object... msgValues) {
        NaaccrValidationError error = new NaaccrValidationError(code, msgValues);
        if (def != null) {
            error.setNaaccrId(def.getNaaccrId());
            error.setNaaccrNum(def.getNaaccrNum());
        }
        if (value != null && !value.isEmpty())
            error.setValue(value);
        entity.addValidationError(error);
    }
}
