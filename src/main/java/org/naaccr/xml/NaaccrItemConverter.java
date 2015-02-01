/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class NaaccrItemConverter implements Converter {

    private RuntimeNaaccrDictionary _dictionary;

    private NaaccrXmlOptions _options;

    public NaaccrItemConverter(RuntimeNaaccrDictionary dictionary, NaaccrXmlOptions options) {
        _dictionary = dictionary;
        _options = options;
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(Item.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Item item = (Item)source;
        if (item.getNum() != null)
            writer.addAttribute("naaccrNum", item.getNum().toString());
        if (item.getId() != null)
            writer.addAttribute("naaccrId", item.getId());
        if (item.getValue() != null)
            writer.setValue(item.getValue());
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

        reader.moveUp();
        
        Item item = new Item();
        String num = reader.getAttribute("naaccrNum");
        if (num != null)
            item.setNum(Integer.valueOf(num));
        item.setId(reader.getAttribute("naaccrId"));
        item.setValue(reader.getValue());
        return item;
    }
}
