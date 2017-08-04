/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Vector;
import java.util.regex.PatternSyntaxException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.io.IOUtils;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;
import com.imsweb.naaccrxml.gui.Standalone;

@SuppressWarnings("FieldCanBeLocal")
public class DictionariesPage extends AbstractPage {

    // global GUI components
    private JLabel _dictionaryUriFld, _versionFld, _descFld;
    private JTextArea _xmlArea;
    private JTextField _filterFld;
    private JTable _itemsTbl;
    private DefaultTableModel _itemsModel;
    private TableRowSorter<TableModel> _itemsSorter;

    public DictionariesPage() {
        super();

        this.setBorder(new MatteBorder(0, 0, 1, 1, Color.GRAY));
        this.setPreferredSize(new Dimension(500, 300)); // need this because some dictionaries define pretty long lines now...

        Vector<NaaccrDictionaryWrapper> standardDictionaries = new Vector<>();
        for (String version : NaaccrFormat.getSupportedVersions()) {
            standardDictionaries.add(new NaaccrDictionaryWrapper(NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(version), true));
            standardDictionaries.add(new NaaccrDictionaryWrapper(NaaccrXmlDictionaryUtils.getDefaultUserDictionaryByVersion(version), false));
        }
        standardDictionaries.sort((o1, o2) -> o2.getDictionary().getNaaccrVersion().compareTo(o1.getDictionary().getNaaccrVersion()));

        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        this.add(controlsPnl, BorderLayout.NORTH);
        controlsPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY), new EmptyBorder(5, 10, 5, 0)));
        controlsPnl.add(Standalone.createBoldLabel("Standard Dictionaries:"));
        controlsPnl.add(Box.createHorizontalStrut(10));
        final JComboBox selectionBox = new JComboBox<>(standardDictionaries);
        controlsPnl.add(selectionBox);
        controlsPnl.add(Box.createHorizontalStrut(25));
        JButton extractBtn = new JButton("Extract to CSV...");
        extractBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Select Target File");
            fileChooser.setApproveButtonToolTipText("Create CSV");
            fileChooser.setMultiSelectionEnabled(false);
            NaaccrDictionaryWrapper dictionary = (NaaccrDictionaryWrapper)selectionBox.getSelectedItem();
            @SuppressWarnings("ConstantConditions")
            String prefix = dictionary.isBase() ? "naaccr-dictionary-" : "user-defined-naaccr-dictionary-";
            fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), prefix + dictionary.getDictionary().getNaaccrVersion() + ".csv"));
            if (fileChooser.showDialog(DictionariesPage.this, "Create CSV") == JFileChooser.APPROVE_OPTION) {
                File targetFile = fileChooser.getSelectedFile();
                if (targetFile.exists()) {
                    int result = JOptionPane.showConfirmDialog(DictionariesPage.this, "Target file already exists, are you sure you want to replace it?", "Confirmation",
                            JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION)
                        return;
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
                    writer.write("NAACCR XML ID,NAACCR Number,Name,Start Column,Length,Record Types,Parent XML Element,Data Type");
                    writer.newLine();
                    dictionary.getDictionary().getItems().stream()
                            .sorted(Comparator.comparing(NaaccrDictionaryItem::getNaaccrId))
                            .forEach(item -> {
                                try {
                                    writer.write(item.getNaaccrId());
                                    writer.write(",");
                                    writer.write(item.getNaaccrNum() == null ? "" : item.getNaaccrNum().toString());
                                    writer.write(",\"");
                                    writer.write(item.getNaaccrName() == null ? "" : item.getNaaccrName());
                                    writer.write("\",");
                                    writer.write(item.getStartColumn() == null ? "" : item.getStartColumn().toString());
                                    writer.write(",");
                                    writer.write(item.getLength().toString());
                                    writer.write(",\"");
                                    writer.write(item.getRecordTypes() == null ? "" : item.getRecordTypes());
                                    writer.write("\",");
                                    writer.write(item.getParentXmlElement() == null ? "" : item.getParentXmlElement());
                                    writer.write(",");
                                    writer.write(item.getDataType() == null ? NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT : item.getDataType());
                                    writer.newLine();
                                }
                                catch (IOException | RuntimeException ex1) {
                                    throw new RuntimeException(ex1); // doing that to make sure the loop is broken...
                                }
                            });
                }
                catch (IOException | RuntimeException ex2) {
                    String msg = "Unexpected error creating CSV file\n\n" + ex2.getMessage();
                    JOptionPane.showMessageDialog(DictionariesPage.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
                JOptionPane.showMessageDialog(DictionariesPage.this, "Extract successfully created!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        controlsPnl.add(extractBtn);

        JPanel centerPnl = new JPanel(new BorderLayout());
        this.add(centerPnl, BorderLayout.CENTER);

        JPanel dictAttributesPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        dictAttributesPnl.setBorder(new EmptyBorder(10, 10, 0, 10));
        dictAttributesPnl.add(Standalone.createBoldLabel("URI:"));
        dictAttributesPnl.add(Box.createHorizontalStrut(5));
        _dictionaryUriFld = new JLabel(" ");
        dictAttributesPnl.add(_dictionaryUriFld);
        dictAttributesPnl.add(Box.createHorizontalStrut(20));
        dictAttributesPnl.add(Standalone.createBoldLabel("Version:"));
        dictAttributesPnl.add(Box.createHorizontalStrut(5));
        _versionFld = new JLabel(" ");
        dictAttributesPnl.add(_versionFld);
        dictAttributesPnl.add(Box.createHorizontalStrut(20));
        dictAttributesPnl.add(Standalone.createBoldLabel("Description:"));
        dictAttributesPnl.add(Box.createHorizontalStrut(5));
        _descFld = new JLabel(" ");
        dictAttributesPnl.add(_descFld);
        centerPnl.add(dictAttributesPnl, BorderLayout.NORTH);

        JPanel fieldsPnl = new JPanel(new BorderLayout());
        centerPnl.add(fieldsPnl, BorderLayout.CENTER);
        fieldsPnl.setBorder(new EmptyBorder(10, 10, 10, 10));
        JTabbedPane pane = new JTabbedPane();
        fieldsPnl.add(pane, BorderLayout.CENTER);

        JPanel xmlPnl = new JPanel(new BorderLayout());
        pane.addTab("View items in XML", xmlPnl);
        _xmlArea = new JTextArea();
        _xmlArea.setEditable(false);
        _xmlArea.setRows(25);
        _xmlArea.setOpaque(true);
        JScrollPane xmlScrollPane = new JScrollPane(_xmlArea);
        xmlScrollPane.setBorder(new MatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
        xmlPnl.add(xmlScrollPane, BorderLayout.CENTER);

        JPanel tablePnl = new JPanel(new BorderLayout());
        tablePnl.setBorder(new LineBorder(Color.LIGHT_GRAY));
        pane.addTab("View items in a table", tablePnl);

        JPanel tableContentPnl = new JPanel(new BorderLayout());
        tablePnl.add(tableContentPnl, BorderLayout.CENTER);
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
        _itemsSorter = new TableRowSorter<>(_itemsModel);
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
        JScrollPane tableScrollPane = new JScrollPane(_itemsTbl);
        tableScrollPane.setBorder(null);
        tableContentPnl.add(tableScrollPane, BorderLayout.CENTER);

        JPanel tableControlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        tableControlsPnl.setBorder(new EmptyBorder(10, 10, 10, 10));
        tablePnl.add(tableControlsPnl, BorderLayout.SOUTH);
        tableControlsPnl.add(new JLabel("Filter Items:"));
        tableControlsPnl.add(Box.createHorizontalStrut(5));
        _filterFld = new JTextField(25);
        _filterFld.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                try {
                    _itemsSorter.setRowFilter(RowFilter.regexFilter("(?i)" + _filterFld.getText(), 0, 1, 2));
                }
                catch (PatternSyntaxException ex) {
                    // ignored
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                try {
                    _itemsSorter.setRowFilter(RowFilter.regexFilter("(?i)" + _filterFld.getText(), 0, 1, 2));
                }
                catch (PatternSyntaxException ex) {
                    // ignored
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        tableControlsPnl.add(_filterFld);
        tableControlsPnl.add(Box.createHorizontalStrut(10));
        tableControlsPnl.add(new JLabel("(only the ID, Num and Name columns are searched)"));

        selectionBox.addItemListener(e -> setDictionary(((NaaccrDictionaryWrapper)selectionBox.getSelectedItem())));

        setDictionary((NaaccrDictionaryWrapper)selectionBox.getSelectedItem());
    }

    private void setDictionary(NaaccrDictionaryWrapper dictionaryWrapper) {
        NaaccrDictionary dictionary = dictionaryWrapper.getDictionary();

        _dictionaryUriFld.setText(dictionary.getDictionaryUri());
        _versionFld.setText(dictionary.getNaaccrVersion());
        _descFld.setText(dictionary.getDescription());
        _itemsModel.setRowCount(0);

        Vector<String> columns = new Vector<>();
        columns.add("ID");
        columns.add("Num");
        columns.add("Name");
        columns.add("Start Col");
        columns.add("Length");
        columns.add("Record Types");
        columns.add("Parent XML Element");
        columns.add("Data Type");
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
            row.add(item.getPadding() == null ? NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_BLANK : item.getPadding());
            row.add(item.getTrim() == null ? NaaccrXmlDictionaryUtils.NAACCR_TRIM_ALL : item.getTrim());
            rows.add(row);
        }
        _itemsModel.setDataVector(rows, columns);
        _itemsSorter.setRowFilter(null);
        _filterFld.setText("");

        _itemsTbl.getColumnModel().getColumn(1).setPreferredWidth(45); // item number
        _itemsTbl.getColumnModel().getColumn(3).setPreferredWidth(45); // start column
        _itemsTbl.getColumnModel().getColumn(4).setPreferredWidth(30); // length
        _itemsTbl.getColumnModel().getColumn(7).setPreferredWidth(40); // data type
        _itemsTbl.getColumnModel().getColumn(9).setPreferredWidth(40); // trim

        try {
            if (dictionary.getDictionaryUri().contains("user-defined"))
                _xmlArea.setText(
                        IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("user-defined-naaccr-dictionary-" + dictionary.getNaaccrVersion() + ".xml"), "UTF-8"));
            else
                _xmlArea.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-dictionary-" + dictionary.getNaaccrVersion() + ".xml"), "UTF-8"));
        }
        catch (IOException ex) {
            _xmlArea.setText("Unable to read dictionary...");
        }
        _xmlArea.setCaretPosition(0);
    }

    private static class NaaccrDictionaryWrapper {

        private NaaccrDictionary _dictionary;

        private boolean _isBase;

        public NaaccrDictionaryWrapper(NaaccrDictionary dictionary, boolean isBase) {
            _dictionary = dictionary;
            _isBase = isBase;
        }

        public NaaccrDictionary getDictionary() {
            return _dictionary;
        }

        public boolean isBase() {
            return _isBase;
        }

        @Override
        public String toString() {
            return _dictionary.getDescription();
        }
    }
}
