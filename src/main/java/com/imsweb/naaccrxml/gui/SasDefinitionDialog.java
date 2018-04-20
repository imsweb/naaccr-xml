/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.apache.commons.io.IOUtils;

import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.NaaccrXmlUtils;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary;
import com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionaryItem;
import com.imsweb.naaccrxml.gui.components.SeerTwoListsSelectionPanel;

public class SasDefinitionDialog extends JDialog {

    public SasDefinitionDialog(JFrame parent, String naaccrVersion, File targetFile) {
        super(parent);

        NaaccrDictionary dictionary = NaaccrXmlDictionaryUtils.getBaseDictionaryByVersion(naaccrVersion);

        this.setTitle("Select Data Items");
        this.setModal(true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(true);
        this.setPreferredSize(new Dimension(900, 600));

        JPanel contentPnl = new JPanel();
        contentPnl.setLayout(new BorderLayout());
        contentPnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(contentPnl, BorderLayout.CENTER);

        //CENTER - selection component
        JPanel centerPnl = new JPanel();
        centerPnl.setLayout(new BorderLayout());
        contentPnl.add(centerPnl, BorderLayout.CENTER);
        JLabel leftLbl = Standalone.createBoldLabel("Available Data Items");
        List<ItemWrapper> leftList = new ArrayList<>();
        for (NaaccrDictionaryItem item : dictionary.getItems())
            leftList.add(new ItemWrapper(item));
        JLabel rightLbl = Standalone.createBoldLabel("Included Data Items");
        List<ItemWrapper> rightList = new ArrayList<>();
        Comparator<ItemWrapper> comp = Comparator.comparing(ItemWrapper::toString);
        SeerTwoListsSelectionPanel<ItemWrapper> selectionPnl = new SeerTwoListsSelectionPanel<>(leftList, rightList, leftLbl, rightLbl, comp, comp);
        centerPnl.add(selectionPnl, BorderLayout.CENTER);

        //SOUTH - controls
        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        controlsPnl.setBorder(new EmptyBorder(25, 0, 0, 0));
        contentPnl.add(controlsPnl, BorderLayout.SOUTH);
        JButton goBtn = new JButton("Create File");
        goBtn.addActionListener(e -> performCreateFile(dictionary, selectionPnl.getRightListContent(), targetFile));
        controlsPnl.add(goBtn);
        JButton cancelBtn = new JButton("  Cancel  ");
        cancelBtn.addActionListener(e -> {
            SasDefinitionDialog.this.setVisible(false);
            SasDefinitionDialog.this.dispose();
        });
        controlsPnl.add(cancelBtn);
    }

    public void performCreateFile(NaaccrDictionary dictionary, List<ItemWrapper> selectedItems, File targetFile) {
        if (selectedItems.isEmpty()) {
            JOptionPane.showMessageDialog(SasDefinitionDialog.this, "You must select at least one data item!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<NaaccrDictionaryItem> items = selectedItems.stream().map(ItemWrapper::getItem).collect(Collectors.toList());

        try (StringReader r = new StringReader(createSasXmlMapper(dictionary, items)); Writer w = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.US_ASCII)) {
            IOUtils.copy(r, w);
        }
        catch (IOException e) {
            String msg = "Unexpected error creating file\n\n" + e.getMessage();
            JOptionPane.showMessageDialog(SasDefinitionDialog.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }

        this.setVisible(false);
        this.dispose();

        JOptionPane.showMessageDialog(SasDefinitionDialog.this, "The file was successfully created!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private String createSasXmlMapper(NaaccrDictionary dict, List<NaaccrDictionaryItem> items) {
        StringBuilder buf = new StringBuilder();

        buf.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>\r\n");
        buf.append("\r\n");
        buf.append("<!-- ############################################################ -->\r\n");
        buf.append("<!-- SAS XML Libname Engine Map -->\r\n");
        buf.append("<!-- Generated by NAACCR XML Java library ").append(Standalone.getVersion()).append(" -->\r\n");
        buf.append("<!-- ############################################################ -->\r\n");
        buf.append("<SXLEMAP description=\"NAACCR XML v").append(dict.getNaaccrVersion()).append(" mapping\" name=\"naaccr_xml_map_").append(dict.getNaaccrVersion()).append("\" version=\"2.1\">\r\n");
        buf.append("\r\n");
        buf.append("    <NAMESPACES count=\"0\"/>\r\n");
        addLevelInfo(items, NaaccrXmlUtils.NAACCR_XML_TAG_ROOT, buf, "NAACCR Data data set", "naaccrdata", "/NaaccrData");
        addLevelInfo(items, NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT, buf, "Patients data set", "patients", "/NaaccrData/Patient");
        addLevelInfo(items, NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR, buf, "Tumors data set", "tumors", "/NaaccrData/Patient/Tumor");
        buf.append("</SXLEMAP>\r\n");

        return buf.toString();
    }

    private void addLevelInfo(List<NaaccrDictionaryItem> items, String level, StringBuilder buf, String desc, String name, String path) {

        buf.append("\r\n");
        buf.append("    <!-- ############################################################ -->\r\n");
        buf.append("    <TABLE description=\"").append(desc).append("\" name=\"").append(name).append("\">\r\n");
        buf.append("        <TABLE-PATH syntax=\"XPath\">").append(path).append("</TABLE-PATH>\r\n");
        buf.append("\r\n");
        buf.append("        <COLUMN class=\"ORDINAL\" name=\"NaaccrDataKey\" retain=\"YES\">\r\n");
        buf.append("            <INCREMENT-PATH beginend=\"BEGIN\" syntax=\"XPath\">/NaaccrData</INCREMENT-PATH>\r\n");
        buf.append("            <TYPE>numeric</TYPE>\r\n");
        buf.append("            <DATATYPE>integer</DATATYPE>\r\n");
        buf.append("        </COLUMN>\r\n");
        if (NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT.equals(level) || NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(level)) {
            buf.append("\r\n");
            buf.append("        <COLUMN class=\"ORDINAL\" name=\"PatientKey\" retain=\"YES\">\r\n");
            buf.append("            <INCREMENT-PATH beginend=\"BEGIN\" syntax=\"XPath\">/NaaccrData/Patient</INCREMENT-PATH>\r\n");
            buf.append("            <TYPE>numeric</TYPE>\r\n");
            buf.append("            <DATATYPE>integer</DATATYPE>\r\n");
            buf.append("        </COLUMN>\r\n");
        }
        if (NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR.equals(level)) {
            buf.append("\r\n");
            buf.append("        <COLUMN class=\"ORDINAL\" name=\"TumorKey\" retain=\"YES\">\r\n");
            buf.append("            <INCREMENT-PATH beginend=\"BEGIN\" syntax=\"XPath\">/NaaccrData/Patient/Tumor</INCREMENT-PATH>\r\n");
            buf.append("            <TYPE>numeric</TYPE>\r\n");
            buf.append("            <DATATYPE>integer</DATATYPE>\r\n");
            buf.append("        </COLUMN>\r\n");
        }

        List<String> included = Arrays.asList("registryId", "nameLast", "primarySite");

        for (NaaccrDictionaryItem item : items) {
            if (!level.equals(item.getParentXmlElement()) || !included.contains(item.getNaaccrId()))
                continue;
            buf.append("\r\n");
            buf.append("        <COLUMN name=\"").append(item.getNaaccrId()).append("\">\r\n");
            buf.append("            <PATH syntax=\"XPath\">").append(createXpath(item)).append("</PATH>\r\n");
            buf.append("            <DESCRIPTION>").append(item.getNaaccrName().replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;"))
                    .append(" [Item #").append(item.getNaaccrNum()).append("]</DESCRIPTION>\r\n");
            buf.append("            <TYPE>character</TYPE>\r\n");
            buf.append("            <DATATYPE>string</DATATYPE>\r\n");
            buf.append("            <LENGTH>").append(item.getLength()).append("</LENGTH>\r\n");
            buf.append("        </COLUMN>\r\n");
        }

        buf.append("    </TABLE>\r\n");
    }

    private String createXpath(NaaccrDictionaryItem item) {
        switch (item.getParentXmlElement()) {
            case NaaccrXmlUtils.NAACCR_XML_TAG_ROOT:
                return "/NaaccrData/Item[@naaccrId=\"" + item.getNaaccrId() + "\"]";
            case NaaccrXmlUtils.NAACCR_XML_TAG_PATIENT:
                return "/NaaccrData/Patient/Item[@naaccrId=\"" + item.getNaaccrId() + "\"]";
            case NaaccrXmlUtils.NAACCR_XML_TAG_TUMOR:
                return "/NaaccrData/Patient/Tumor/Item[@naaccrId=\"" + item.getNaaccrId() + "\"]";
            default:
                throw new RuntimeException("Unsupported parent XML element: " + item.getParentXmlElement());
        }
    }

    private static class ItemWrapper {

        private NaaccrDictionaryItem _item;

        public ItemWrapper(NaaccrDictionaryItem item) {
            _item = item;
        }

        public NaaccrDictionaryItem getItem() {
            return _item;
        }

        @Override
        public String toString() {
            return _item.getNaaccrName() + " [#" + _item.getNaaccrNum() + "]";
        }
    }
}
