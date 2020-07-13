/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.swing.JLabel;

@SuppressWarnings("unused")
public class SeerClickableLabel extends JLabel implements MouseListener {

    // the action that needs to be executed when clicked
    private SeerClickableLabelAction _action;

    // the current state of the label
    private boolean _underlined;

    /**
     * Constructor
     * @param label text for the label
     */
    public SeerClickableLabel(String label) {
        this(label, null);
    }

    /**
     * Constructor
     * @param label text for the label
     * @param action action to execute when the lable is clicked
     */
    public SeerClickableLabel(String label, SeerClickableLabelAction action) {
        super(label);

        _action = action;
        _underlined = false;

        this.setForeground(Color.BLUE);

        // this class is it's own listener...
        this.addMouseListener(this);
    }

    /**
     * Sets the action for this clickable label...
     * @param action action to set
     */
    public void setAction(SeerClickableLabelAction action) {
        _action = action;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (isEnabled() && _action != null)
            _action.execute();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (isEnabled()) {
            _underlined = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            repaint();
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (isEnabled()) {
            _underlined = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (isEnabled()) {
            g.setColor(getForeground());
            if (_underlined)
                g.drawLine(0, this.getHeight() - 1, this.getWidth(), this.getHeight() - 1);
        }
    }

    /**
     * Interface used to represent an action that needs to be executed when a label is clicked.
     */
    public interface SeerClickableLabelAction {

        /**
         * Main method for an action that needs to be executed.
         */
        void execute();
    }

    /**
     * Utility method to create an action that browse to a given internet address.
     * @param url internet address
     * @return corresponding action
     */
    public static SeerClickableLabelAction createBrowseToUrlAction(final String url) {
        return () -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI.create(url));
                }
                catch (IOException | RuntimeException e) {
                    // ignored
                }
            }
        };
    }

    /**
     * Utility method to create an action that opened a given file.
     * @param path path to the file
     * @return corresponding action
     */
    public static SeerClickableLabelAction createOpenFileAction(final String path) {
        return () -> {
            File file = new File(path);
            try {
                Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(Action.OPEN))
                    Desktop.getDesktop().open(file);
            }
            catch (IOException | RuntimeException e) {
                // ignored
            }
        };
    }

    /**
     * Utility method to create an action that opened a given file.
     * @param path path to the file
     * @return corresponding action
     */
    public static SeerClickableLabelAction createOpenParentFolderAction(final String path) {
        return () -> {
            File file = new File(path);
            try {
                if (file.exists() && System.getProperty("os.name").startsWith("Windows"))
                    Runtime.getRuntime().exec("Explorer /select," + file.getParentFile().getAbsolutePath() + "\\" + file.getName());
                else {
                    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                    if (desktop != null && desktop.isSupported(Action.OPEN))
                        Desktop.getDesktop().open(file.getParentFile());
                }
            }
            catch (IOException | RuntimeException e) {
                // ignored
            }
        };
    }
}

