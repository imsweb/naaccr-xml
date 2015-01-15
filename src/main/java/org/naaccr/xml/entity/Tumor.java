/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity;

import java.util.ArrayList;
import java.util.List;

public class Tumor {
    
    private List<Item> items;

    public List<Item> getItems() {
        if (items == null)
            items = new ArrayList<>();
        return items;
    }
}
