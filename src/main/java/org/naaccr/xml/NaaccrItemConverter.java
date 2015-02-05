/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml;

import java.lang.reflect.Field;

import org.naaccr.xml.entity.Item;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionaryItem;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.Path;
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
        
        Item item = new Item();
        String rawId = reader.getAttribute("naaccrId");
        if (rawId != null) {
            rawId = rawId.trim();
            if (!rawId.isEmpty())
                item.setId(rawId);
        }
        
        // read the number
        String rawNum = reader.getAttribute("naaccrNum");
        if (rawNum != null) {
            rawNum = rawNum.trim();
            if (!rawNum.isEmpty()) {
                try {
                    item.setNum(Integer.valueOf(rawNum));
                }
                catch (NumberFormatException e) {
                    throw createValidationException("invalid 'naaccrNum' attribute value: " + rawNum);
                }
            }
        }
        
        // read the value
        item.setValue(reader.getValue());
        
        // item should have either the naaccrId or the naaccrNum
        if (item.getId() == null && item.getNum() == null)
            throw createValidationException("'naaccrId' and 'naaccrNum' attributes cannot be both missing");
        
        // item should be found
        RuntimeNaaccrDictionaryItem itemDef;
        if (item.getId() != null)
            itemDef = _dictionary.getItemByNaaccrId(item.getId());
        else
            itemDef = _dictionary.getItemByNaaccrNum(item.getNum());
        
        if (itemDef != null) {

            // item should be under the proper patient level
            Path path = getCurrentPath(reader);
            if (path != null) {
                String[] parts = path.toString().split("/");
                String parentTag = parts[parts.length - 2];
                int idx = parentTag.indexOf('[');
                if (idx != -1)
                    parentTag = parentTag.substring(0, idx);
                if (!parentTag.equals(itemDef.getParentXmlElement()))
                    throw createValidationException("invalid parent XML tag; was expecting '" + itemDef.getParentXmlElement() + "' but got '" + parentTag + "'");
            }
            
            // item should be in the proper record type
            if (!itemDef.getRecordTypes().contains(_dictionary.getFormat().getRecordType()))
                throw createValidationException("item '" + itemDef.getNaaccrId() + "' is not allowed for this record type");
            
            // value should be valid
            // TODO FPD use regex for value validation; also need to use the options...
        }
        
        return item;
    }
    
    private ConversionException createValidationException(String message) {
        ConversionException ex = new ConversionException(message);
        ex.add("message", message);
        return ex;
    }
    
    private Path getCurrentPath(Object readerOrWriter) {
        // this is a horrible solution; I will try to submit a bug so they provide access to the path tracker!
        if (readerOrWriter instanceof PathTrackingReader || readerOrWriter instanceof PathTrackingWriter) {
            try {
                Field f = PathTrackingReader.class.getDeclaredField("pathTracker");
                f.setAccessible(true);
                return ((PathTracker)f.get(readerOrWriter)).getPath();
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                return null;
            }
        }
        return null;
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
