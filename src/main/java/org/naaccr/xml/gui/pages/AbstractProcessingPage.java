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
import java.io.LineNumberReader;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

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
import javax.swing.filechooser.FileFilter;

import org.naaccr.xml.NaaccrDictionaryUtils;
import org.naaccr.xml.NaaccrFormat;
import org.naaccr.xml.NaaccrIOException;
import org.naaccr.xml.NaaccrStreamObserver;
import org.naaccr.xml.NaaccrValidationError;
import org.naaccr.xml.NaaccrXmlOptions;
import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.gui.Standalone;
import org.naaccr.xml.gui.StandaloneOptions;

public abstract class AbstractProcessingPage extends AbstractPage {

    protected static final String _PANEL_ID_NO_FILE = "no-file";
    protected static final String _PANEL_ID_ANALYSIS = "analysis";
    protected static final String _PANEL_ID_ANALYSIS_RESULTS = "analysis-results";
    protected static final String _PANEL_ID_OPTIONS = "options";
    protected static final String _PANEL_ID_PROCESSING = "processing";
    protected static final String _PANEL_ID_ERROR = "error";

    protected JFileChooser _fileChooser, _dictionaryFileChooser;
    protected CardLayout _northLayout, _centerLayout;
    protected JPanel _northPnl, _centerPnl, _northProcessingPnl;
    protected JTextField _sourceFld, _targetFld, _dictionaryFld;
    protected JProgressBar _analysisBar, _processingBar;
    protected JLabel _errorLbl, _formatLbl, _numLinesLbl, _fileSizeLbl, _processingErrorsLbl;
    protected transient SwingWorker<Void, Void> _analysisWorker;
    protected transient SwingWorker<Void, Patient> _processingWorker;
    protected JTextArea _textArea;
    protected StandaloneOptions _guiOptions;

    public AbstractProcessingPage() {
        super();

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
        _dictionaryFileChooser.addChoosableFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "XML files (*.xml)";
            }

