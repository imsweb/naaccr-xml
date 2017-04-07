/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity.dictionary;

/**
 * This class encapsulates a single dictionary grouped item, as provided in the dictionary XML files.
 */
public class NaaccrDictionaryGroupedItem extends NaaccrDictionaryItem {

    private String _contains;

    public String getContains() {
        return _contains;
    }

    public void setContains(String contains) {
        _contains = contains;
    }
}
