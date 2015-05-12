/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.naaccr.xml.entity.NaaccrData;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.runtime.RuntimeNaaccrDictionary;
import org.naaccr.xml.runtime.RuntimeNaaccrDictionaryItem;

/**
 * This class can be used to wrap a generic writer into a patient writer handling the NAACCR flat-file format.
 */
public class PatientFlatWriter implements AutoCloseable {

    protected BufferedWriter _writer;

    protected NaaccrData _rootData;

    protected NaaccrOptions _options;

    protected RuntimeNaaccrDictionary _dictionary;

    protected RuntimeNaaccrDictionaryItem _naaccrVersionItem, _recordTypeItem;

    public PatientFlatWriter(Writer writer, NaaccrData data) throws NaaccrIOException {
        this(writer, data, null, null);
    }

    public PatientFlatWriter(Writer writer, NaaccrData data, NaaccrOptions options) throws NaaccrIOException {
        this(writer, data, options, null);
    }

    public PatientFlatWriter(Writer writer, NaaccrData data, NaaccrOptions options, NaaccrDictionary userDictionary) throws NaaccrIOException {
        _writer = new BufferedWriter(writer);
        _rootData = data;
        _options = options == null ? new NaaccrOptions() : options;

        // TODO FPD add better validation

        NaaccrDictionary baseDictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByUri(data.getBaseDictionaryUri());
        _dictionary = new RuntimeNaaccrDictionary(data.getRecordType(), baseDictionary, userDictionary);

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

    public void writePatient(Patient patient) throws NaaccrIOException {
        for (String line : createLinesFromPatient(_rootData, patient)) {
            try {
                _writer.write(line);
                _writer.newLine();
            }
            catch (IOException e) {
                throw new NaaccrIOException(e.getMessage());
            }
        }
    }

    @Override
    public void close() throws NaaccrIOException {
        try {
            _writer.close();
        }
        catch (IOException e) {
            throw new NaaccrIOException(e.getMessage());
        }
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
                if (_options.getItemsToExclude().contains(itemDef.getNaaccrId()))
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

                    String value = getValueForItem(itemDef, root, patient, tumor);
                    if (value != null) {
                        if (value.length() > length)
                            value = value.substring(0, length);
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

    protected String getValueForItem(RuntimeNaaccrDictionaryItem itemDef, NaaccrData root, Patient patient, Tumor tumor) throws NaaccrIOException {
        String value;

        if (NaaccrXmlUtils.NAACCR_XML_TAG_ROOT.equals(itemDef.getParentXmlElement()))
            value = root.getItemValue(itemDef.getNaaccrId());
        else if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentXmlElement()))
            value = patient.getItemValue(itemDef.getNaaccrId());
        else if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentXmlElement()))
            value = tumor.getItemValue(itemDef.getNaaccrId());
        else
            throw new NaaccrIOException("Unsupported parent element: " + itemDef.getParentXmlElement());

        // handle the padding
        if (value != null && !value.isEmpty() && itemDef.getLength() != null && itemDef.getPadding() != null && value.length() < itemDef.getLength()) {
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

        return value;
    }
}
