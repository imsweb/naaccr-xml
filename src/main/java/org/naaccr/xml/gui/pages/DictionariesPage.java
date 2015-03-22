/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.MatteBorder;

import org.apache.commons.io.IOUtils;

public class DictionariesPage extends AbstractPage {

    // TODO support the different version...
    public DictionariesPage() {
        super();
        this.setBorder(new MatteBorder(0, 0, 1, 1, Color.GRAY));
        
        JTextArea area = new JTextArea();
        try {
            area.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-dictionary-140.xml"), "UTF-8"));
        }
        catch (IOException e) {
            area.setText("Unable to read dictionary...");
        }
        JScrollPane pane = new JScrollPane(area);
        pane.setBorder(null);
        this.add(pane, BorderLayout.CENTER);
    }
    
}
