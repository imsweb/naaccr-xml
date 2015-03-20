/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui.pages;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;

import org.naaccr.xml.NaaccrDictionaryUtils;
import org.naaccr.xml.NaaccrFormat;
import org.naaccr.xml.NaaccrStreamObserver;
import org.naaccr.xml.NaaccrValidationError;
import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.gui.Standalone;
import org.naaccr.xml.gui.StandaloneOptions;

public class FlatToXmlPage extends AbstractPage {

    private static final String _PANEL_ID_NO_FILE = "no-file";
    private static final String _PANEL_ID_ANALYSIS = "analysis";
    private static final String _PANEL_ID_ANALYSIS_RESULTS = "analysis-results";
    private static final String _PANEL_ID_OPTIONS = "options";
    private static final String _PANEL_ID_PROCESSING = "processing";
    private static final String _PANEL_ID_ERROR = "error";

    private JFileChooser _fileChooser, _dictionaryFileChooser;
    private CardLayout _northLayout, _centerLayout;
    private JPanel _northPnl, _centerPnl, _northProcessingPnl;
    private JTextField _sourceFld, _targetFld, _dictionaryFld;
    private JProgressBar _analysisBar, _processingBar;
    private JLabel _errorLbl, _formatLbl, _numLinesLbl;
    private SwingWorker<Void, Void> _analysisWorker;
    private SwingWorker<Void, Integer> _processingWorker;
    private JTextArea _textArea;
    private StandaloneOptions _guiOptions;