            @Override
            public boolean accept(File f) {
                return f != null && (f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"));
            }
        });

        JPanel northPnl = new JPanel(new BorderLayout());
        JPanel sourceFilePnl = new JPanel();
        sourceFilePnl.setOpaque(false);
        sourceFilePnl.setBorder(null);
        sourceFilePnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        sourceFilePnl.add(Standalone.createBoldLabel(getSourceLabelText()));
        sourceFilePnl.add(Box.createHorizontalStrut(10));
        _sourceFld = new JTextField(60);
        _sourceFld.setBackground(Color.WHITE);
        sourceFilePnl.add(_sourceFld);
        sourceFilePnl.add(Box.createHorizontalStrut(10));
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_fileChooser.showDialog(AbstractProcessingPage.this, "Select") == JFileChooser.APPROVE_OPTION) {
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
    
    protected abstract String getSourceLabelText();

    protected abstract String getTargetLabelText();
    
    private JPanel buildNoFileSelectedPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(10, 10, 0, 0));
        pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        pnl.add(Standalone.createItalicLabel("No file selected; please use the Browse button to select one. The file can be uncompressed or GZipped."));

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
        pnl.add(Standalone.createBoldLabel("Numbe of lines: "));
        pnl.add(Box.createHorizontalStrut(5));
        _numLinesLbl = new JLabel(" ");
        pnl.add(_numLinesLbl);
        pnl.add(Box.createHorizontalStrut(25));
        pnl.add(Standalone.createBoldLabel("File size: "));
        pnl.add(Box.createHorizontalStrut(5));
        _fileSizeLbl = new JLabel(" ");
        pnl.add(_fileSizeLbl);

        return pnl;
    }

    private JPanel buildOptionsPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));

        JPanel targetFieldPnl = new JPanel();
        targetFieldPnl.setOpaque(false);
        targetFieldPnl.setBorder(null);
        targetFieldPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        targetFieldPnl.add(Standalone.createBoldLabel(getTargetLabelText()));
        targetFieldPnl.add(Box.createHorizontalStrut(10));
        _targetFld = new JTextField(60);
        targetFieldPnl.add(_targetFld);
        targetFieldPnl.add(Box.createHorizontalStrut(10));
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_fileChooser.showDialog(AbstractProcessingPage.this, "Select") == JFileChooser.APPROVE_OPTION)
                    _targetFld.setText(_fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        targetFieldPnl.add(browseBtn);
        pnl.add(targetFieldPnl);
        pnl.add(Box.createVerticalStrut(15));

        JPanel optionsPnl = new JPanel(new BorderLayout());
        Font font = new JLabel().getFont();
        optionsPnl.setBorder(new TitledBorder(null, "Processing Options", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, font.deriveFont(Font.BOLD), Color.BLACK));
        _guiOptions = createOptions();
        _guiOptions.setBorder(new EmptyBorder(10, 20, 10, 10));
        optionsPnl.add(_guiOptions);
        pnl.add(optionsPnl);

        JPanel dictionaryPnl = new JPanel();
        dictionaryPnl.setBorder(new EmptyBorder(15, 0, 0, 0));
        dictionaryPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        dictionaryPnl.add(Standalone.createBoldLabel("User Dictionary:"));
        dictionaryPnl.add(Box.createHorizontalStrut(10));
        _dictionaryFld = new JTextField(60);
        _dictionaryFld.setBackground(Color.WHITE);
        dictionaryPnl.add(_dictionaryFld);
        dictionaryPnl.add(Box.createHorizontalStrut(10));
        JButton dictionaryBrowseBtn = new JButton("Browse...");
        dictionaryBrowseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_dictionaryFileChooser.showDialog(AbstractProcessingPage.this, "Select") == JFileChooser.APPROVE_OPTION)
                    _dictionaryFld.setText(_dictionaryFileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        dictionaryPnl.add(dictionaryBrowseBtn);
        pnl.add(dictionaryPnl);

        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        controlsPnl.setBorder(new EmptyBorder(25, 0, 0, 0));
        controlsPnl.add(Box.createHorizontalStrut(50));
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
    
    protected abstract StandaloneOptions createOptions();

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
        textAreaLabelPnl.add(Standalone.createBoldLabel("Processing warnings"));
        textAreaLabelPnl.add(Box.createHorizontalStrut(15));
        _processingErrorsLbl = new JLabel("");
        textAreaLabelPnl.add(_processingErrorsLbl);
        centerPnl.add(textAreaLabelPnl, BorderLayout.NORTH);
        _textArea = new JTextArea();
        _textArea.setEditable(false);
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
        _centerPnl.setVisible(true);
        _centerLayout.show(_centerPnl, _PANEL_ID_ANALYSIS);
        _analysisBar.setMinimum(0);
        _analysisBar.setIndeterminate(true);
        _analysisWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                File file = new File(_sourceFld.getText());
                String format = getFormatForInputFile(file);
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
                        if (file.getName().toLowerCase().endsWith(".gz"))
                            _formatLbl.setText("Compressed " + NaaccrFormat.getInstance(format).getDisplayName());
                        else
                            _formatLbl.setText(NaaccrFormat.getInstance(format).getDisplayName());
                        _numLinesLbl.setText(Standalone.formatNumber(numLines));
                        _fileSizeLbl.setText(Standalone.formatFileSize(file.length()));
                        _northLayout.show(_northPnl, _PANEL_ID_ANALYSIS_RESULTS);
                        _analysisBar.setIndeterminate(false);
                        _centerLayout.show(_centerPnl, _PANEL_ID_OPTIONS);
                        _targetFld.setText(invertFilename(new File(_sourceFld.getText())));
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                }
                catch (CancellationException | InterruptedException e) {
                    // ignored
                }
                catch (ExecutionException e) {
                    reportError(e.getCause().getMessage());
                }
                finally {
                    _analysisWorker = null;
                }
            }
        };
        _analysisWorker.execute();
    }

    protected abstract String getFormatForInputFile(File file);

    private void performProcessing() {
        _northProcessingPnl.setVisible(true);
        _textArea.setText(null);
        _centerLayout.show(_centerPnl, _PANEL_ID_PROCESSING);
        _processingBar.setMinimum(0);
        _processingBar.setMaximum(Integer.valueOf(_numLinesLbl.getText().replaceAll(",", "")));
        _processingBar.setValue(0);
        _processingErrorsLbl.setText("");
        _processingWorker = new SwingWorker<Void, Patient>() {
            @Override
            protected Void doInBackground() throws Exception {
                File srcFile = new File(_sourceFld.getText());
                File targetFile = new File(_targetFld.getText());

                NaaccrDictionary userDictionary = null;
                if (!_dictionaryFld.getText().isEmpty())
                    userDictionary = NaaccrDictionaryUtils.readDictionary(new File(_dictionaryFld.getText()));

                runProcessing(srcFile, targetFile, _guiOptions.getOptions(), userDictionary, new NaaccrStreamObserver() {
                    @Override
                    public void patientRead(Patient patient) {
                        publish(patient);
                    }

                    @Override
                    public void patientWritten(Patient patient) {
                    }
                });

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                }
                catch (CancellationException | InterruptedException e) {
                    // ignored
                }
                catch (ExecutionException e) {
                    reportError(e.getCause().getMessage());
                }
                finally {
                    _northProcessingPnl.setVisible(false);
                    _processingWorker = null;
                }
            }

            @Override
            protected void process(final List<Patient> patients) {
                final StringBuilder buf = new StringBuilder();
                for (Patient patient : patients) {
                    for (NaaccrValidationError error : patient.getAllValidationErrors()) {
                        buf.append("Line ").append(error.getLineNumber());
                        if (error.getNaaccrId() != null) {
                            buf.append(", item '").append(error.getNaaccrId()).append("'");
                            if (error.getNaaccrNum() != null)
                                buf.append(" (#").append(error.getNaaccrNum()).append(")");
                        }
                        buf.append(": ").append(error.getMessage());
                        if (error.getValue() != null && !error.getValue().isEmpty())
                            buf.append(": ").append(error.getValue());
                        buf.append("\n");
                    }
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // technically this should use the "endLineNumber", not the "startLineNumber", but that's close enough for a progress bar...
                        int processedLineNumber = 0;
                        for (Patient patient : patients)
                            processedLineNumber = Math.max(processedLineNumber, patient.getStartLineNumber());
                        _processingBar.setValue(processedLineNumber);
                        if (_textArea.getLineCount() < 10000)
                            _textArea.append(buf.toString());
                        else if (_processingErrorsLbl.getText().trim().isEmpty()) {
                            _processingErrorsLbl.setText("(reached maximum number of warnings that can be displayed)");
                            _textArea.append("...");
                        }
                    }
                });
            }
        };
        _processingWorker.execute();
    }
    
    protected abstract void runProcessing(File source, File target, NaaccrXmlOptions options, NaaccrDictionary dictionary, NaaccrStreamObserver observer) throws NaaccrIOException;

    private void reportError(String error) {
        _centerPnl.setVisible(true);
        _analysisBar.setIndeterminate(false);
        _errorLbl.setText(error == null || error.isEmpty() ? "unexpected error" : error);
        _centerLayout.show(_centerPnl, _PANEL_ID_ERROR);
    }
}
