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
import java.util.Collections;
import java.util.HashMap;
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
import org.naaccr.xml.NaaccrDictionaryUtils;
import org.naaccr.xml.NaaccrFormat;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.gui.Standalone;

// TODO FPD way too many things hard-coded here!
public class DictionariesPage extends AbstractPage {

    private JLabel _descLbl;

    public DictionariesPage() {
        super();

        this.setBorder(new MatteBorder(0, 0, 1, 1, Color.GRAY));

        final Map<String, NaaccrDictionary> dictionaries = new HashMap<>();
        for (String version : NaaccrFormat.getSupportedVersions()) {
            NaaccrDictionary dictionary = NaaccrDictionaryUtils.getBaseDictionaryByVersion(version);
            dictionaries.put(dictionary.getDescription(), dictionary);
            NaaccrDictionary userDictionary = NaaccrDictionaryUtils.getDefaultUserDictionaryByVersion(version);
            dictionaries.put(userDictionary.getDescription(), userDictionary);
        }

        JPanel northPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        northPnl.setBorder(new EmptyBorder(10, 10, 0, 0));
        Vector<String> data = new Vector<>(dictionaries.keySet());
        Collections.sort(data);
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
                NaaccrDictionary dictionary = dictionaries.get(s);
                _descLbl.setText(dictionary.getDictionaryUri() + " ");
                try {
                    if (dictionary.getDictionaryUri().contains("gap"))
                        area.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-dictionary-gaps-" + dictionary.getNaaccrVersion() + ".xml"), "UTF-8"));
                    else
                        area.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-dictionary-" + dictionary.getNaaccrVersion() + ".xml"), "UTF-8"));
                }
                catch (IOException ex) {
                    area.setText("Unable to read dictionary...");
                }
                area.setCaretPosition(0);
            }
        });

        NaaccrDictionary dictionary = NaaccrDictionaryUtils.getBaseDictionaryByVersion("140");
        selectionBox.setSelectedItem(dictionary.getDescription());
        _descLbl.setText(dictionary.getDictionaryUri() + " ");
        try {
            area.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-dictionary-140.xml"), "UTF-8"));
        }
        catch (IOException ex) {
            area.setText("Unable to read dictionary...");
        }
        area.setCaretPosition(0);
    }

}
