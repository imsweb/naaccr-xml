/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.io.IOUtils;
import org.naaccr.xml.NaaccrFormat;
import org.naaccr.xml.NaaccrValidationError;
import org.naaccr.xml.NaaccrValidationException;
import org.naaccr.xml.NaaccrXmlOptions;
import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.PatientXmlReader;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.runtime.RuntimeNaaccrDictionary;

@SuppressWarnings("unchecked")
public class Standalone {

    public static void main(String[] args) {
        
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
                // ignored, the look and feel will be the default Java one...
            }
            UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
            Insets insets = UIManager.getInsets("TabbedPane.tabAreaInsets");
            insets.bottom = 0;
            UIManager.put("TabbedPane.tabAreaInsets", insets);
        }

        final JFrame frame = new StandaloneFrame();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.pack();
                frame.setLocation(200, 200);
                frame.setVisible(true);
            }
        });
    }

    private static class StandaloneFrame extends JFrame {

        private JFileChooser _fileChooser;

        public StandaloneFrame() {
            this.setTitle("NAACCR XML Utility v0.4");
            this.setPreferredSize(new Dimension(1000, 700));
            this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            JPanel contentPnl = new JPanel();
            contentPnl.setOpaque(true);
            contentPnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            contentPnl.setLayout(new BorderLayout());
            contentPnl.setBackground(new Color(180, 191, 211));
            this.getContentPane().setLayout(new BorderLayout());
            this.getContentPane().add(contentPnl, BorderLayout.CENTER);
            
            JPanel northPnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
            northPnl.setOpaque(false);
            JLabel northLbl = new JLabel("This GUI is in a very early phase; feel free to report bugs in the \"Java library beta testing\" basecamp thread.");
            northLbl.setForeground(new Color(150, 0, 0));
            northPnl.add(northLbl);
            contentPnl.add(northPnl, BorderLayout.NORTH);
            
            JTabbedPane pane = new JTabbedPane();
            pane.add("  Transform Flat to XML  ", crateFlatToXmlPanel());
            pane.add("  Transform XML to Flat  ", crateXmlToFlatPanel());
            pane.add("  Validation Proof of Concept  ", crateValidationTestPanel());
            pane.add("  Dictionary (XML Format)  ", crateDictionaryPanel());
            pane.add("  More XML Data Examples  ", crateExamplePanel());
            contentPnl.add(pane, BorderLayout.CENTER);

            _fileChooser = new JFileChooser();
            _fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            _fileChooser.setDialogTitle("Select File");
            _fileChooser.setApproveButtonToolTipText("Select file");
            _fileChooser.setMultiSelectionEnabled(false);
        }
        
        private String invertFilename(File file) {
            String[] name = file.getName().split("\\.");
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

        private JPanel crateFlatToXmlPanel() {
            final JPanel pnl = new JPanel();
            pnl.setOpaque(false);
            pnl.setBorder(null);
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.PAGE_AXIS));

            JPanel row1Pnl = new JPanel();
            row1Pnl.setOpaque(false);
            row1Pnl.setBorder(null);
            row1Pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
            JLabel flatToXmlSourceLbl = new JLabel("Source Flat File:");
            flatToXmlSourceLbl.setFont(flatToXmlSourceLbl.getFont().deriveFont(Font.BOLD));
            row1Pnl.add(flatToXmlSourceLbl);
            final JTextField flatToXmlSourceFld = new JTextField(75);
            row1Pnl.add(flatToXmlSourceFld);
            JButton flatToXmlSourceBtn = new JButton("Browse...");
            row1Pnl.add(flatToXmlSourceBtn);
            pnl.add(row1Pnl);

            JPanel row3Pnl = new JPanel();
            row3Pnl.setOpaque(false);
            row3Pnl.setBorder(null);
            row3Pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
            JLabel flatToXmlFormatLbl = new JLabel("File Format:");
            flatToXmlFormatLbl.setFont(flatToXmlFormatLbl.getFont().deriveFont(Font.BOLD));
            flatToXmlFormatLbl.setPreferredSize(flatToXmlSourceLbl.getPreferredSize());
            flatToXmlFormatLbl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            row3Pnl.add(flatToXmlFormatLbl);
            List<String> supportedFormat = new ArrayList<>(NaaccrFormat.getSupportedFormats());
            supportedFormat.add("< none selected >");
            Collections.sort(supportedFormat);
            final JComboBox flatToXmlFormatBox = new JComboBox(supportedFormat.toArray(new String[supportedFormat.size()]));
            row3Pnl.add(flatToXmlFormatBox);
            row3Pnl.add(new JLabel("  (the format will be automatically set after you select a source file, if possible)"));
            pnl.add(row3Pnl);

            JPanel row2Pnl = new JPanel();
            row2Pnl.setOpaque(false);
            row2Pnl.setBorder(null);
            row2Pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
            JLabel flatToXmlTargetLbl = new JLabel("Target XML File:");
            flatToXmlTargetLbl.setFont(flatToXmlTargetLbl.getFont().deriveFont(Font.BOLD));
            row2Pnl.add(flatToXmlTargetLbl);
            final JTextField flatToXmlTargetFld = new JTextField(75);
            row2Pnl.add(flatToXmlTargetFld);
            JButton flatToXmlTargetBtn = new JButton("Browse...");
            row2Pnl.add(flatToXmlTargetBtn);
            pnl.add(row2Pnl);

            JPanel row5Pnl = new JPanel();
            row5Pnl.setOpaque(false);
            row5Pnl.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            row5Pnl.setLayout(new FlowLayout(FlowLayout.CENTER));
            final JLabel flatToXmlResultLbl = new JLabel("Provide a value for the parameters and click the Process File button...");
            flatToXmlResultLbl.setFont(flatToXmlResultLbl.getFont().deriveFont(Font.ITALIC));
            row5Pnl.add(flatToXmlResultLbl);
            pnl.add(row5Pnl);

            JPanel row4Pnl = new JPanel();
            row4Pnl.setOpaque(false);
            row4Pnl.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            row4Pnl.setLayout(new FlowLayout(FlowLayout.CENTER));
            final JButton flatToXmlProcessBtn = new JButton("Process File");
            row4Pnl.add(flatToXmlProcessBtn);
            pnl.add(row4Pnl);

            flatToXmlSourceBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int returnVal = _fileChooser.showDialog(StandaloneFrame.this, "Select");
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        flatToXmlSourceFld.setText(_fileChooser.getSelectedFile().getAbsolutePath());
                        String format = NaaccrXmlUtils.getFormatFromFlatFile(_fileChooser.getSelectedFile());
                        if (format != null)
                            flatToXmlFormatBox.setSelectedItem(format);
                        flatToXmlTargetFld.setText(invertFilename(_fileChooser.getSelectedFile()));
                        flatToXmlProcessBtn.requestFocusInWindow();
                    }
                }
            });

            flatToXmlTargetBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int returnVal = _fileChooser.showDialog(StandaloneFrame.this, "Select");
                    if (returnVal == JFileChooser.APPROVE_OPTION)
                        flatToXmlTargetFld.setText(_fileChooser.getSelectedFile().getAbsolutePath());
                }
            });

            flatToXmlProcessBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File sourceFile = new File(flatToXmlSourceFld.getText());
                    File targetFile = new File(flatToXmlTargetFld.getText());
                    String format = (String)flatToXmlFormatBox.getSelectedItem();
                    flatToXmlResultLbl.setText("Processing file...");
                    pnl.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        int numRecords = NaaccrXmlUtils.flatToXml(sourceFile, targetFile, format, new NaaccrXmlOptions(), null);
                        flatToXmlResultLbl.setText("Done processing source flat file; processed " + numRecords + " records!");
                    }
                    catch (IOException | NaaccrValidationException ex) {
                        JOptionPane.showMessageDialog(StandaloneFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        flatToXmlResultLbl.setText("Provide a value for the parameters and click the Process File button...");
                    }
                    finally {
                        pnl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

            JPanel wrapperPnl = new JPanel(new BorderLayout());
            wrapperPnl.setOpaque(true);
            wrapperPnl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            wrapperPnl.add(pnl, BorderLayout.NORTH);
            return wrapperPnl;
        }

        private JPanel crateXmlToFlatPanel() {
            final JPanel pnl = new JPanel();
            pnl.setOpaque(false);
            pnl.setBorder(null);
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.PAGE_AXIS));


            JPanel row1Pnl = new JPanel();
            row1Pnl.setOpaque(false);
            row1Pnl.setBorder(null);
            row1Pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
            JLabel xmlToFlatSourceLbl = new JLabel("Source XML File:");
            xmlToFlatSourceLbl.setFont(xmlToFlatSourceLbl.getFont().deriveFont(Font.BOLD));
            row1Pnl.add(xmlToFlatSourceLbl);
            final JTextField xmlToFlatSourceFld = new JTextField(75);
            row1Pnl.add(xmlToFlatSourceFld);
            JButton xmlToFlatSourceBtn = new JButton("Browse...");
            row1Pnl.add(xmlToFlatSourceBtn);
            pnl.add(row1Pnl);

            JPanel row2Pnl = new JPanel();
            row2Pnl.setOpaque(false);
            row2Pnl.setBorder(null);
            row2Pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
            JLabel xmlToFlatFormatLbl = new JLabel("File Format:");
            xmlToFlatFormatLbl.setFont(xmlToFlatFormatLbl.getFont().deriveFont(Font.BOLD));
            xmlToFlatFormatLbl.setPreferredSize(xmlToFlatSourceLbl.getPreferredSize());
            xmlToFlatFormatLbl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            row2Pnl.add(xmlToFlatFormatLbl);
            List<String> supportedFormat = new ArrayList<>(NaaccrFormat.getSupportedFormats());
            supportedFormat.add("< none selected >");
            Collections.sort(supportedFormat);
            final JComboBox xmlToFlatFormatBox = new JComboBox(supportedFormat.toArray(new String[supportedFormat.size()]));
            row2Pnl.add(xmlToFlatFormatBox);
            row2Pnl.add(new JLabel("  (the format will be automatically set after you select a source file, if possible)"));
            pnl.add(row2Pnl);
            
            JPanel row3Pnl = new JPanel();
            row3Pnl.setOpaque(false);
            row3Pnl.setBorder(null);
            row3Pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
            JLabel xmlToFlatTargetLbl = new JLabel("Target Flat File:");
            xmlToFlatTargetLbl.setFont(xmlToFlatTargetLbl.getFont().deriveFont(Font.BOLD));
            row3Pnl.add(xmlToFlatTargetLbl);
            final JTextField xmlToFlatTargetFld = new JTextField(75);
            row3Pnl.add(xmlToFlatTargetFld);
            JButton xmlToFlatTargetBtn = new JButton("Browse...");
            row3Pnl.add(xmlToFlatTargetBtn);
            pnl.add(row3Pnl);

            JPanel row4Pnl = new JPanel();
            row4Pnl.setOpaque(false);
            row4Pnl.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            row4Pnl.setLayout(new FlowLayout(FlowLayout.CENTER));
            final JLabel xmlToFlatResultLbl = new JLabel("Provide a value for the parameters and click the Process File button...");
            xmlToFlatResultLbl.setFont(xmlToFlatResultLbl.getFont().deriveFont(Font.ITALIC));
            row4Pnl.add(xmlToFlatResultLbl);
            pnl.add(row4Pnl);

            JPanel row5Pnl = new JPanel();
            row5Pnl.setOpaque(false);
            row5Pnl.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            row5Pnl.setLayout(new FlowLayout(FlowLayout.CENTER));
            final JButton xmlToFlatProcessBtn = new JButton("Process File");
            row5Pnl.add(xmlToFlatProcessBtn);
            pnl.add(row5Pnl);

            xmlToFlatSourceBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int returnVal = _fileChooser.showDialog(StandaloneFrame.this, "Select");
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        xmlToFlatSourceFld.setText(_fileChooser.getSelectedFile().getAbsolutePath());
                        String format = NaaccrXmlUtils.getFormatFromXmlFile(_fileChooser.getSelectedFile());
                        if (format != null)
                            xmlToFlatFormatBox.setSelectedItem(format);
                        xmlToFlatTargetFld.setText(invertFilename(_fileChooser.getSelectedFile()));
                        xmlToFlatProcessBtn.requestFocusInWindow();
                    }
                }
            });

            xmlToFlatTargetBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int returnVal = _fileChooser.showDialog(StandaloneFrame.this, "Select");
                    if (returnVal == JFileChooser.APPROVE_OPTION)
                        xmlToFlatTargetFld.setText(_fileChooser.getSelectedFile().getAbsolutePath());
                }
            });

            xmlToFlatProcessBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File sourceFile = new File(xmlToFlatSourceFld.getText());
                    File targetFile = new File(xmlToFlatTargetFld.getText());
                    String format = (String)xmlToFlatFormatBox.getSelectedItem();
                    xmlToFlatResultLbl.setText("Processing file...");
                    pnl.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        int numRecords = NaaccrXmlUtils.xmlToFlat(sourceFile, targetFile, format, new NaaccrXmlOptions(), null);
                        xmlToFlatResultLbl.setText("Done processing source XML file; processed " + numRecords + " tumors!");
                    }
                    catch (IOException | NaaccrValidationException ex) {
                        JOptionPane.showMessageDialog(StandaloneFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        xmlToFlatResultLbl.setText("Provide a value for the parameters and click the Process File button...");
                    }
                    finally {
                        pnl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

            JPanel wrapperPnl = new JPanel(new BorderLayout());
            wrapperPnl.setOpaque(true);
            wrapperPnl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            wrapperPnl.add(pnl, BorderLayout.NORTH);
            return wrapperPnl;
        }

        private JPanel crateValidationTestPanel() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setOpaque(true);
            pnl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            JPanel disclaimerPnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
            disclaimerPnl.setOpaque(false);
            disclaimerPnl.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
            JLabel disclaimerLbl = new JLabel("Modify the XML to introduce some errors, and click the Validate button to see the errors.");
            disclaimerLbl.setFont(disclaimerLbl.getFont().deriveFont(Font.BOLD));
            disclaimerPnl.add(disclaimerLbl);
            pnl.add(disclaimerPnl, BorderLayout.NORTH);
            
            JPanel textPnl = new JPanel(new BorderLayout());
            textPnl.setOpaque(false);
            textPnl.setBorder(null);
            final JTextArea textFld = new JTextArea();
            StringWriter writer = new StringWriter();
            try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("testing-template.xml")) {
                IOUtils.copy(inputStream, writer, "UTF-8");
                textFld.setText(writer.toString());
            }
            catch (IOException e) {
                textFld.setText("Unexpected error reading template file...");
            }
            textPnl.add(new JScrollPane(textFld), BorderLayout.CENTER);
            pnl.add(textPnl, BorderLayout.CENTER);
            
            JPanel validationPnl = new JPanel(new BorderLayout());
            validationPnl.setOpaque(false);
            validationPnl.setBorder(null);
            JPanel buttonPnl = new JPanel(new GridBagLayout());
            buttonPnl.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
            JButton validateBtn = new JButton("  Validate  ");
            buttonPnl.add(validateBtn);
            validationPnl.add(buttonPnl, BorderLayout.NORTH);
            final JTextArea errorsFld = new JTextArea();
            errorsFld.setRows(10);
            validationPnl.add(new JScrollPane(errorsFld), BorderLayout.CENTER);
            pnl.add(validationPnl, BorderLayout.SOUTH);

            validateBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    errorsFld.setText(null);
                    String format = NaaccrXmlUtils.getFormatFromXmlReader(new StringReader(textFld.getText()));
                    if (format == null)
                        format = NaaccrFormat.NAACCR_FORMAT_14_ABSTRACT; // TODO this should be an error
                    RuntimeNaaccrDictionary dictionary = new RuntimeNaaccrDictionary(format, NaaccrXmlUtils.getBaseDictionary(), null);
                    try (PatientXmlReader reader = new PatientXmlReader(new StringReader(textFld.getText()), NaaccrXmlUtils.getStandardXStream(dictionary, new NaaccrXmlOptions()))) {
                        do {
                            Patient patient = reader.readPatient();
                            if (patient == null)
                                break;
                            for (NaaccrValidationError error : patient.getAllValidationErrors()) {
                                errorsFld.setForeground(new Color(125, 0, 0));
                                if (!errorsFld.getText().isEmpty())
                                    errorsFld.append("\n");
                                errorsFld.append("Line #" + error.getLineNumber() + ": DATA validation error\n");
                                errorsFld.append("   message: " + (error.getMessage() == null ? "<not available>" : error.getMessage()) + "\n");
                                errorsFld.append("   path: " + (error.getPath() == null ? "<not available>" : error.getPath()) + "\n");
                                errorsFld.append("   item ID: " + (error.getNaaccrId() == null ? "<not available>" : error.getNaaccrId()) + "\n");
                                errorsFld.append("   item #: " + (error.getNaaccrNum() == null ? "<not available>" : error.getNaaccrNum()) + "\n");
                                errorsFld.append("   value: " + (error.getValue() == null ? "<blank>" : error.getValue()) + "\n");
                            }
                        }
                        while (true);
                        if (errorsFld.getText().isEmpty()) {
                            errorsFld.append("Reading completed; found no error.");
                            errorsFld.setForeground(new Color(0, 115, 0));
                        }
                    }
                    catch (NaaccrValidationException error) {
                        errorsFld.setForeground(new Color(125, 0, 0));
                        if (!errorsFld.getText().isEmpty())
                            errorsFld.append("\n");
                        errorsFld.append("Line " + (error.getLineNumber() == null ? "?" : ("#" + error.getLineNumber())) + ": SYNTAX validation error\n");
                        errorsFld.append("   message: " + (error.getMessage() == null ? "<not available>" : error.getMessage()) + "\n");
                        errorsFld.append("   path: " + (error.getPath() == null ? "<not available>" : error.getPath()) + "\n");
                        errorsFld.append("   item ID: <not available>\n");
                        errorsFld.append("   item #: <not available>\n");
                        errorsFld.append("   value: <not available>\n");
                        errorsFld.append("\nReading interrupted because of a syntax error...");
                    }
                    catch (IOException ex) {
                        errorsFld.setForeground(new Color(125, 0, 0));
                        errorsFld.append("Unexpected error:\n");
                        errorsFld.append("      -> " + ex.getMessage() + "\n");
                        errorsFld.append("Reading interrupted...");
                    }
                    errorsFld.setCaretPosition(0);
                }
            });
            
            return pnl;
        }

        private JPanel crateDictionaryPanel() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setOpaque(true);
            pnl.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            JTextArea area = new JTextArea();
            area.setEditable(false);
            try {
                area.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("naaccr-dictionary-140.xml"), "UTF-8"));
            }
            catch (IOException e) {
                area.setText("Unable to read dictionary...");
            }
            JScrollPane pane = new JScrollPane(area);
            pane.setBorder(null);
            pnl.add(pane, BorderLayout.CENTER);
            
            return pnl;
        }
        
        private JPanel crateExamplePanel() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setOpaque(true);
            pnl.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            Vector<String> data = new Vector<>();
            data.add("attributes-both.xml");
            data.add("attributes-id-only.xml");
            data.add("attributes-num-only.xml");
            data.add("items-above-patient-level.xml");
            data.add("order.xml");
            data.add("smaller-attributes.xml");
            data.add("group-demo.xml");
            data.add("state-requestor-items.xml");
            data.add("extensions.xml");
            final JList list = new JList(data);
            list.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            pnl.add(new JScrollPane(list), BorderLayout.WEST);
            
            JPanel centerPnl = new JPanel(new BorderLayout());
            pnl.setOpaque(false);
            final JTextArea area = new JTextArea();
            area.setEditable(true);
            JScrollPane pane = new JScrollPane(area);
            pane.setBorder(null);
            centerPnl.add(pane, BorderLayout.CENTER);
            pnl.add(centerPnl, BorderLayout.CENTER);
            
            list.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    String s = (String)list.getSelectedValue();
                    try {
                        area.setText(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("examples/" + s), "UTF-8"));
                    }
                    catch (IOException ex) {
                        area.setText("Unable to read dictionary...");
                    }
                    area.setCaretPosition(0);
                }
            });

            list.setSelectedIndex(0);
            
            return pnl;
        }
    }
}
