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
import javax.swing.JOptionPane;
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

import org.naaccr.xml.NaaccrFormat;
import org.naaccr.xml.NaaccrIOException;
import org.naaccr.xml.NaaccrObserver;
import org.naaccr.xml.NaaccrOptions;
import org.naaccr.xml.NaaccrValidationError;
import org.naaccr.xml.NaaccrXmlDictionaryUtils;
import org.naaccr.xml.NaaccrXmlUtils;
import org.naaccr.xml.entity.Patient;
import org.naaccr.xml.entity.dictionary.NaaccrDictionary;
import org.naaccr.xml.gui.Standalone;
import org.naaccr.xml.gui.StandaloneOptions;

public abstract class AbstractProcessingPage extends AbstractPage {

    protected static final String _NORTH_PANEL_ID_NO_FILE = "no-file";
    protected static final String _NORTH_PANEL_ID_ANALYSIS = "analysis-progress";
    protected static final String _NORTH_PANEL_ID_ANALYSIS_RESULTS = "analysis-results";
    protected static final String _NORTH_PANEL_ID_ERROR = "analysis-error";

    protected static final String _CENTER_PANEL_ID_OPTIONS = "options";
    protected static final String _CENTER_PANEL_ID_PROCESSING = "processing";

    protected static final String _NORTH_PROCESSING_PANEL_ID_PROGRESS = "processing-progress";
    protected static final String _NORTH_PROCESSING_PANEL_ID_RESULTS = "processing-results";
    protected static final String _NORTH_PROCESSING_PANEL_ID_INTERRUPTED = "processing-interrupted";
    protected static final String _NORTH_PROCESSING_PANEL_ID_ERROR = "processing-error";

    protected JFileChooser _fileChooser, _dictionaryFileChooser;
    protected CardLayout _northLayout, _centerLayout, _northProcessingLayout;
    protected JPanel _northPnl, _centerPnl, _northProcessingPnl;
    protected JTextField _sourceFld, _targetFld, _dictionaryFld;
    protected JProgressBar _analysisBar, _processingBar;
    protected JLabel _analysisErrorLbl, _processingErrorLbl, _processingWarningLbl, _processingResultLbl, _formatLbl, _numLinesLbl, _fileSizeLbl;
    protected transient SwingWorker<Void, Void> _analysisWorker;
    protected transient SwingWorker<Void, Patient> _processingWorker;
    protected JTextArea _warningsTextArea;
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

        JPanel inputFilePnl = new JPanel(new BorderLayout());
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
        inputFilePnl.add(sourceFilePnl, BorderLayout.NORTH);

        _northPnl = new JPanel();
        _northLayout = new CardLayout();
        _northPnl.setLayout(_northLayout);
        _northPnl.add(_NORTH_PANEL_ID_NO_FILE, buildNoFileSelectedPanel());
        _northPnl.add(_NORTH_PANEL_ID_ANALYSIS, buildAnalysisPanel());
        _northPnl.add(_NORTH_PANEL_ID_ANALYSIS_RESULTS, buildAnalysisResultsPanel());
        _northPnl.add(_NORTH_PANEL_ID_ERROR, buildAnalysisErrorPanel());
        inputFilePnl.add(_northPnl, BorderLayout.SOUTH);
        this.add(inputFilePnl, BorderLayout.NORTH);

        _centerPnl = new JPanel();
        _centerLayout = new CardLayout();
        _centerPnl.setLayout(_centerLayout);
        _centerPnl.add(_CENTER_PANEL_ID_OPTIONS, buildOptionsPanel());
        _centerPnl.add(_CENTER_PANEL_ID_PROCESSING, buildProcessingPanel());
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
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBorder(new EmptyBorder(10, 25, 0, 0));

        JPanel lblPnl = new JPanel();
        lblPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        lblPnl.setBorder(null);
        lblPnl.add(Standalone.createItalicLabel("Analyzing file (this can take a while, especially when reading network resources)..."));
        pnl.add(lblPnl, BorderLayout.NORTH);

        JPanel contentPnl = new JPanel(new BorderLayout());
        JPanel progressPnl = new JPanel(new BorderLayout());
        progressPnl.setBorder(new EmptyBorder(5, 0, 5, 0));
        _analysisBar = new JProgressBar();
        progressPnl.add(_analysisBar, BorderLayout.CENTER);
        contentPnl.add(progressPnl, BorderLayout.CENTER);

