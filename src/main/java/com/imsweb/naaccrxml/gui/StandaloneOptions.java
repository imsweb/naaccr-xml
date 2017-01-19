/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang3.StringUtils;

import com.imsweb.naaccrxml.NaaccrOptions;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;

public class StandaloneOptions extends JPanel {

    // available "modes"
    private boolean _readFlat, _writeFlat, _readXml, _writeXml;

    // global GUI components
    private JCheckBox _groupTumorBox, _reportMismatchBox, _validateValuesBox, _ignoreUnkItemsBox, _writeNumBox, _applyPaddingBox, _reportValTooLongBox;
    private JTextField _itemListFld;
    private JRadioButton _itemsIncludeBtn, _itemsExcludeBtn;

    /**
     * Constructor.
     */
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
            contentPnl.add(addHelpRow("The few items defined as root-items (like registry ID) but having different values for different patients will also be reported."));
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

        if (writeFlat || writeXml) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _applyPaddingBox = new JCheckBox(" When writing the items, apply padding rules (this might change the actual values being written).");
            _applyPaddingBox.setSelected(false);
            pnl.add(_applyPaddingBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, items defining a padding rule (usually left 0-padded) will have their value modified to honor the definition."));
            contentPnl.add(Box.createVerticalStrut(15));
        }

        if (writeFlat) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            _reportValTooLongBox = new JCheckBox(" When writing the items, report an error if a value is too long.");
            _reportValTooLongBox.setSelected(false);
            pnl.add(_reportValTooLongBox);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("If this option is checked, a warning will be reported if an item in the XML has a value that is too long for the flat file."));
            contentPnl.add(addHelpRow("Otherwise the value will be silently cut-off to the maximum length allowed."));
            contentPnl.add(addHelpRow("Note that this option doesn't affect items that allow unlimited-text; those will be silently cut-off regardless."));
            contentPnl.add(Box.createVerticalStrut(15));
        }

        if (readFlat || readXml || writeFlat || writeXml) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            pnl.add(new JLabel("Exclude/Include following items: "));
            _itemListFld = new JTextField(55);
            pnl.add(_itemListFld);
            pnl.add(Box.createHorizontalStrut(15));
            _itemsExcludeBtn = new JRadioButton("Exclusion list");
            pnl.add(_itemsExcludeBtn);
            pnl.add(Box.createHorizontalStrut(5));
            _itemsIncludeBtn = new JRadioButton("Inclusion list");
            pnl.add(_itemsIncludeBtn);
            ButtonGroup group = new ButtonGroup();
            group.add(_itemsExcludeBtn);
            group.add(_itemsIncludeBtn);
            _itemsExcludeBtn.setSelected(true);
            contentPnl.add(pnl);
            contentPnl.add(Box.createVerticalStrut(3));
            contentPnl.add(addHelpRow("Comma-separated list of items (NAACCR ID or NAACCR Number) to exclude/include when reading or writing the file."));
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

    public NaaccrOptions getOptions(NaaccrDictionary baseDictionary, NaaccrDictionary userDictionary) {
        NaaccrOptions options = new NaaccrOptions();

        // this one is not part of the GUI; I guess at some point it can be
        options.setUseStrictNamespaces(true);

        if (_readFlat) {
            if (_groupTumorBox.isSelected())
                options.setTumorGroupingItems(Collections.singletonList(NaaccrXmlUtils.DEFAULT_TUMOR_GROUPING_ITEM));
            else
                options.setTumorGroupingItems(Collections.<String>emptyList());
        }

        if (_readFlat)
            options.setReportLevelMismatch(_reportMismatchBox.isSelected());

        if (_readFlat || _readXml)
            options.setValidateReadValues(_validateValuesBox.isSelected());

        if (_readXml) {
            if (_ignoreUnkItemsBox.isSelected())
                options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_IGNORE);
            else
                options.setUnknownItemHandling(NaaccrOptions.ITEM_HANDLING_ERROR);
        }

        if (_writeXml)
            options.setWriteItemNumber(_writeNumBox.isSelected());

        if (_writeFlat || _writeXml)
            options.setApplyPaddingRules(_applyPaddingBox.isSelected());

        if (_writeFlat)
            options.setReportValuesTooLong(_reportValTooLongBox.isSelected());

        if (_readFlat || _readXml || _writeFlat || _writeXml) {
            String itemsStr = _itemListFld.getText().replace(" ", "");
            if (!itemsStr.isEmpty()) {

                // build a quick access lookup
                Map<String, String> itemNumToItemId = new HashMap<>();
                if (baseDictionary != null)
                    for (NaaccrDictionaryItem item : baseDictionary.getItems())
                        if (item.getNaaccrNum() != null)
                            itemNumToItemId.put(item.getNaaccrNum().toString(), item.getNaaccrId());
                if (userDictionary != null)
                    for (NaaccrDictionaryItem item : userDictionary.getItems())
                        if (item.getNaaccrNum() != null)
                            itemNumToItemId.put(item.getNaaccrNum().toString(), item.getNaaccrId());

                // gather the items
                List<String> naaccrIds = new ArrayList<>();
                for (String s : StringUtils.split(itemsStr, ','))
                    naaccrIds.add(itemNumToItemId.containsKey(s) ? itemNumToItemId.get(s) : s);

                // set the items on the options
                if (_itemsExcludeBtn.isSelected())
                    options.setItemsToExclude(naaccrIds);
                else
                    options.setItemsToInclude(naaccrIds);
            }
        }

        return options;
    }
}
