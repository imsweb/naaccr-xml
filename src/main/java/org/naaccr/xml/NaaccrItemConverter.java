/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.lang.reflect.Field;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingReader;
import com.thoughtworks.xstream.io.path.PathTrackingWriter;

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

        // TODO FPD add validation
        
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
        
        // TODO FPD add validation
        
        Item item = new Item();
        String num = reader.getAttribute("naaccrNum");
        if (num != null)
            item.setNum(Integer.valueOf(num));
        item.setId(reader.getAttribute("naaccrId"));
        item.setValue(reader.getValue());
        return item;
    }
    
    private String getParentTag(Object readerOrWriter) {
        // this is a horrible solution; I will try to submit a bug so they provide access to the path tracker!
        if (readerOrWriter instanceof PathTrackingReader || readerOrWriter instanceof PathTrackingWriter) {
            try {
                Field f = PathTrackingReader.class.getDeclaredField("pathTracker");
                f.setAccessible(true);
                PathTracker tracker = (PathTracker)f.get(readerOrWriter);
                String[] path = tracker.getPath().toString().split("/");
                return path[path.length - 2];
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }
}
