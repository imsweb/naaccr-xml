/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.imsweb.naaccrxml.NaaccrValidationError;

public class Patient extends AbstractEntity {
    
    protected List<Tumor> _tumors;

    public Patient() {
        super();
        _tumors = new ArrayList<>();
    }
    
    public List<Tumor> getTumors() {
        return Collections.unmodifiableList(_tumors);
    }
    
    public List<NaaccrValidationError> getAllValidationErrors() {
        List<NaaccrValidationError> results = new ArrayList<>(getValidationErrors());
        for (Tumor tumor : getTumors())
            results.addAll(tumor.getValidationErrors());
        return results;
    }
    
    public void addTumor(Tumor tumor) {
        _tumors.add(tumor);
    }
}
