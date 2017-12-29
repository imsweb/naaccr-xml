/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;
import com.imsweb.naaccrxml.gui.Standalone;

// TODO re-work the toolbar, add "current file", maybe a modified status.
// TODO finish the popup (insert before/after, delete (disabled if only one line left)), setup rules of what options are allowed based on selection
// TODO hook up actions
// TODO add proper validation, maybe show the validation result at the bottom of the table? Or maybe a popup dialog...
// TODO set focus on first cell, editing it
// TODO enter should start editing, not go to next cell
// TODO add proper help for this feature

@SuppressWarnings("FieldCanBeLocal")
public class DictionaryEditorPage extends AbstractPage implements ActionListener {

    private static final String _BLANK_VERSION = "<Any>";

    // global GUI components
    private JTextField _dictionaryUriFld, _descFld;
    private JComboBox<String> _versionBox;
    private JTable _itemsTbl;
    private DefaultTableModel _itemsModel;
    protected JFileChooser _dictionaryFileChooser, _outputFileChooser;

    public DictionaryEditorPage() {
        super();

        this.setBorder(new MatteBorder(0, 0, 1, 1, Color.GRAY));

        // NORTH
        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        this.add(controlsPnl, BorderLayout.NORTH);
        controlsPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY), new EmptyBorder(5, 10, 5, 0)));
        controlsPnl.add(createToolBar());

        // CENTER
        JPanel centerPnl = new JPanel(new BorderLayout());
        this.add(centerPnl, BorderLayout.CENTER);

        // CENTER/NORTH
        JPanel dictAttributesPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        centerPnl.add(dictAttributesPnl, BorderLayout.NORTH);
        dictAttributesPnl.setBorder(new EmptyBorder(10, 10, 0, 10));
        dictAttributesPnl.add(Standalone.createBoldLabel("URI:"));
        dictAttributesPnl.add(Box.createHorizontalStrut(5));
        _dictionaryUriFld = new JTextField(45);
        dictAttributesPnl.add(_dictionaryUriFld);
        dictAttributesPnl.add(Box.createHorizontalStrut(20));
        dictAttributesPnl.add(Standalone.createBoldLabel("Version:"));
        dictAttributesPnl.add(Box.createHorizontalStrut(5));
        Vector<String> versions = new Vector<>();
        versions.add(_BLANK_VERSION);
        NaaccrFormat.getSupportedVersions().stream().sorted(Collections.reverseOrder()).forEach(versions::add);
        _versionBox = new JComboBox<>(versions);
        dictAttributesPnl.add(_versionBox);
        dictAttributesPnl.add(Box.createHorizontalStrut(20));
        dictAttributesPnl.add(Standalone.createBoldLabel("Description:"));
        dictAttributesPnl.add(Box.createHorizontalStrut(5));
        _descFld = new JTextField(40);
        dictAttributesPnl.add(_descFld);

        // CENTER/CENTER
        JPanel tablePnl = new JPanel(new BorderLayout());
        centerPnl.add(tablePnl, BorderLayout.CENTER);
        tablePnl.setBorder(new EmptyBorder(10, 10, 5, 10));
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
        _itemsTbl.setSelectionBackground(new Color(210, 227, 236));
        _itemsTbl.setSelectionForeground(Color.BLACK);
        _itemsTbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu popup = new JPopupMenu("Table Popup");
                    popup.setBorder(new BevelBorder(BevelBorder.RAISED));

                    JMenuItem addRowBeforeItem = new JMenuItem("Insert row before");
                    addRowBeforeItem.setActionCommand("table-add-row-before");
                    addRowBeforeItem.addActionListener(DictionaryEditorPage.this);
                    popup.add(addRowBeforeItem);

                    JMenuItem addRowAfterItem = new JMenuItem("Insert row after");
                    addRowAfterItem.setActionCommand("table-add-row-after");
                    addRowAfterItem.addActionListener(DictionaryEditorPage.this);
                    popup.add(addRowAfterItem);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        populateGuiFromDictionary(createEmptyDictionary());
        _itemsTbl.getColumnModel().getColumn(1).setPreferredWidth(45); // item number
        _itemsTbl.getColumnModel().getColumn(3).setPreferredWidth(45); // start column
        _itemsTbl.getColumnModel().getColumn(4).setPreferredWidth(30); // length
        _itemsTbl.getColumnModel().getColumn(7).setPreferredWidth(40); // data type
        _itemsTbl.getColumnModel().getColumn(9).setPreferredWidth(40); // trim

        JComboBox<String> recordTypeBox = new JComboBox<>();
        recordTypeBox.addItem("A,M,C,I");
        recordTypeBox.addItem("A,M,C");
        recordTypeBox.addItem("A,M");
        _itemsTbl.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(recordTypeBox));

        JComboBox<String> parentElementBox = new JComboBox<>();
        parentElementBox.addItem(NaaccrXmlUtils.NAACCR_XML_TAG_ROOT);
        parentElementBox.addItem(NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT);
        parentElementBox.addItem(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
        _itemsTbl.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(parentElementBox));

        JComboBox<String> dataTypeBox = new JComboBox<>();
        dataTypeBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
        dataTypeBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DIGITS);
        dataTypeBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_ALPHA);
        dataTypeBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_MIXED);
        dataTypeBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_NUMERIC);
        dataTypeBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_DATE);
        _itemsTbl.getColumnModel().getColumn(7).setCellEditor(new DefaultCellEditor(dataTypeBox));

        JComboBox<String> paddingBox = new JComboBox<>();
        paddingBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_BLANK);
        paddingBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_ZERO);
        paddingBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_PADDING_LEFT_BLANK);
        paddingBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_PADDING_LEFT_ZERO);
        _itemsTbl.getColumnModel().getColumn(8).setCellEditor(new DefaultCellEditor(paddingBox));

        JComboBox<String> trimmingBox = new JComboBox<>();
        trimmingBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_TRIM_ALL);
        trimmingBox.addItem(NaaccrXmlDictionaryUtils.NAACCR_TRIM_NONE);
        _itemsTbl.getColumnModel().getColumn(9).setCellEditor(new DefaultCellEditor(trimmingBox));

        JScrollPane tableScrollPane = new JScrollPane(_itemsTbl);
        tableScrollPane.setBorder(null);
        tableContentPnl.add(tableScrollPane, BorderLayout.CENTER);

        // CENTER/SOUTH
        JPanel disclaimerPnl = new JPanel();
        disclaimerPnl.setBorder(new EmptyBorder(0, 10, 5, 0));
        disclaimerPnl.setLayout(new BoxLayout(disclaimerPnl, BoxLayout.Y_AXIS));
        JPanel line1Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 2));
        line1Pnl.add(new JLabel("Disclaimer 1"));
        disclaimerPnl.add(line1Pnl);
        JPanel line2Pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 2));
        line2Pnl.add(new JLabel("Disclaimer 2..."));
        disclaimerPnl.add(line2Pnl);
        centerPnl.add(disclaimerPnl, BorderLayout.SOUTH);

        _dictionaryFileChooser = new JFileChooser();
        _dictionaryFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _dictionaryFileChooser.setDialogTitle("Select File");
        _dictionaryFileChooser.setApproveButtonToolTipText("Select file");
        _dictionaryFileChooser.setMultiSelectionEnabled(false);
        _dictionaryFileChooser.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "XML files (*.xml)";
            }

            @Override
            public boolean accept(File f) {
                return f != null && (f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"));
            }
        });

        _outputFileChooser = new JFileChooser();
        _outputFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _outputFileChooser.setDialogTitle("Select File");
        _outputFileChooser.setApproveButtonToolTipText("Select file");
        _outputFileChooser.setMultiSelectionEnabled(false);
        _outputFileChooser.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "XML files (*.xml)";
            }

            @Override
            public boolean accept(File f) {
                return f != null && (f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"));
            }
        });
    }

    private JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        toolbar.setFloatable(false);
        toolbar.add(createToolbarButton("load", "toolbar-load", "Load user-defined dictionary"));
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(createToolbarSeparation());
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(createToolbarButton("save", "toolbar-save", "Save dictionary into current file"));
        toolbar.add(Box.createHorizontalStrut(2));
        toolbar.add(createToolbarButton("save-as", "toolbar-save-as", "Save dictionary into new file"));
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(createToolbarSeparation());
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(createToolbarButton("validate", "toolbar-validate", "Validate dictionary"));

        return toolbar;
    }

    @SuppressWarnings("ConstantConditions")
    private JButton createToolbarButton(String icon, String action, String tooltip) {
        JButton btn = new JButton();
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setActionCommand(action);
        btn.setToolTipText(tooltip);
        btn.addActionListener(this);
        btn.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("gui/icons/toolbar/editor-" + icon + ".png")));
        btn.setDisabledIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("gui/icons/toolbar/editor-" + icon + "-disable.png")));
        btn.setBorder(new EmptyBorder(3, 3, 3, 3));
        return btn;
    }

    private JPanel createToolbarSeparation() {
        return new JPanel() {
            @Override
            public void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D)graphics;

                Rectangle bounds = getBounds();
                g.setColor(Color.GRAY);
                g.drawLine(bounds.width / 2, 0, bounds.width / 2, bounds.height);
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(bounds.width / 2 + 1, 0, bounds.width / 2 + 1, bounds.height);
            }
        };
    }

    private NaaccrDictionary createEmptyDictionary() {
        NaaccrDictionary dictionary = new NaaccrDictionary();
        dictionary.setDictionaryUri("http://mycompany.com/naaccrxml/my-naaccr-dictionary.xml");
        dictionary.setNaaccrVersion(null);
        dictionary.setDescription("My awesome dictionary");
        return dictionary;
    }

    private void populateGuiFromDictionary(NaaccrDictionary dictionary) {
        _dictionaryUriFld.setText(dictionary.getDictionaryUri());
        if (dictionary.getNaaccrVersion() != null)
            _versionBox.setSelectedItem(dictionary.getNaaccrVersion());
        else
            _versionBox.setSelectedItem(_BLANK_VERSION);
        if (dictionary.getDescription() != null)
            _descFld.setText(dictionary.getDescription());

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
        if (dictionary.getItems().isEmpty()) {
            Vector<Object> row = new Vector<>();
            row.add(null);
            row.add(null);
            row.add(null);
            row.add(null);
            row.add(null);
            row.add("A,M,C,I");
            row.add(NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR);
            row.add(NaaccrXmlDictionaryUtils.NAACCR_DATA_TYPE_TEXT);
            row.add(NaaccrXmlDictionaryUtils.NAACCR_PADDING_RIGHT_BLANK);
            row.add(NaaccrXmlDictionaryUtils.NAACCR_TRIM_ALL);
            rows.add(row);
        }
        else {
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
        }
        _itemsModel.setDataVector(rows, columns);
    }

    private NaaccrDictionary createDictionaryFromGui() {
        NaaccrDictionary dictionary = new NaaccrDictionary();
        dictionary.setDictionaryUri(_dictionaryUriFld.getText().trim());
        if (!_BLANK_VERSION.equals(_versionBox.getSelectedItem()))
            dictionary.setNaaccrVersion((String)_versionBox.getSelectedItem());
        dictionary.setDescription(_descFld.getText().trim().isEmpty() ? null : _descFld.getText().trim());

        for (int i = 0; i < _itemsModel.getRowCount(); i++) {
            NaaccrDictionaryItem item = new NaaccrDictionaryItem();
            item.setNaaccrId((String)_itemsModel.getValueAt(i, 0));
            item.setNaaccrNum((Integer)_itemsModel.getValueAt(i, 1));
            item.setNaaccrName((String)_itemsModel.getValueAt(i, 2));
            item.setStartColumn((Integer)_itemsModel.getValueAt(i, 3));
            item.setLength((Integer)_itemsModel.getValueAt(i, 4));
            item.setRecordTypes((String)_itemsModel.getValueAt(i, 5));
            item.setParentXmlElement((String)_itemsModel.getValueAt(i, 6));
            item.setDataType((String)_itemsModel.getValueAt(i, 7));
            item.setPadding((String)_itemsModel.getValueAt(i, 8));
            item.setTrim((String)_itemsModel.getValueAt(i, 9));
            dictionary.addItem(item);
        }

        return dictionary;
    }

    private void performLoad() {
        if (_dictionaryFileChooser.showDialog(DictionaryEditorPage.this, "Select") == JFileChooser.APPROVE_OPTION) {
            try {
                populateGuiFromDictionary(NaaccrXmlDictionaryUtils.readDictionary(_dictionaryFileChooser.getSelectedFile()));
            }
            catch (IOException e) {
                JOptionPane.showMessageDialog(DictionaryEditorPage.this, "Unable to load dictionary.\r\n\r\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performSave() {

    }

    private void performSaveAs() {
        NaaccrDictionary dictionary = createDictionaryFromGui();

        String naaccrVersion = (String)_versionBox.getSelectedItem();
        if (_BLANK_VERSION.equals(naaccrVersion))
            naaccrVersion = NaaccrFormat.getSupportedVersions().stream().max(String.CASE_INSENSITIVE_ORDER).orElse(null);

        String errorMsg = NaaccrXmlDictionaryUtils.validateUserDictionary(dictionary, naaccrVersion);
        if (errorMsg != null) {
            JOptionPane.showMessageDialog(DictionaryEditorPage.this, "Dictionary is not valid.\r\n\r\nError:\r\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // TODO error if they try to save a "standard" URI

        if (_outputFileChooser.showDialog(DictionaryEditorPage.this, "Select") == JFileChooser.APPROVE_OPTION) {
            try {
                NaaccrXmlDictionaryUtils.writeDictionary(dictionary, _outputFileChooser.getSelectedFile());
                JOptionPane.showMessageDialog(DictionaryEditorPage.this, "File successfully created!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            catch (IOException e) {
                JOptionPane.showMessageDialog(DictionaryEditorPage.this, "Unable to save dictionary.\r\n\r\nError:\r\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performValidate() {

    }

    private void performAddRow(boolean insertBefore) {

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "toolbar-load":
                performLoad();
                break;
            case "toolbar-save":
                performSave();
                break;
            case "toolbar-save-as":
                performSaveAs();
                break;
            case "toolbar-validate":
                performValidate();
                break;
            case "table-add-row-before":
                performAddRow(true);
                break;
            case "table-add-row-after":
                performAddRow(false);
                break;
            default:
                throw new RuntimeException("Unknown action: " + e.getActionCommand());
        }
    }
}
