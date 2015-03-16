/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionaryItem;

public class PatientFlatWriter implements AutoCloseable {

    protected BufferedWriter _writer;

    protected NaaccrXmlOptions _options;

    protected RuntimeNaaccrDictionary _dictionary;

    protected RuntimeNaaccrDictionaryItem _naaccrVersionItem, _recordTypeItem;

    public PatientFlatWriter(Writer writer, String format) throws IOException {
        this(writer, format, null, null);
    }

    public PatientFlatWriter(Writer writer, String format, NaaccrXmlOptions options) throws IOException {
        this(writer, format, options, null);
    }

    public PatientFlatWriter(Writer writer, String format, NaaccrXmlOptions options, NaaccrDictionary userDictionary) throws IOException {
        _writer = new BufferedWriter(writer);
        _options = options == null ? new NaaccrXmlOptions() : options;

        // TODO FPD add better validation

        NaaccrFormat naaccrFormat = NaaccrFormat.getInstance(format);
        NaaccrDictionary baseDictionary = NaaccrDictionaryUtils.getBaseDictionaryByVersion(naaccrFormat.getNaaccrVersion());
        _dictionary = new RuntimeNaaccrDictionary(naaccrFormat.getRecordType(), baseDictionary, userDictionary);

        for (RuntimeNaaccrDictionaryItem item : _dictionary.getItems()) {
            if (item.getNaaccrNum() != null) {
                if (item.getNaaccrNum().equals(10))
                    _recordTypeItem = item;
                if (item.getNaaccrNum().equals(50))
                    _naaccrVersionItem = item;
                if (_recordTypeItem != null && _naaccrVersionItem != null)
                    break;
            }
        }
    }

    public void writePatient(Patient patient) throws IOException {
        for (String line : createLinesFromPatient(patient)) {
            _writer.write(line);
            _writer.newLine();
        }
    }

    @Override
    public void close() throws IOException {
        _writer.close();
    }

    protected List<String> createLinesFromPatient(Patient patient) throws IOException {
        List<String> lines = new ArrayList<>();

        // it's possible to have a patient without any tumor; in that case, we will want to output a line...
        List<Tumor> tumors = new ArrayList<>(patient.getTumors());
        if (tumors.isEmpty())
            tumors.add(new Tumor());

        for (Tumor tumor : tumors) {
            int currentIndex = 1;
            StringBuilder line = new StringBuilder();
            for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems()) {

                // as soon as an item is not supported for the dictionary's record type, we can stop (making the assumption the items are correctly sorted)
                if (!itemDef.getRecordTypes().contains(_dictionary.getRecordType()))
                    break;

                if (itemDef.getParentXmlElement() != null && itemDef.getStartColumn() != null && itemDef.getLength() != null) {
                    int start = itemDef.getStartColumn();
                    int length = itemDef.getLength();
                    int end = start + length - 1;

                    // TODO this code doesn't handle padding yet...

                    // adjust for the "leading" gap
                    if (start > currentIndex)
                        for (int i = 0; i < start - currentIndex; i++)
                            line.append(' ');
                    currentIndex = start;

                    String value = getValueForItem(itemDef, patient, tumor);
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

    protected String getValueForItem(RuntimeNaaccrDictionaryItem itemDef, Patient patient, Tumor tumor) throws IOException {
        Item item;

        if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentXmlElement()))
            item = patient.getItem(itemDef.getNaaccrId(), itemDef.getNaaccrNum());
        else if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentXmlElement()))
            item = tumor.getItem(itemDef.getNaaccrId(), itemDef.getNaaccrNum());
        else
            throw new IOException("Unsupported parent element: " + itemDef.getParentXmlElement());

        return item == null ? null : item.getValue();
    }
}