        JPanel controlsPnl = new JPanel();
        controlsPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        controlsPnl.setBorder(new EmptyBorder(0, 10, 0, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_analysisWorker != null)
                    _analysisWorker.cancel(true);
                _analysisWorker = null;
                _sourceFld.setText(null);
                _northLayout.show(_northPnl, _NORTH_PANEL_ID_NO_FILE);
            }
        });
        controlsPnl.add(cancelBtn);
        contentPnl.add(controlsPnl, BorderLayout.EAST);
        pnl.add(contentPnl, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel buildAnalysisResultsPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(15, 25, 0, 0));
        pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        pnl.add(Standalone.createBoldLabel("Source File format: "));
        pnl.add(Box.createHorizontalStrut(5));
        _formatLbl = new JLabel(" ");
        pnl.add(_formatLbl);
        pnl.add(Box.createHorizontalStrut(25));
        pnl.add(Standalone.createBoldLabel("Number of lines: "));
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

    private JPanel buildAnalysisErrorPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(10, 10, 0, 0));
        pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        JLabel lbl = Standalone.createBoldLabel("Error analysing file: ");
        lbl.setForeground(Color.RED);
        pnl.add(lbl);
        _analysisErrorLbl = new JLabel(" ");
        _analysisErrorLbl.setForeground(Color.RED);
        pnl.add(_analysisErrorLbl);

        return pnl;
    }

    private JPanel buildOptionsPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(10, 0, 0, 0));
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
        if (showTargetInput()) {
            pnl.add(targetFieldPnl);
            pnl.add(Box.createVerticalStrut(15));
        }

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
        controlsPnl.setBorder(new EmptyBorder(50, 250, 0, 0));
        controlsPnl.add(Box.createHorizontalStrut(50));
        JButton processBtn = new JButton("Process Source File");
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

    protected boolean showTargetInput() {
        return true;
    }

    protected abstract StandaloneOptions createOptions();

    private JPanel buildProcessingPanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        _northProcessingPnl = new JPanel();
        _northProcessingLayout = new CardLayout();
        _northProcessingPnl.setLayout(_northProcessingLayout);
        _northProcessingPnl.add(_NORTH_PROCESSING_PANEL_ID_PROGRESS, buildProcessingProgressPanel());
        _northProcessingPnl.add(_NORTH_PROCESSING_PANEL_ID_RESULTS, buildProcessingResultsPanel());
        _northProcessingPnl.add(_NORTH_PROCESSING_PANEL_ID_INTERRUPTED, buildProcessingInterruptedPanel());
        _northProcessingPnl.add(_NORTH_PROCESSING_PANEL_ID_ERROR, buildProcessingErrorPanel());
        pnl.add(_northProcessingPnl, BorderLayout.NORTH);

        JPanel centerPnl = new JPanel(new BorderLayout());
        JPanel textAreaLabelPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        textAreaLabelPnl.add(Standalone.createBoldLabel("Processing warnings"));
        textAreaLabelPnl.add(Box.createHorizontalStrut(15));
        _processingWarningLbl = new JLabel("");
        textAreaLabelPnl.add(_processingWarningLbl);
        centerPnl.add(textAreaLabelPnl, BorderLayout.NORTH);
        _warningsTextArea = new JTextArea();
        _warningsTextArea.setEditable(false);
        _warningsTextArea.setBorder(new EmptyBorder(2, 3, 2, 3));
        JScrollPane pane = new JScrollPane(_warningsTextArea);
        pane.setBorder(new CompoundBorder(new EmptyBorder(5, 0, 0, 0), new LineBorder(Color.LIGHT_GRAY)));
        centerPnl.add(pane, BorderLayout.CENTER);
        pnl.add(centerPnl, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel buildProcessingProgressPanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        JPanel lblPnl = new JPanel();
        lblPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        lblPnl.setBorder(null);
        lblPnl.add(Standalone.createItalicLabel("Processing file..."));
        pnl.add(lblPnl, BorderLayout.NORTH);

        JPanel contentPnl = new JPanel(new BorderLayout());
        JPanel progressPnl = new JPanel(new BorderLayout());
        progressPnl.setBorder(new EmptyBorder(5, 0, 5, 0));
        _processingBar = new JProgressBar();
        progressPnl.add(_processingBar, BorderLayout.CENTER);
        contentPnl.add(progressPnl, BorderLayout.CENTER);

        JPanel controlsPnl = new JPanel();
        controlsPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        controlsPnl.setBorder(new EmptyBorder(0, 10, 0, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_processingWorker != null)
                    _processingWorker.cancel(true);
                _processingWorker = null;
                _northProcessingLayout.show(_northProcessingPnl, _NORTH_PROCESSING_PANEL_ID_INTERRUPTED);
            }
        });
        controlsPnl.add(cancelBtn);
        contentPnl.add(controlsPnl, BorderLayout.EAST);
        pnl.add(contentPnl, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel buildProcessingResultsPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        _processingResultLbl = new JLabel(" ");
        pnl.add(_processingResultLbl);

        return pnl;
    }

    private JPanel buildProcessingInterruptedPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        JLabel lbl = Standalone.createBoldLabel("Processing was interrupted.");
        pnl.add(lbl);

        return pnl;
    }

    private JPanel buildProcessingErrorPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(10, 10, 0, 0));
        pnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        JLabel lbl = Standalone.createBoldLabel("Error processing file: ");
        lbl.setForeground(Color.RED);
        pnl.add(lbl);
        _processingErrorLbl = new JLabel(" ");
        _processingErrorLbl.setForeground(Color.RED);
        pnl.add(_processingErrorLbl);

        return pnl;
    }

    private void performAnalysis() {
        _centerPnl.setVisible(false);
        _northLayout.show(_northPnl, _NORTH_PANEL_ID_ANALYSIS);
        _analysisBar.setMinimum(0);
        _analysisBar.setIndeterminate(true);
        _analysisWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                File file = new File(_sourceFld.getText());
                String format = getFormatForInputFile(file);
                if (!NaaccrFormat.isFormatSupported(format))
                    reportAnalysisError("unknown or unsupported file format");
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
                        _northLayout.show(_northPnl, _NORTH_PANEL_ID_ANALYSIS_RESULTS);
                        _analysisBar.setIndeterminate(false);
                        _centerPnl.setVisible(true);
                        _centerLayout.show(_centerPnl, _CENTER_PANEL_ID_OPTIONS);
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
                    reportAnalysisError(e.getCause().getMessage());
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
        if (new File(_targetFld.getText()).exists()) {
            int result = JOptionPane.showConfirmDialog(this, "Target file already exists, are you sure you want to replace it?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION)
                return;
        }

        _northProcessingPnl.setVisible(true);
        _warningsTextArea.setText(null);
        _warningsTextArea.setForeground(new Color(150, 0, 0));
        _centerLayout.show(_centerPnl, _CENTER_PANEL_ID_PROCESSING);
        _processingBar.setMinimum(0);
        _processingBar.setMaximum(Integer.valueOf(_numLinesLbl.getText().replaceAll(",", "")));
        _processingBar.setValue(0);
        _processingWarningLbl.setText("");
        _processingWorker = new SwingWorker<Void, Patient>() {
            @Override
            protected Void doInBackground() throws Exception {
                File srcFile = new File(_sourceFld.getText());
                File targetFile = new File(_targetFld.getText());

                NaaccrDictionary userDictionary = null;
                if (!_dictionaryFld.getText().isEmpty())
                    userDictionary = NaaccrXmlDictionaryUtils.readDictionary(new File(_dictionaryFld.getText()));

                long start = System.currentTimeMillis();
                runProcessing(srcFile, targetFile, _guiOptions.getOptions(), userDictionary, new NaaccrObserver() {
                    @Override
                    public void patientRead(Patient patient) {
                        publish(patient);
                    }

                    @Override
                    public void patientWritten(Patient patient) {
                    }
                });

                String time = Standalone.formatTime(System.currentTimeMillis() - start);
                String size = Standalone.formatFileSize(targetFile.length());
                _processingResultLbl.setText(getProcessingResultText(targetFile.getPath(), time, size));
                _northProcessingLayout.show(_northProcessingPnl, _NORTH_PROCESSING_PANEL_ID_RESULTS);

                if (_warningsTextArea.getText().isEmpty()) {
                    _warningsTextArea.setForeground(Color.GRAY);
                    _warningsTextArea.setText("Found no warning, well done!");
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
                    reportProcessingError(e.getCause().getMessage());
                }
                finally {
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
                        if (_warningsTextArea.getLineCount() < 10000)
                            _warningsTextArea.append(buf.toString());
                        else if (_processingWarningLbl.getText().trim().isEmpty()) {
                            _processingWarningLbl.setText("(reached maximum number of warnings that can be displayed)");
                            _warningsTextArea.append("...");
                        }
                    }
                });
            }
        };
        _processingWorker.execute();
    }

    protected abstract void runProcessing(File source, File target, NaaccrOptions options, NaaccrDictionary dictionary, NaaccrObserver observer) throws NaaccrIOException;
    
    protected String getProcessingResultText(String path, String time, String size) {
        return "Successfully created \"" + path + "\" (" + size + ") in " + time;
    }

    private void reportAnalysisError(String error) {
        _centerPnl.setVisible(false);
        _analysisBar.setIndeterminate(false);
        _analysisErrorLbl.setText(error == null || error.isEmpty() ? "unexpected error" : error);
        _northLayout.show(_northPnl, _NORTH_PANEL_ID_ERROR);
    }

    private void reportProcessingError(String error) {
        _processingErrorLbl.setText(error == null || error.isEmpty() ? "unexpected error" : error);
        _northProcessingLayout.show(_northProcessingPnl, _NORTH_PROCESSING_PANEL_ID_ERROR);
    }
}