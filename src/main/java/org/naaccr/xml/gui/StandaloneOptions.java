/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collections;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.naaccr.xml.NaaccrXmlOptions;
import org.naaccr.xml.NaaccrXmlUtils;

public class StandaloneOptions extends JPanel {

    private boolean _readFlat, _writeFlat, _readXml, _writeXml;
    
    private JCheckBox _groupTumorBox, _reportMissmatchBox, _validateValuesBox, _ignoreUnkItemsBox, _writeNumBox;
    
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
            contentPnl.add(Box.createVerticalStrut(15));
        }

        if (readFlat) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _reportMissmatchBox = new JCheckBox(" When grouping the tumors, report values missmatch.");
            _reportMissmatchBox.setSelected(true);
            pnl.add(_reportMissmatchBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, the items of the tumors grouped together but having different values will be reported as errors."));
            contentPnl.add(addHelpRow("This option has no effet if the tumor are not grouped."));
            contentPnl.add(Box.createVerticalStrut(15));
        }

        if (readFlat || readXml) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _validateValuesBox = new JCheckBox(" When reading the items, validate their value.");
            _validateValuesBox.setSelected(false);
            pnl.add(_validateValuesBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, each value will be validated againts the item's data type defined in the dictionary."));
            contentPnl.add(Box.createVerticalStrut(15));
        }

        if (readXml) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _ignoreUnkItemsBox = new JCheckBox(" When reading the file, ignore unknown items.");
            _ignoreUnkItemsBox.setSelected(true);
            pnl.add(_ignoreUnkItemsBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, unknown items will be ignored. Otherwise an error will be reported."));
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
            contentPnl.add(addHelpRow("Otherwise only the NAACCR ID (which are required) will be written as attributes."));
            contentPnl.add(Box.createVerticalStrut(15));
        }
    }

    private JPanel addHelpRow(String text) {
        JPanel helpPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        helpPnl.setBorder(new EmptyBorder(0, 50, 0, 0));
        helpPnl.add(Standalone.createItalicLabel(text));
        return helpPnl;
    }
    
    public NaaccrXmlOptions getOptions() {
        NaaccrXmlOptions options = new NaaccrXmlOptions();

        if (_readFlat) {
            if (_groupTumorBox.isSelected())
                options.setTumorGroupingItems(Collections.singletonList(NaaccrXmlUtils.DEFAULT_TUMOR_GROUPING_ITEM));
            else
                options.setTumorGroupingItems(Collections.<String>emptyList());
        }
        
        if (_readFlat)
            options.setReportLevelMismatch(_reportMissmatchBox.isSelected());

        if (_readFlat || _readXml)
            options.setValidateValues(_validateValuesBox.isSelected());

        if (_readXml) {
            if (_ignoreUnkItemsBox.isSelected())
                options.setUnknownItemHandling(NaaccrXmlOptions.ITEM_HANDLING_IGNORE);
            else
                options.setUnknownItemHandling(NaaccrXmlOptions.ITEM_HANDLING_ERROR);
        }

        if (_writeXml)
            options.setWriteItemNumber(_writeNumBox.isSelected());
        
        return options;
    }
}