    public FlatToXmlPage() {
        super("Flat to XML", "Transform a NAACCR Flat file into the corresponding NAACCR XML file.");

        _fileChooser = new JFileChooser();
        _fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _fileChooser.setDialogTitle("Select File");
        _fileChooser.setApproveButtonToolTipText("Select file");
        _fileChooser.setMultiSelectionEnabled(false);

        _dictionaryFileChooser = new JFileChooser();
        _dictionaryFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        _dictionaryFileChooser.setDialogTitle("Select File");
        _dictionaryFileChooser.setApproveButtonToolTipText("Select file");
        _dictionaryFileChooser.setMultiSelectionEnabled(false);
        // TODO FPD add restriction on XML

        JPanel northPnl = new JPanel(new BorderLayout());
        JPanel sourceFilePnl = new JPanel();
        sourceFilePnl.setOpaque(false);
        sourceFilePnl.setBorder(null);
        sourceFilePnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        sourceFilePnl.add(Standalone.createBoldLabel("Source Flat File:"));
        sourceFilePnl.add(Box.createHorizontalStrut(10));
        _sourceFld = new JTextField(75);
        _sourceFld.setBackground(Color.WHITE);
        sourceFilePnl.add(_sourceFld);
        sourceFilePnl.add(Box.createHorizontalStrut(10));
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_fileChooser.showDialog(FlatToXmlPage.this, "Select") == JFileChooser.APPROVE_OPTION) {
                    _sourceFld.setText(_fileChooser.getSelectedFile().getAbsolutePath());
                    performAnalysis();
                }
            }
        });
        sourceFilePnl.add(browseBtn);
        northPnl.add(sourceFilePnl, BorderLayout.NORTH);
        _northPnl = new JPanel();
        _northLayout = new CardLayout();
        _northPnl.setLayout(_northLayout);
        _northPnl.add(_PANEL_ID_NO_FILE, buildNoFileSelectedPanel());
        _northPnl.add(_PANEL_ID_ANALYSIS_RESULTS, buildAnalysResultsPanel());
        northPnl.add(_northPnl, BorderLayout.SOUTH);
        this.add(northPnl, BorderLayout.NORTH);

        _centerPnl = new JPanel();
        _centerLayout = new CardLayout();
        _centerPnl.setLayout(_centerLayout);
        _centerPnl.add(_PANEL_ID_ANALYSIS, buildAnalysisPanel());
        _centerPnl.add(_PANEL_ID_OPTIONS, buildOptionsPanel());
        _centerPnl.add(_PANEL_ID_PROCESSING, buildProcessingPanel());
        _centerPnl.add(_PANEL_ID_ERROR, buildErrorPanel());
        this.add(_centerPnl, BorderLayout.CENTER);
        _centerPnl.setVisible(false);
    }

    private JPanel buildNoFileSelectedPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(10, 10, 0, 0));
        pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        pnl.add(Standalone.createItalicLabel("No file selected. Please use the Browse button to select one."));

        return pnl;
    }

    private JPanel buildAnalysisPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(10, 25, 0, 0));
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));

        JPanel lblPnl = new JPanel();
        lblPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        lblPnl.setBorder(null);
        lblPnl.add(Standalone.createItalicLabel("Analyzing file..."));
        pnl.add(lblPnl);

        JPanel progressPnl = new JPanel();
        progressPnl.setLayout(new BorderLayout());
        progressPnl.setBorder(new EmptyBorder(0, 0, 0, 25));
        _analysisBar = new JProgressBar();
        progressPnl.add(_analysisBar, BorderLayout.CENTER);
        pnl.add(progressPnl);

        JPanel controlsPnl = new JPanel();
        controlsPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        controlsPnl.setBorder(new EmptyBorder(5, 35, 0, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_analysisWorker != null)
                    _analysisWorker.cancel(true);
                _analysisWorker = null;
                _sourceFld.setText(null);
                _northLayout.show(_northPnl, _PANEL_ID_NO_FILE);
            }
        });
        controlsPnl.add(cancelBtn);
        pnl.add(controlsPnl);

        JPanel wrapperPnl = new JPanel(new BorderLayout());
        wrapperPnl.add(pnl, BorderLayout.NORTH);
        return wrapperPnl;
    }

    private JPanel buildAnalysResultsPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(15, 0, 15, 0));
        pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        pnl.add(Standalone.createBoldLabel("File format: "));
        pnl.add(Box.createHorizontalStrut(5));
        _formatLbl = new JLabel(" ");
        pnl.add(_formatLbl);
        pnl.add(Box.createHorizontalStrut(25));
        pnl.add(Standalone.createBoldLabel("Num lines: "));
        pnl.add(Box.createHorizontalStrut(5));
        _numLinesLbl = new JLabel(" ");
        pnl.add(_numLinesLbl);

        return pnl;
    }

    private JPanel buildOptionsPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        
        JPanel targetFieldPnl = new JPanel();
        targetFieldPnl.setOpaque(false);
        targetFieldPnl.setBorder(null);
        targetFieldPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        targetFieldPnl.add(Standalone.createBoldLabel("Target XML File:"));
        targetFieldPnl.add(Box.createHorizontalStrut(10));
        _targetFld = new JTextField(75);
        targetFieldPnl.add(_targetFld);
        targetFieldPnl.add(Box.createHorizontalStrut(10));
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_fileChooser.showDialog(FlatToXmlPage.this, "Select") == JFileChooser.APPROVE_OPTION)
                    _targetFld.setText(_fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        targetFieldPnl.add(browseBtn);
        pnl.add(targetFieldPnl);
        pnl.add(Box.createVerticalStrut(15));
        
        JPanel optionsPnl = new JPanel(new BorderLayout());
        Font font = new JLabel().getFont();
        optionsPnl.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, font.deriveFont(Font.BOLD), Color.BLACK));
        _guiOptions = new StandaloneOptions(true, false, false, true);
        _guiOptions.setBorder(new EmptyBorder(10, 15, 10, 10));
        optionsPnl.add(_guiOptions);
        pnl.add(optionsPnl);

        JPanel dictionaryPnl = new JPanel();
        dictionaryPnl.setBorder(new EmptyBorder(15, 0, 0, 0));
        dictionaryPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        dictionaryPnl.add(Standalone.createBoldLabel("User Dictionary:"));
        dictionaryPnl.add(Box.createHorizontalStrut(10));
        _dictionaryFld = new JTextField(75);
        _dictionaryFld.setBackground(Color.WHITE);
        dictionaryPnl.add(_dictionaryFld);
        dictionaryPnl.add(Box.createHorizontalStrut(10));
        JButton dictionaryBrowseBtn = new JButton("Browse...");
        dictionaryBrowseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_dictionaryFileChooser.showDialog(FlatToXmlPage.this, "Select") == JFileChooser.APPROVE_OPTION)
                    _dictionaryFld.setText(_dictionaryFileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        dictionaryPnl.add(dictionaryBrowseBtn);
        pnl.add(dictionaryPnl);

        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        controlsPnl.setBorder(new EmptyBorder(10, 0, 0, 0));
        controlsPnl.add(Box.createHorizontalStrut(75));
        JButton processBtn = new JButton("Process File");
        processBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performProcessing();
            }
        });
        controlsPnl.add(processBtn);
        pnl.add(controlsPnl);
        
        JPanel wrapperPnl = new JPanel(new BorderLayout());
        wrapperPnl.add(pnl, BorderLayout.NORTH);
        return wrapperPnl;
    }

    private JPanel buildProcessingPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBorder(new EmptyBorder(5, 0, 0, 0));

        _northProcessingPnl = new JPanel();
        _northProcessingPnl.setBorder(new EmptyBorder(0, 25, 25, 0));
        _northProcessingPnl.setLayout(new BoxLayout(_northProcessingPnl, BoxLayout.Y_AXIS));
        JPanel lblPnl = new JPanel();
        lblPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        lblPnl.setBorder(null);
        lblPnl.add(Standalone.createItalicLabel("Processing file..."));
        _northProcessingPnl.add(lblPnl);
        JPanel progressPnl = new JPanel();
        progressPnl.setLayout(new BorderLayout());
        progressPnl.setBorder(new EmptyBorder(0, 0, 0, 25));
        _processingBar = new JProgressBar();
        progressPnl.add(_processingBar, BorderLayout.CENTER);
        _northProcessingPnl.add(progressPnl);
        JPanel controlsPnl = new JPanel();
        controlsPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        controlsPnl.setBorder(new EmptyBorder(5, 35, 0, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_processingWorker != null)
                    _processingWorker.cancel(true);
                _processingWorker = null;
                _northProcessingPnl.setVisible(false);
            }
        });
        controlsPnl.add(cancelBtn);
        _northProcessingPnl.add(controlsPnl);
        pnl.add(_northProcessingPnl, BorderLayout.NORTH);

        JPanel centerPnl = new JPanel(new BorderLayout());
        JPanel textAreaLabelPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        textAreaLabelPnl.add(Standalone.createBoldLabel("Processing Errors"));
        centerPnl.add(textAreaLabelPnl, BorderLayout.NORTH);
        _textArea = new JTextArea();
        _textArea.setBorder(new EmptyBorder(2, 3, 2, 3));
        JScrollPane pane = new JScrollPane(_textArea);
        pane.setBorder(new CompoundBorder(new EmptyBorder(5, 0, 0, 0), new LineBorder(Color.LIGHT_GRAY)));
        centerPnl.add(pane, BorderLayout.CENTER);
        pnl.add(centerPnl, BorderLayout.CENTER);
        
        return pnl;
    }
    
    private JPanel buildErrorPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(10, 10, 0, 0));
        pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        JLabel lbl = Standalone.createBoldLabel("Error: ");
        lbl.setForeground(Color.RED);
        pnl.add(lbl);
        _errorLbl = new JLabel(" ");
        _errorLbl.setForeground(Color.RED);
        pnl.add(_errorLbl);

        return pnl;
    }

    private void performAnalysis() {
        _northPnl.setVisible(false);
        _centerPnl.setVisible(true);
        _centerLayout.show(_centerPnl, _PANEL_ID_ANALYSIS);
        _analysisBar.setMinimum(0);
        _analysisBar.setIndeterminate(true);
        _analysisWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                File file = new File(_sourceFld.getText());
                String format = NaaccrXmlUtils.getFormatFromFlatFile(file);
                if (!NaaccrFormat.isFormatSupported(format))
                    reportError("unknown or unsupported file format");
                else {
                    int numLines = 0;
                    try (LineNumberReader reader = new LineNumberReader(NaaccrXmlUtils.createReader(file))) {
                        String line = reader.readLine();
                        while (line != null) {
                            numLines++;
                            line = reader.readLine();
                        }
                        _formatLbl.setText(NaaccrFormat.getInstance(format).getDisplayName());
                        _numLinesLbl.setText(String.valueOf(numLines));
                        _northPnl.setVisible(true);
                        _northLayout.show(_northPnl, _PANEL_ID_ANALYSIS_RESULTS);
                        _analysisBar.setIndeterminate(false);
                        _centerLayout.show(_centerPnl, _PANEL_ID_OPTIONS);
                        _targetFld.setText(invertFilename(new File(_sourceFld.getText())));
                    }
                    catch (IOException e) {
                        reportError(e.getMessage());
                    }
                }
                _analysisWorker = null;
                return null;
            }
        };
        _analysisWorker.execute();
    }

    private void performProcessing() {
        _northProcessingPnl.setVisible(true);
        _textArea.setText(null);
        _centerLayout.show(_centerPnl, _PANEL_ID_PROCESSING);
        _processingBar.setMinimum(0);
        _processingBar.setMaximum(Integer.valueOf(_numLinesLbl.getText()));
        _processingWorker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                File srcFile = new File(_sourceFld.getText());
                File targetFile = new File(_targetFld.getText());
                
                try {
                    NaaccrDictionary userDictionary = null;
                    if (_dictionaryFld.getText() != null)
                        userDictionary = NaaccrDictionaryUtils.readDictionary(new File(_dictionaryFld.getText()));
                    NaaccrXmlUtils.flatToXml(srcFile, targetFile, _guiOptions.getOptions(), userDictionary, new NaaccrStreamObserver() {
                        @Override
                        public void patientRead(Patient patient) {
                            publish(patient.getTumors().size());
                            if (_textArea.getLineCount() < 10000) {
                                for (NaaccrValidationError error : patient.getAllValidationErrors())
                                    _textArea.append("Line " + error.getLineNumber() + ": " + error.getMessage() + "\n");
                            }
                            else if (!_textArea.getText().endsWith("..."))
                                _textArea.append("...");
                            
                            try {
                                _textArea.setCaretPosition(_textArea.getLineStartOffset(_textArea.getLineCount() - 1));
                            }
                            catch (BadLocationException e) {
                                // ignore this one, we won't scroll to the end...
                            }
                        }

                        @Override
                        public void patientWritten(Patient patient) {
                        }
                    });
                }
                catch (IOException e1) {
                    reportError(e1.getMessage());
                }

                _northProcessingPnl.setVisible(false);
                _processingWorker = null;
                return null;
            }

            @Override
            protected void process(final List<Integer> chunks) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int count = _processingBar.getValue();
                        for (Integer i : chunks)
                            count += i;
                        _processingBar.setValue(count);     
                    }
                });
            }
        };
        _processingWorker.execute();
    }

    private void reportError(String error) {
        _northPnl.setVisible(true);
        _centerPnl.setVisible(false);
        _analysisBar.setIndeterminate(false);
        _errorLbl.setText(error == null ? "null access" : error);
        _northLayout.show(_northPnl, _PANEL_ID_ERROR);
    }
}
