/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

public class SeerList<E> extends JList {

    /**
     * Available filtering mode (contains vs starts with)
     */
    public static final int FILTERING_MODE_EQUALS = 0;
    public static final int FILTERING_MODE_STARTS_WITH = 1;
    public static final int FILTERING_MODE_CONTAINED = 2;

    /**
     * Available display modes
     */
    public static final int DISPLAY_MODE_NONE = 0;
    public static final int DISPLAY_MODE_ALT_COLORS = 1;
    public static final int DISPLAY_MODE_DOTTED_LINES = 2;

    /**
     * Colors
     */
    public static final Color COLOR_LIST_ROW_LBL = new Color(0, 0, 0);
    public static final Color COLOR_LIST_ROW_EVEN = new Color(224, 238, 255);
    public static final Color COLOR_LIST_ROW_ODD = new Color(255, 255, 255);
    public static final Color COLOR_LIST_ROW_SELECTED = new Color(176, 211, 255);
    public static final Color COLOR_LIST_ROW_SELECTED_LBL = new Color(0, 0, 0);

    /**
     * Cached borders
     */
    public static final Border LIST_BORDER_IN = BorderFactory.createLineBorder(Color.BLACK);
    public static final Border LIST_BORDER_OUT = BorderFactory.createLineBorder(Color.GRAY);

    /**
     * Private model
     */
    private SeerListModel _model;

    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    public SeerList(SeerListModel<E> model, int displayStyle, boolean addFocusBorder) {
        _model = model;

        this.setModel(_model);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        if (displayStyle == DISPLAY_MODE_ALT_COLORS) {
            this.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                    if (isSelected) {
                        result.setForeground(COLOR_LIST_ROW_SELECTED_LBL);
                        result.setBackground(COLOR_LIST_ROW_SELECTED);
                    }
                    else {
                        result.setForeground(COLOR_LIST_ROW_LBL);
                        result.setBackground(((index % 2) == 0) ? COLOR_LIST_ROW_ODD : COLOR_LIST_ROW_EVEN);
                    }

                    return result;
                }
            });
        }
        else if (displayStyle == DISPLAY_MODE_DOTTED_LINES) {
            this.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, final int index, boolean isSelected, boolean cellHasFocus) {
                    Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    ((JComponent)comp).setBorder(new LineBorder(new Color(215, 215, 215)) {
                        @Override
                        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                            if (index > 0) {
                                Color oldColor = g.getColor();
                                g.setColor(Color.LIGHT_GRAY);
                                for (int i = 0; i * 10 + 5 < width - 10; i++)
                                    g.drawLine(x + i * 10 + 10, y, x + i * 10 + 15, y);
                                g.setColor(oldColor);
                            }
                        }
                    });
                    return comp;
                }
            });
            this.setSelectionBackground(COLOR_LIST_ROW_SELECTED);
            this.setSelectionForeground(COLOR_LIST_ROW_SELECTED_LBL);
        }
        else if (displayStyle == DISPLAY_MODE_NONE) {
            this.setSelectionBackground(COLOR_LIST_ROW_SELECTED);
            this.setSelectionForeground(COLOR_LIST_ROW_SELECTED_LBL);
        }
        else
            throw new RuntimeException("Unsupported display style: " + displayStyle);

        if (addFocusBorder) {
            this.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    JComponent comp = (JComponent)e.getComponent().getParent().getParent();
                    if (comp instanceof JScrollPane)
                        comp.setBorder(LIST_BORDER_IN);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    JComponent comp = (JComponent)e.getComponent().getParent().getParent();
                    if (comp instanceof JScrollPane)
                        comp.setBorder(LIST_BORDER_OUT);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    public void resetData(List<E> data) {
        _model.resetData(data);
    }

    public void filter(String filter) {
        _model.filter(filter);
    }

    public void resetFilter() {
        _model.filter(null);
    }
}

