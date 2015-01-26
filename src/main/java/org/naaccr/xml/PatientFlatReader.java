/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.Tumor;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionaryItem;

public class PatientFlatReader implements AutoCloseable {

    protected LineNumberReader _reader;

    protected RuntimeNaaccrDictionary _dictionary;

    protected RuntimeNaaccrDictionaryItem _patientIdNumberItem;

    protected String _previousLine;

    public PatientFlatReader(Reader reader, String format, NaaccrDictionary nonStandardDictionary) throws IOException {
        _reader = new LineNumberReader(reader);
        _dictionary = new RuntimeNaaccrDictionary(format, NaaccrXmlUtils.getStandardDictionary(), nonStandardDictionary);
        for (RuntimeNaaccrDictionaryItem item : _dictionary.getItems()) {
            if (item.getNumber() != null && item.getNumber().equals(20)) {
                _patientIdNumberItem = item;
                break;
            }
        }
    }

    public Patient readPatient() throws IOException {
        List<String> lines = new ArrayList<>();

        if (_previousLine == null)
            _previousLine = _reader.readLine();
            
        if (_previousLine != null) {
            lines.add(_previousLine);

            String line = _reader.readLine();
            boolean samePatient = true;
            while (line != null && samePatient) {
                String prevPatId = getPatientIdNumber(_previousLine);
                samePatient = prevPatId != null && prevPatId.equals(getPatientIdNumber(line));
                if (samePatient)
                    lines.add(line);
                line = _reader.readLine();
            }

            _previousLine = line;
        }

        return lines.isEmpty() ? null : createPatientFromLines(lines);
    }

    @Override
    public void close() throws IOException {
        _reader.close();
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

    protected Patient createPatientFromLines(List<String> lines) throws IOException {

        // create the patient using the first line only (other lines are supposed to be identical for patient items)
        Patient patient = new Patient();
        for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems())
            if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(itemDef.getParentXmlElement()))
                patient.getItems().addAll(createItemsFromLine(lines.get(0), itemDef));

        // create the tumors, one per line
        for (String line : lines) {
            Tumor tumor = new Tumor();
            for (RuntimeNaaccrDictionaryItem itemDef : _dictionary.getItems())
                if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(itemDef.getParentXmlElement()))
                    tumor.getItems().addAll(createItemsFromLine(lines.get(0), itemDef));
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

            // never trim a group field unless it's completely empty (or we would lose the info of which child value is which)
            if (itemDef.getSubItems().isEmpty() || trimmedValue.isEmpty())
                value = trimmedValue;

            if (!value.isEmpty()) {
                Item item = new Item();
                item.setId(itemDef.getId());
                item.setNum(itemDef.getNumber());
                item.setValue(value);
                items.add(item);

                // handle the sub-items if any (when reading, we add an item for both the parent and the children)
                if (!itemDef.getSubItems().isEmpty()) {
                    for (RuntimeNaaccrDictionaryItem subItemDef : itemDef.getSubItems()) {
                        start = subItemDef.getStartColumn() - 1; // dictionary is 1-based; Java substring is 0-based...
                        end = start + subItemDef.getLength();

                        value = line.substring(start, end).trim();
                        if (!value.isEmpty()) {
                            Item subItem = new Item();
                            subItem.setId(subItemDef.getId());
                            subItem.setNum(subItemDef.getNumber());
                            subItem.setValue(value);
                            items.add(subItem);
                        }
                    }
                }
            }
        }

        return items;
    }

    // TODO remove this testing method
    public static void main(String[] args) throws IOException {
        File inputFile = new File(System.getProperty("user.dir") + "/src/test/resources/data/fake-naaccr14inc-2-rec.txt");
        PatientFlatReader reader = new PatientFlatReader(new FileReader(inputFile), NaaccrFormat.NAACCR_FORMAT_14_INCIDENCE, null);
        int count = 0;
        Patient patient = reader.readPatient();
        while (patient != null) {
            System.out.println("  > num tumors: " + patient.getTumors().size());
            System.out.println("      >> site: " + patient.getTumors().get(0).getItemById("primarySite").getValue());
            System.out.println("      >> morph: " + patient.getTumors().get(0).getItemById("morphologyIcdO3").getValue());
            System.out.println("      >> hist: " + patient.getTumors().get(0).getItemById("histologyIcdO3").getValue());
            System.out.println("      >> beh: " + patient.getTumors().get(0).getItemById("behaviorIcdO3").getValue());
            count++;
            patient = reader.readPatient();
        }
        reader.close();
        System.out.println("num patients: " + count);
    }
}
