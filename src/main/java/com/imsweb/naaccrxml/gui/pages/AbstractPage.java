/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractPage extends JPanel {

    public AbstractPage() {
        this.setOpaque(true);
        this.setLayout(new BorderLayout());
        this.setBorder(null);
        this.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 1, Color.GRAY), new EmptyBorder(10, 10, 10, 25)));
    }
    
    protected String invertFilename(File file) {
        String[] name = StringUtils.split(file.getName(), '.');
        if (name.length < 2)
            return null;
        String extension = name[name.length - 1];
        boolean compressed = false;
        if (extension.equalsIgnoreCase("gz")) {
            extension = name[name.length - 2];
            compressed = true;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < (compressed ? name.length - 2 : name.length - 1); i++)
            result.append(name[i]).append(".");
        result.append(extension.equalsIgnoreCase("xml") ? "txt" : "xml");
        if (compressed)
            result.append(".gz");
        return new File(file.getParentFile(), result.toString()).getAbsolutePath();
    }
    
}
