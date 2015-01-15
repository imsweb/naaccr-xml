/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.entity;

import java.util.ArrayList;
import java.util.List;

public class Patient {
    
    private List<Item> items;
    
    private List<Tumor> tumors;

    public List<Item> getItems() {
        if (items == null)
            items = new ArrayList<>();
        return items;
    }

    public List<Tumor> getTumors() {
        if (tumors == null)
            tumors = new ArrayList<>();
        return tumors;
    }
}
