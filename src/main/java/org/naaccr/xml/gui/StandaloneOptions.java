/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Collections;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.naaccr.xml.NaaccrOptions;
import org.naaccr.xml.NaaccrXmlUtils;

public class StandaloneOptions extends JPanel {

    private boolean _readFlat, _writeFlat, _readXml, _writeXml;
    
    private JCheckBox _groupTumorBox, _reportMismatchBox, _validateValuesBox, _ignoreUnkItemsBox, _writeNumBox;
    
    public StandaloneOptions(boolean readFlat, boolean writeFlat, boolean readXml, boolean writeXml) {
        _readFlat = readFlat;
        _writeFlat = writeFlat;
        _readXml = readXml;
        _writeXml = writeXml;

        setLayout(new BorderLayout());
        setBorder(null);
        setOpaque(false);

        JPanel contentPnl = new JPanel();
        contentPnl.setLayout(new BoxLayout(contentPnl, BoxLayout.Y_AXIS));
        add(contentPnl, BorderLayout.NORTH);

        if (readFlat) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _groupTumorBox = new JCheckBox(" When reading the tumors, group them by Patient ID Number (Item #20).");
            _groupTumorBox.setSelected(true);
            pnl.add(_groupTumorBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, the tumors will be grouped together, resulting in several tumors per patient."));
            contentPnl.add(addHelpRow("Otherwise the tumors won't be grouped and every patient will contain exactly one tumor."));
            contentPnl.add(addHelpRow("If this option is selected, the lines in the flat file belonging to the same patient are assumed to appear next to each other.", new Color(150, 0, 0)));
            contentPnl.add(Box.createVerticalStrut(15));
        }

        if (readFlat) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _reportMismatchBox = new JCheckBox(" When grouping the tumors, report value mismatch.");
            _reportMismatchBox.setSelected(false);
            pnl.add(_reportMismatchBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, the items of the tumors grouped together, but having different values will be reported as warnings."));
            contentPnl.add(addHelpRow("This option has no effect if the tumors are not grouped."));
            contentPnl.add(Box.createVerticalStrut(15));
        }

        if (readFlat || readXml) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _validateValuesBox = new JCheckBox(" When reading the items, validate their value.");
            _validateValuesBox.setSelected(false);
            pnl.add(_validateValuesBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, each value will be validated against the item's data type defined in the dictionary."));
            contentPnl.add(Box.createVerticalStrut(15));
        }

        if (readXml) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _ignoreUnkItemsBox = new JCheckBox(" When reading the file, ignore unknown items.");
            _ignoreUnkItemsBox.setSelected(true);
            pnl.add(_ignoreUnkItemsBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, unknown items will be ignored. Otherwise a warning will be reported."));
            contentPnl.add(Box.createVerticalStrut(15));
        }

        if (writeXml) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _writeNumBox = new JCheckBox(" When writing the items in the XML file, also include the NAACCR Number.");
            _writeNumBox.setSelected(false);
            pnl.add(_writeNumBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, the NAACCR Numbers will be written to the file in addition to the NAACCR IDs."));
            contentPnl.add(addHelpRow("Otherwise only the NAACCR ID (which ia required) is written as an attribute."));
            contentPnl.add(Box.createVerticalStrut(15));
        }
    }

    private JPanel addHelpRow(String text) {
        return addHelpRow(text, Color.BLACK);
    }
    
    private JPanel addHelpRow(String text, Color color) {
        JPanel helpPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        helpPnl.setBorder(new EmptyBorder(0, 50, 0, 0));
        JLabel lbl = Standalone.createItalicLabel(text);
        lbl.setForeground(color);
        helpPnl.add(lbl);
        return helpPnl;
    }
    
    public NaaccrOptions getOptions() {
        NaaccrOptions options = new NaaccrOptions();

        if (_readFlat) {
            if (_groupTumorBox.isSelected())
                options.setTumorGroupingItems(Collections.singletonList(NaaccrXmlUtils.DEFAULT_TUMOR_GROUPING_ITEM));
            else
                options.setTumorGroupingItems(Collections.<String>emptyList());
        }
        
        if (_readFlat)
            options.setReportLevelMismatch(_reportMismatchBox.isSelected());

        if (_readFlat || _readXml)
            options.setValidateValues(_validateValuesBox.isSelected());

        if (_readXml) {
            if (_ignoreUnkItemsBox.isSelected())
                options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_IGNORE);
            else
                options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_ERROR);
        }

        if (_writeXml)
            options.setWriteItemNumber(_writeNumBox.isSelected());
        
        return options;
    }
}
