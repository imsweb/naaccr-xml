/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.imsweb.naaccrxml.NaaccrValidationError;

/**
 * Corresponds to the "Patient" element in the XML.
 */
public class Patient extends AbstractEntity {

    protected List<Tumor> _tumors;

    public Patient() {
        super();
        _tumors = new ArrayList<>();
    }

    public List<Tumor> getTumors() {
        return Collections.unmodifiableList(_tumors);
    }

    /**
     * This methods returns all the validation errors on the patient, any of its items and any of its tumors.
     * @return collection of validation errors, maybe empty but never null
     */
    public List<NaaccrValidationError> getAllValidationErrors() {
        List<NaaccrValidationError> results = new ArrayList<>(getValidationErrors());
        results.addAll(getItems().stream().filter(item -> item.getValidationError() != null).map(Item::getValidationError).collect(Collectors.toList()));
        for (Tumor tumor : getTumors())
            results.addAll(tumor.getAllValidationErrors());
        return results;
    }

    public void addTumor(Tumor tumor) {
        _tumors.add(tumor);
    }
}
