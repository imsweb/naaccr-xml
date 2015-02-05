/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity;

import java.util.ArrayList;
import java.util.List;

import org.naaccr.xml.NaaccrValidationError;
import org.naaccr.xml.entity.dictionary.AbstractEntity;

public class Patient extends AbstractEntity {
    
    protected List<Tumor> tumors;

    public List<Tumor> getTumors() {
        if (tumors == null)
            tumors = new ArrayList<>();
        return tumors;
    }
    
    public List<NaaccrValidationError> getAllValidationErrors() {
        List<NaaccrValidationError> results = new ArrayList<>(getValidationErrors());
        for (Tumor tumor : getTumors())
            results.addAll(tumor.getValidationErrors());
        return results;
    }
}
