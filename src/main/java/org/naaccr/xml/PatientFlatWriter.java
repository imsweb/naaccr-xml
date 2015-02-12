/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionaryItem;

public class PatientFlatWriter implements AutoCloseable {

    protected BufferedWriter _writer;

    protected RuntimeNaaccrDictionary _dictionary;

    protected RuntimeNaaccrDictionaryItem _naaccrVersionItem, _recordTypeItem;

    public PatientFlatWriter(Writer writer, RuntimeNaaccrDictionary dictionary) throws IOException {
        _writer = new BufferedWriter(writer);
        _dictionary = dictionary;
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
                if (!itemDef.getRecordTypes().contains(_dictionary.getFormat().getRecordType()))
                    break;
                
                // sub-items are handled as part of the parent, so ignore them here
                if (itemDef.getGroupNaaccrId() != null)
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

                    // get value; if the item defines sub-items, always use the sub-items
                    if (!itemDef.getSubItems().isEmpty()) {
                        for (RuntimeNaaccrDictionaryItem subItemDef : itemDef.getSubItems()) {
                            int subStart = subItemDef.getStartColumn();
                            int subLength = subItemDef.getLength();
                            int subEnd = start + length - 1;

                            // adjust for the "leading" gap within the sub-items
                            if (subStart > currentIndex)
                                for (int i = 0; i < subStart - currentIndex; i++)
                                    line.append(' ');
                            currentIndex = subStart;

                            if (subEnd <= end) { // do not write the current sub-item out if it can potentially go out of the space
                                String value = getValueForItem(subItemDef, patient, patient.getTumors().get(0));
                                if (value == null)
                                    value = "";
                                if (value.length() > subLength)
                                    value = value.substring(0, subLength);
                                line.append(value);
                                currentIndex = subStart + value.length();
                            }
                        }

                        // adjust for the "trailing" gap within the sub-items
                        if (currentIndex <= end)
                            for (int i = 0; i < end - currentIndex + 1; i++)
                                line.append(' ');
                        currentIndex = end + 1;
                    }
                    else {
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
            }

            // adjust for the final "trailing" gap
            if (currentIndex <= _dictionary.getFormat().getLineLength())
                for (int i = 0; i < _dictionary.getFormat().getLineLength() - currentIndex + 1; i++)
                    line.append(' ');

            // always use the format to write the NAACCR version and record type
            if (_dictionary.getFormat().getRecordType() != null && _recordTypeItem != null)
                line.replace(_recordTypeItem.getStartColumn() - 1, _recordTypeItem.getStartColumn() + _recordTypeItem.getLength() - 1, _dictionary.getFormat().getRecordType());
            if (_dictionary.getFormat().getNaaccrVersion() != null && _naaccrVersionItem != null)
                line.replace(_naaccrVersionItem.getStartColumn() - 1, _naaccrVersionItem.getStartColumn() + _naaccrVersionItem.getLength() - 1, _dictionary.getFormat().getNaaccrVersion());
            
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

    // TODO remove this testing method
    public static void main(String[] args) throws IOException {
        Patient patient = new Patient();
        patient.getItems().add(new Item("nameLast", "DEPRY"));
        patient.getItems().add(new Item("nameFirst", "FABIAN"));
        patient.getTumors().add(new Tumor());
        patient.getTumors().get(0).getItems().add(new Item("primarySite", "C619"));
        patient.getTumors().get(0).getItems().add(new Item("hosptialAbstractorId", "FDEPRY")); // should be ignored because not defined
        patient.getTumors().add(new Tumor());
        patient.getTumors().get(1).getItems().add(new Item("primarySite", "C447"));
        File outputFile = new File(System.getProperty("user.dir") + "/build/write-flat-test.txt");
        RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, NaaccrXmlUtils.getStandardDictionary(), null);
        PatientFlatWriter writer = new PatientFlatWriter(new FileWriter(outputFile), dictionary);
        writer.writePatient(patient);
        writer.close();
    }
}
