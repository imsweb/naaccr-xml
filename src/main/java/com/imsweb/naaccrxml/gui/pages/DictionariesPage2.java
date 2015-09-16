/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Vector;
import java.util.regex.PatternSyntaxException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;
import com.imsweb.naaccrxml.gui.Standalone;

public class DictionariesPage2 extends AbstractPage {

    // current dictionary being displayed
    private NaaccrDictionaryWrapper _currentDictionary;

    // whether there are any modifications
    private boolean _modified = false;

    // global GUI components
    private JButton _createBtn, _openBtn, _saveBtn, _addItemBtn, _removeItemBtn;
    private JTextField _locationFld, _dictionaryUriFld, _versionFld, _descFld;
    private JTable _itemsTbl;
    private DefaultTableModel _itemsModel;
    private TableRowSorter<TableModel> _itemsSorter;

    public DictionariesPage2() {
        super();

        this.setBorder(new MatteBorder(0, 0, 1, 1, Color.GRAY));

        Vector<NaaccrDictionaryWrapper> standardDictionaries = new Vector<>();
        for (String version : NaaccrFormat.getSupportedVersions()) {
            standardDictionaries.add(new NaaccrDictionaryWrapper(NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(version)));
            standardDictionaries.add(new NaaccrDictionaryWrapper(NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(version)));
        }

        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        this.add(controlsPnl, BorderLayout.NORTH);
        controlsPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY), new EmptyBorder(5, 10, 5, 0)));
        controlsPnl.add(Standalone.createBoldLabel("View Standard Dictionaries:"));
        controlsPnl.add(Box.createHorizontalStrut(10));
        final JComboBox selectionBox = new JComboBox<>(standardDictionaries);
        controlsPnl.add(selectionBox);
        controlsPnl.add(Box.createHorizontalStrut(35));
        controlsPnl.add(Standalone.createBoldLabel("Manage Customized Dictionaries:"));
        controlsPnl.add(Box.createHorizontalStrut(10));
        _createBtn = new JButton("Create");
        controlsPnl.add(_createBtn);
        controlsPnl.add(Box.createHorizontalStrut(10));
        _openBtn = new JButton("Open");
        controlsPnl.add(_openBtn);
        controlsPnl.add(Box.createHorizontalStrut(10));
        _saveBtn = new JButton("Save");
        controlsPnl.add(_saveBtn);

        JPanel centerPnl = new JPanel(new BorderLayout());
        this.add(centerPnl, BorderLayout.CENTER);

        JPanel dictAttributesPnl = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(4, 4, 4, 4);
        // location
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_END;
        dictAttributesPnl.add(Standalone.createBoldLabel("Location:"), c);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.LINE_START;
        _locationFld = new JTextField(80);
        dictAttributesPnl.add(_locationFld, c);
        // URI
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_END;
        dictAttributesPnl.add(Standalone.createBoldLabel("Dictionary URI:"), c);
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_START;
        _dictionaryUriFld = new JTextField(55);
        dictAttributesPnl.add(_dictionaryUriFld, c);
        // version
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_START;
        dictAttributesPnl.add(Standalone.createBoldLabel("NAACCR Version:"), c);
        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.LINE_START;
        _versionFld = new JTextField(5);
        dictAttributesPnl.add(_versionFld, c);
        // description
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_END;
        dictAttributesPnl.add(Standalone.createBoldLabel("Description:"), c);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.LINE_START;
        _descFld = new JTextField(80);
        dictAttributesPnl.add(_descFld, c);
        JPanel dictAttributeWrapperPnl = new JPanel(new BorderLayout());
        dictAttributeWrapperPnl.add(dictAttributesPnl, BorderLayout.WEST);
        centerPnl.add(dictAttributeWrapperPnl, BorderLayout.NORTH);

        JPanel tablePnl = new JPanel(new BorderLayout());
        centerPnl.add(tablePnl, BorderLayout.CENTER);
        tablePnl.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel tableControlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        tablePnl.add(tableControlsPnl, BorderLayout.NORTH);
        _addItemBtn = new JButton("   Add Item   ");
        tableControlsPnl.add(_addItemBtn);
        tableControlsPnl.add(Box.createHorizontalStrut(10));
        _removeItemBtn = new JButton("Remove Item(s)");
        tableControlsPnl.add(_removeItemBtn);
        tableControlsPnl.add(Box.createHorizontalStrut(35));
        tableControlsPnl.add(new JLabel("Filter Items:"));
        tableControlsPnl.add(Box.createHorizontalStrut(5));
        final JTextField filterFld = new JTextField(25);
        filterFld.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                try {
                    _itemsSorter.setRowFilter(RowFilter.regexFilter("(?i)" + filterFld.getText(), 0, 1, 2));
                }
                catch (PatternSyntaxException ex) {
                    // ignored
                }
            }
        });
        tableControlsPnl.add(filterFld);

        JPanel tableContentPnl = new JPanel(new BorderLayout());
        tablePnl.add(tableContentPnl, BorderLayout.CENTER);
        tableContentPnl.setBorder(new EmptyBorder(3, 0, 0, 0));
        _itemsModel = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1 || columnIndex == 3 || columnIndex == 4)
                    return Integer.class;
                return String.class;
            }
        };
        _itemsTbl = new JTable(_itemsModel);
        _itemsTbl.setDragEnabled(false);
        _itemsSorter = new TableRowSorter<>((TableModel)_itemsModel);
        _itemsTbl.setRowSorter(_itemsSorter);
        DefaultTableCellRenderer itemsRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComponent comp = (JComponent)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                comp.setBorder(new CompoundBorder(new EmptyBorder(new Insets(1, 1, 1, 1)), getBorder()));
                return comp;
            }
        };
        itemsRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        _itemsTbl.setDefaultRenderer(String.class, itemsRenderer);
        _itemsTbl.setDefaultRenderer(Integer.class, itemsRenderer);
        tableContentPnl.add(new JScrollPane(_itemsTbl), BorderLayout.CENTER);

        selectionBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setDictionary(((NaaccrDictionaryWrapper)selectionBox.getSelectedItem()));
            }
        });

        setDictionary((NaaccrDictionaryWrapper)selectionBox.getSelectedItem());
    }

    private void setDictionary(NaaccrDictionaryWrapper dictionaryWrapper) {
        NaaccrDictionary dictionary = dictionaryWrapper.getDictionary();

        _locationFld.setText("<internal>");
        _dictionaryUriFld.setText(dictionary.getDictionaryUri());
        _versionFld.setText(dictionary.getNaaccrVersion());
        _descFld.setText(dictionary.getDescription());
        _itemsModel.setRowCount(0);

        Vector<String> columns = new Vector<>();
        columns.add("ID");
        columns.add("Num");
        columns.add("Name");
        columns.add("Start Column");
        columns.add("Length");
        columns.add("Record Types");
        columns.add("Parent XML Element");
        columns.add("Data Type");
        columns.add("Validation Regex");
        columns.add("Padding");
        columns.add("Trimming");

        Vector<Vector<Object>> rows = new Vector<>();
        for (NaaccrDictionaryItem item : dictionary.getItems()) {
            Vector<Object> row = new Vector<>();
            row.add(item.getNaaccrId());
            row.add(item.getNaaccrNum());
            row.add(item.getNaaccrName());
            row.add(item.getStartColumn());
            row.add(item.getLength());
            row.add(item.getRecordTypes());
            row.add(item.getParentXmlElement());
            row.add(item.getDataType() == null ? NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT : item.getDataType());
            row.add(item.getRegexValidation());
            row.add(item.getPadding() == null ? NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_BLANK : item.getPadding());
            row.add(item.getTrim() == null ? NaaccrXmlDictionaryUtils.NAACCR_TRIM_ALL : item.getTrim());
            rows.add(row);
        }
        _itemsModel.setDataVector(rows, columns);
        _itemsSorter.setRowFilter(null);

        _currentDictionary = dictionaryWrapper;

        updateControlsState();
    }

    private void updateControlsState() {
        boolean isInternal = _currentDictionary.isInternal();

        _createBtn.setEnabled(true);
        _openBtn.setEnabled(true);
        _saveBtn.setEnabled(_modified);

        _locationFld.setEditable(false);
        _dictionaryUriFld.setEditable(!isInternal);
        _versionFld.setEditable(false);
        _descFld.setEditable(!isInternal);

        _addItemBtn.setEnabled(!isInternal);
        _removeItemBtn.setEnabled(!isInternal && _itemsTbl.getSelectedRowCount() > 0);
    }

    private static class NaaccrDictionaryWrapper implements Comparable<NaaccrDictionary> {

        private NaaccrDictionary _dictionary;

        public NaaccrDictionaryWrapper(NaaccrDictionary dictionary) {
            _dictionary = dictionary;
        }

        public NaaccrDictionary getDictionary() {
            return _dictionary;
        }

        public boolean isInternal() {
            boolean isInternal = false;
            if (NaaccrXmlDictionaryUtils.getBaseDictionaryByUri(_dictionary.getDictionaryUri()) != null)
                isInternal = true;
            if (!isInternal && NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByUri(_dictionary.getDictionaryUri()) != null)
                isInternal = true;
            return isInternal;
        }

        @Override
        public String toString() {
            return _dictionary.getDescription();
        }

        @Override
        public int compareTo(NaaccrDictionary o) {
            return _dictionary.getDescription().compareTo(o.getDescription());
        }
    }
}
