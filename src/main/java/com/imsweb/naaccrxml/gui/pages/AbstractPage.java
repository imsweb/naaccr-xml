/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import org.apache.commons.lang3.StringUtils;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;

public abstract class AbstractPage extends JPanel {

    public AbstractPage() {
        this.setOpaque(true);
        this.setLayout(new BorderLayout());
        this.setBorder(null);
        this.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 1, Color.GRAY), new EmptyBorder(10, 10, 10, 25)));
    }

    protected String invertFilename(File file) {
        String[] name = StringUtils.split(file.getName(), '.');
        if (name.length < 2)
            return null;
        String extension = name[name.length - 1];
        boolean compressed = false;
        if (extension.equalsIgnoreCase("gz")) {
            extension = name[name.length - 2];
            compressed = true;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < (compressed ? name.length - 2 : name.length - 1); i++)
            result.append(name[i]).append(".");
        result.append(extension.equalsIgnoreCase("xml") ? "txt" : "xml");
        if (compressed)
            result.append(".gz");
        return new File(file.getParentFile(), result.toString()).getAbsolutePath();
    }

    protected void performExtractToCsv(NaaccrDictionary dictionary, String filename) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Select Target File");
        fileChooser.setApproveButtonToolTipText("Create CSV");
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), filename));
        if (fileChooser.showDialog(this, "Create CSV") == JFileChooser.APPROVE_OPTION) {
            File targetFile = fileChooser.getSelectedFile();
            if (targetFile.exists()) {
                int result = JOptionPane.showConfirmDialog(this, "Target file already exists, are you sure you want to replace it?", "Confirmation",
                        JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION)
                    return;
            }

            try {
                NaaccrXmlDictionaryUtils.writeDictionaryToCsv(dictionary, targetFile);
                JOptionPane.showMessageDialog(this, "Extract successfully created!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            catch (IOException | RuntimeException e) {
                String msg = "Unexpected error creating CSV file\n\n" + e.getMessage();
                JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
