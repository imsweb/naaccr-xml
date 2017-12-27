/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.imsweb.naaccrxml.gui.Standalone;

// TODO add new icon for this new page and link it in Standalone...
public class DictionaryEditorPage extends AbstractPage implements ActionListener {

    // global GUI components
    private JTextField _dictionaryUriFld, _versionFld, _descFld;
    private JTable _itemsTbl;
    private DefaultTableModel _itemsModel;
    private TableRowSorter<TableModel> _itemsSorter;

    public DictionaryEditorPage() {
        super();

        this.setBorder(new MatteBorder(0, 0, 1, 1, Color.GRAY));

        // NORTH
        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        this.add(controlsPnl, BorderLayout.NORTH);
        controlsPnl.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY), new EmptyBorder(5, 10, 5, 0)));
        JButton _loadBtn = new JButton("Load");
        controlsPnl.add(_loadBtn);
        controlsPnl.add(Box.createHorizontalStrut(10));

        // CENTER
        JPanel centerPnl = new JPanel(new BorderLayout());
        this.add(centerPnl, BorderLayout.CENTER);

        // CENTER/NORTH
        JPanel dictAttributesPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        centerPnl.add(dictAttributesPnl, BorderLayout.NORTH);
        dictAttributesPnl.setBorder(new EmptyBorder(10, 10, 0, 10));
        dictAttributesPnl.add(Standalone.createBoldLabel("URI:"));
        dictAttributesPnl.add(Box.createHorizontalStrut(5));
        _dictionaryUriFld = new JTextField(50);
        dictAttributesPnl.add(_dictionaryUriFld);
        dictAttributesPnl.add(Box.createHorizontalStrut(20));
        dictAttributesPnl.add(Standalone.createBoldLabel("Version:"));
        dictAttributesPnl.add(Box.createHorizontalStrut(5));
        _versionFld = new JTextField(5);
        dictAttributesPnl.add(_versionFld);
        dictAttributesPnl.add(Box.createHorizontalStrut(20));
        dictAttributesPnl.add(Standalone.createBoldLabel("Description:"));
        dictAttributesPnl.add(Box.createHorizontalStrut(5));
        _descFld = new JTextField(50);
        dictAttributesPnl.add(_descFld);

        // CENTER/CENTER
        JPanel tablePnl = new JPanel(new BorderLayout());
        centerPnl.add(tablePnl, BorderLayout.CENTER);
        tablePnl.setBorder(new EmptyBorder(10, 10, 10, 10));
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
        _itemsTbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu popup = new JPopupMenu("Table Popup");
                    popup.setBorder(new BevelBorder(BevelBorder.RAISED));
                    JMenuItem addRowItem = new JMenuItem("Add row");
                    addRowItem.setActionCommand("add-row");
                    addRowItem.addActionListener(DictionaryEditorPage.this);
                    popup.add(addRowItem);
                    //popup.addSeparator();
                }
            }
        });
        JScrollPane tableScrollPane = new JScrollPane(_itemsTbl);
        tableScrollPane.setBorder(null);
        tableContentPnl.add(tableScrollPane, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "add-row":
                System.out.println("ADD ROW");
                break;
            default:
                throw new RuntimeException("Unknown action: " + e.getActionCommand());
        }
    }
}
