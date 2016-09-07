/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.imsweb.naaccrxml.NaaccrValidationError;

/**
 * Corresponds to the "Tumor" element in the XML.
 */
public class Tumor extends AbstractEntity {

    /**
     * This methods returns all the validation errors on the tumor and any of its items.
     * @return collection of validation errors, maybe empty but never null
     */
    public List<NaaccrValidationError> getAllValidationErrors() {
        List<NaaccrValidationError> results = new ArrayList<>(getValidationErrors());
        results.addAll(getItems().stream().filter(item -> item.getValidationError() != null).map(Item::getValidationError).collect(Collectors.toList()));
        return results;
    }
}
