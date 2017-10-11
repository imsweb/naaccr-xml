/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.entity.dictionary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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

    /**
     * Returns the NAACCR ID of the items contained in this grouped item.
     * @return the NAACCR ID of the items contained in this grouped item
     */
    public List<String> getContainedItemId() {
        if (StringUtils.isBlank(_contains))
            return Collections.emptyList();
        return Arrays.asList(StringUtils.split(_contains, ','));
    }
}
