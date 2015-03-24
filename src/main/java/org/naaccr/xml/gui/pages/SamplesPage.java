/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import org.apache.commons.io.IOUtils;
import org.naaccr.xml.gui.Standalone;

public class SamplesPage extends AbstractPage {
    
    private JLabel _descLbl;

    public SamplesPage() {
        super();

        this.setBorder(new MatteBorder(0, 0, 1, 1, Color.GRAY));

        final Map<String, String> samples = new LinkedHashMap<>();
        samples.put("attributes-id-only.xml", "Only the NAACCR ID's are required in the XML file; they are used to uniquely identify the items.");
        samples.put("attributes-both.xml", "The NAACCR Numbers can also be provided in addition to the NAACCR ID's.");
        samples.put("items-above-patient-level.xml", "Some items are defined only once on the top of the file, outside the 'patient' tags.");
        samples.put("state-requestor-items-old.xml", "The State Requestor item can still be used as a 1,000 character fields as it was in the NAACCR flat files.");
        samples.put("state-requestor-items-new.xml", "A better way to use the State Requestor item is to override its definition in a user-defined dictionary.");
        samples.put("extensions.xml", "Extensions are an advanced feature that allows customized XML to be embedded into the standard syntax.");
        
        JPanel northPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        northPnl.setBorder(new EmptyBorder(10, 10, 0, 0));
        Vector<String> data = new Vector<>(samples.keySet());
        final JComboBox selectionBox = new JComboBox<>(data);
        northPnl.add(selectionBox);
        northPnl.add(Box.createHorizontalStrut(25));
        _descLbl = Standalone.createItalicLabel("");
        northPnl.add(_descLbl);
        this.add(northPnl, BorderLayout.NORTH);

        JPanel centerPnl = new JPanel(new BorderLayout());
        centerPnl.setBorder(new EmptyBorder(10, 0, 0, 0));
        final JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setOpaque(true);
        JScrollPane pane = new JScrollPane(area);
        pane.setBorder(new MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        centerPnl.add(pane, BorderLayout.CENTER);
        this.add(centerPnl, BorderLayout.CENTER);

        selectionBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                String s = (String)selectionBox.getSelectedItem();
                _descLbl.setText(samples.get(s));
                try {
                    area.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("examples/" + s), "UTF-8"));
                }
                catch (IOException ex) {
                    area.setText("Unable to read dictionary...");
                }
                area.setCaretPosition(0);
            }
        });

        String selected = "attributes-id-only.xml";
        selectionBox.setSelectedItem(selected);
        _descLbl.setText(samples.get(selected));
        try {
            area.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("examples/" + selected), "UTF-8"));
        }
        catch (IOException ex) {
            area.setText("Unable to read dictionary...");
        }
        area.setCaretPosition(0);
    }
    
}
