/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui.pages;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.io.IOUtils;

public class SamplesPage extends AbstractPage {

    public SamplesPage() {
        super();

        Vector<String> data = new Vector<>();
        data.add("attributes-both.xml");
        data.add("attributes-id-only.xml");
        data.add("attributes-num-only.xml");
        data.add("items-above-patient-level.xml");
        data.add("state-requestor-items.xml");
        data.add("extensions.xml");
        final JList list = new JList(data);
        list.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        this.add(new JScrollPane(list), BorderLayout.WEST);

        JPanel centerPnl = new JPanel(new BorderLayout());
        final JTextArea area = new JTextArea();
        area.setEditable(true);
        JScrollPane pane = new JScrollPane(area);
        pane.setBorder(null);
        centerPnl.add(pane, BorderLayout.CENTER);
        this.add(centerPnl, BorderLayout.CENTER);

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                String s = (String)list.getSelectedValue();
                try {
                    area.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("examples/" + s), "UTF-8"));
                }
                catch (IOException ex) {
                    area.setText("Unable to read dictionary...");
                }
                area.setCaretPosition(0);
            }
        });

        list.setSelectedIndex(0);
    }
    
}
