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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import org.naaccr.xml.NaaccrErrorUtils;
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

    protected static final String _COMPRESSION_NONE = "None";
    protected static final String _COMPRESSION_GZIP = "GZip";
    protected static final String _COMPRESSION_XZ = "XZ (LZMA)";

    protected static final String _NORTH_PANEL_ID_NO_FILE = "no-file";
    protected static final String _NORTH_PANEL_ID_ERROR = "pre-analysis-error";
    protected static final String _NORTH_PANEL_ID_ANALYSIS_RESULTS = "pre-analysis-results";

    protected static final String _CENTER_PANEL_ID_HELP = "help";
    protected static final String _CENTER_PANEL_ID_OPTIONS = "options";
    protected static final String _CENTER_PANEL_ID_PROCESSING = "processing";

    protected static final String _NORTH_PROCESSING_PANEL_ID_ANALYSIS = "processing-analysis";
    protected static final String _NORTH_PROCESSING_PANEL_ID_PROGRESS = "processing-progress";
    protected static final String _NORTH_PROCESSING_PANEL_ID_RESULTS = "processing-results";
    protected static final String _NORTH_PROCESSING_PANEL_ID_INTERRUPTED = "processing-interrupted";
    protected static final String _NORTH_PROCESSING_PANEL_ID_ERROR = "processing-error";

    protected JFileChooser _fileChooser, _dictionaryFileChooser;
    protected CardLayout _northLayout, _centerLayout, _northProcessingLayout;
    protected JPanel _northPnl, _centerPnl, _northProcessingPnl;
    protected JTextField _sourceFld, _targetFld, _dictionaryFld;
    protected JComboBox<String> _compressionBox;
    protected JProgressBar _analysisBar, _processingBar;
    protected JLabel _analysisErrorLbl, _processingErrorLbl, _processingResultLbl, _formatLbl, _numLinesLbl, _fileSizeLbl;
    protected JTextArea _warningsTextArea, _warningsSummaryTextArea;
    protected JTabbedPane _warningsPane;
    protected StandaloneOptions _guiOptions;

    protected transient SwingWorker<Void, Void> _analysisWorker;
    protected transient SwingWorker<Void, Patient> _processingWorker;

    protected boolean _maxWarningsReached = false, _maxWarningsDiscAdded = false;
    protected Map<String, AtomicInteger> _warningStats = new HashMap<>();
    protected Map<String, Set<String>> _warningStatsDetails = new HashMap<>();

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
        sourceFilePnl.add(Box.createHorizontalStrut(5));
        _sourceFld = new JTextField(60);
        _sourceFld.setBackground(Color.WHITE);
        sourceFilePnl.add(_sourceFld);
        sourceFilePnl.add(Box.createHorizontalStrut(5));
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_fileChooser.showDialog(AbstractProcessingPage.this, "Select") == JFileChooser.APPROVE_OPTION) {
                    _sourceFld.setText(_fileChooser.getSelectedFile().getAbsolutePath());
                    performPreAnalysis();
                }
            }
        });
        sourceFilePnl.add(browseBtn);
        inputFilePnl.add(sourceFilePnl, BorderLayout.NORTH);

        _northPnl = new JPanel();
        _northLayout = new CardLayout();
        _northPnl.setLayout(_northLayout);
        _northPnl.add(_NORTH_PANEL_ID_NO_FILE, buildNoFileSelectedPanel());
        _northPnl.add(_NORTH_PANEL_ID_ANALYSIS_RESULTS, buildAnalysisResultsPanel());
        _northPnl.add(_NORTH_PANEL_ID_ERROR, buildAnalysisErrorPanel());
        inputFilePnl.add(_northPnl, BorderLayout.SOUTH);
        this.add(inputFilePnl, BorderLayout.NORTH);

        _centerPnl = new JPanel();
        _centerLayout = new CardLayout();
        _centerPnl.setLayout(_centerLayout);
        _centerPnl.add(_CENTER_PANEL_ID_HELP, buildHelpPanel());
        _centerPnl.add(_CENTER_PANEL_ID_OPTIONS, buildOptionsPanel());
        _centerPnl.add(_CENTER_PANEL_ID_PROCESSING, buildProcessingPanel());
        this.add(_centerPnl, BorderLayout.CENTER);
    }

    protected abstract String getSourceLabelText();

    protected abstract String getTargetLabelText();

    private JPanel buildTextPnl(String text) {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 2));
        pnl.add(new JLabel(text));
        return pnl;
    }

    private JPanel buildNoFileSelectedPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        pnl.setBorder(new EmptyBorder(10, 10, 20, 0));
        pnl.add(buildTextPnl("No file selected; please use the Browse button to select one."));
        return pnl;
    }

    private JPanel buildHelpPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(10, 10, 0, 0));
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));

        pnl.add(buildTextPnl("The following NAACCR versions are supported:"));
        pnl.add(buildTextPnl("             NAACCR 15"));
        pnl.add(buildTextPnl("             NAACCR 14"));
        pnl.add(buildTextPnl("The Abstract, Modified, Confidential and Incidence flavors are supported for those versions."));
        pnl.add(Box.createVerticalStrut(25));
        pnl.add(buildTextPnl("Note that this utility is not a data conversion tool, it simply translated one format into another."));
        pnl.add(buildTextPnl("That means the created file (Flat or XML) will always have the same NAACCR version (and same data) as its source."));
        pnl.add(Box.createVerticalStrut(25));
        pnl.add(buildTextPnl("The following compressions are supported:"));
        pnl.add(buildTextPnl("             GZip (\".gz\" extension)"));
        pnl.add(buildTextPnl("             XZ (\".xz\" extension; this compression will usually produce smaller files than GZip but will take longer to process"));
        pnl.add(buildTextPnl("             Uncompressed (anything not ending in .gz or .xz will be treated as uncompressed)"));

        JPanel wrapperPnl = new JPanel(new BorderLayout());
        wrapperPnl.add(pnl, BorderLayout.NORTH);
        return wrapperPnl;
    }

    private JPanel buildAnalysisResultsPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(15, 25, 10, 0));
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
        JPanel pnl = new JPanel(new BorderLayout());

        JPanel headerPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        headerPnl.setBorder(new EmptyBorder(10, 0, 15, 0));
        JLabel headerLbl = Standalone.createBoldLabel("Please review and/or change the following options. Once you are ready, click the process button at the bottom of the page.");
        headerLbl.setForeground(new Color(150, 0, 0));
        headerPnl.add(headerLbl);
        pnl.add(headerPnl, BorderLayout.NORTH);

        JPanel allOptionsPnl = new JPanel();
        allOptionsPnl.setBorder(new EmptyBorder(0, 15, 0, 0));
        allOptionsPnl.setLayout(new BoxLayout(allOptionsPnl, BoxLayout.Y_AXIS));
        pnl.add(allOptionsPnl, BorderLayout.CENTER);

        if (showTargetInput()) {
            JPanel targetFieldPnl = new JPanel();
            targetFieldPnl.setOpaque(false);
            targetFieldPnl.setBorder(null);
            targetFieldPnl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
            targetFieldPnl.add(Standalone.createBoldLabel(getTargetLabelText()));
            targetFieldPnl.add(Box.createHorizontalStrut(5));
            _targetFld = new JTextField(60);
            targetFieldPnl.add(_targetFld);
            targetFieldPnl.add(Box.createHorizontalStrut(5));
            JButton browseBtn = new JButton("Browse...");
            browseBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (_fileChooser.showDialog(AbstractProcessingPage.this, "Select") == JFileChooser.APPROVE_OPTION)
                        _targetFld.setText(_fileChooser.getSelectedFile().getAbsolutePath());
                }
            });
            targetFieldPnl.add(browseBtn);
            targetFieldPnl.add(Box.createHorizontalStrut(10));
            targetFieldPnl.add(Standalone.createBoldLabel("Compression:"));
            targetFieldPnl.add(Box.createHorizontalStrut(5));
            _compressionBox = new JComboBox<>(new String[] {_COMPRESSION_NONE, _COMPRESSION_GZIP, _COMPRESSION_XZ});
            _compressionBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!_targetFld.getText().isEmpty())
                        _targetFld.setText(fixFileExtension(_targetFld.getText(), (String)_compressionBox.getSelectedItem()));
                }
            });
            targetFieldPnl.add(_compressionBox);
            allOptionsPnl.add(targetFieldPnl);
            allOptionsPnl.add(Box.createVerticalStrut(15));
        }

        JPanel optionsPnl = new JPanel(new BorderLayout());
        Font font = new JLabel().getFont();
        optionsPnl.setBorder(new TitledBorder(null, "Processing Options", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, font.deriveFont(Font.BOLD), Color.BLACK));
        _guiOptions = createOptions();
        _guiOptions.setBorder(new EmptyBorder(10, 20, 10, 10));
        optionsPnl.add(_guiOptions);
        allOptionsPnl.add(optionsPnl);

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
        allOptionsPnl.add(dictionaryPnl);

        JPanel controlsPnl = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        controlsPnl.setBorder(new EmptyBorder(25, 300, 10, 0));
        JButton processBtn = new JButton("Process Source File");
        processBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performAnalysis();
            }
        });
        controlsPnl.add(processBtn);
        allOptionsPnl.add(controlsPnl);

        // need this to make sure process button is visible; shouldn't need it, but I can't make it work otherwise!
        pnl.add(new JLabel(" "), BorderLayout.SOUTH);

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
        _northProcessingPnl.add(_NORTH_PROCESSING_PANEL_ID_ANALYSIS, buildProcessingAnalysisPanel());
        _northProcessingPnl.add(_NORTH_PROCESSING_PANEL_ID_PROGRESS, buildProcessingProgressPanel());
        _northProcessingPnl.add(_NORTH_PROCESSING_PANEL_ID_RESULTS, buildProcessingResultsPanel());
        _northProcessingPnl.add(_NORTH_PROCESSING_PANEL_ID_INTERRUPTED, buildProcessingInterruptedPanel());
        _northProcessingPnl.add(_NORTH_PROCESSING_PANEL_ID_ERROR, buildProcessingErrorPanel());
        pnl.add(_northProcessingPnl, BorderLayout.NORTH);

        JPanel centerPnl = new JPanel(new BorderLayout());
        _warningsPane = new JTabbedPane();
        centerPnl.add(_warningsPane, BorderLayout.CENTER);

        JPanel warningsPnl = new JPanel(new BorderLayout());
        warningsPnl.setBorder(null);
        _warningsTextArea = new JTextArea("Processing not starting...");
        _warningsTextArea.setForeground(Color.GRAY);
        _warningsTextArea.setEditable(false);
        _warningsTextArea.setBorder(new EmptyBorder(2, 3, 2, 3));
        JScrollPane warningsPane = new JScrollPane(_warningsTextArea);
        warningsPane.setBorder(new LineBorder(Color.LIGHT_GRAY));
        warningsPnl.add(warningsPane, BorderLayout.CENTER);
        _warningsPane.add("Warnings", warningsPnl);

        JPanel summaryPnl = new JPanel(new BorderLayout());
        summaryPnl.setBorder(null);
        _warningsSummaryTextArea = new JTextArea("Processing not starting...");
        _warningsSummaryTextArea.setForeground(Color.GRAY);
        _warningsSummaryTextArea.setEditable(false);
        _warningsSummaryTextArea.setBorder(new EmptyBorder(2, 3, 2, 3));
        JScrollPane summaryPane = new JScrollPane(_warningsSummaryTextArea);
        summaryPane.setBorder(new LineBorder(Color.LIGHT_GRAY));
        summaryPnl.add(summaryPane, BorderLayout.CENTER);
        _warningsPane.add("Summary", summaryPnl);

        pnl.add(centerPnl, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel buildProcessingAnalysisPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBorder(new EmptyBorder(0, 0, 10, 0));

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
                _analysisBar.setMinimum(0);
                _analysisBar.setIndeterminate(true);
                _sourceFld.setText(null);
                _northLayout.show(_northPnl, _NORTH_PANEL_ID_NO_FILE);
                _centerLayout.show(_centerPnl, _CENTER_PANEL_ID_HELP);
            }
        });
        controlsPnl.add(cancelBtn);
        contentPnl.add(controlsPnl, BorderLayout.EAST);
        pnl.add(contentPnl, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel buildProcessingProgressPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBorder(new EmptyBorder(0, 0, 10, 0));

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
                _sourceFld.setText(null);
                _northLayout.show(_northPnl, _NORTH_PANEL_ID_NO_FILE);
                _centerLayout.show(_centerPnl, _CENTER_PANEL_ID_HELP);
            }
        });
        controlsPnl.add(cancelBtn);
        contentPnl.add(controlsPnl, BorderLayout.EAST);
        pnl.add(contentPnl, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel buildProcessingResultsPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(new EmptyBorder(10, 0, 0, 0));
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

    private void performPreAnalysis() {
        _centerPnl.setVisible(false);
        File file = new File(_sourceFld.getText());
        NaaccrFormat format = getFormatForInputFile(file);
        if (format != null) { // if it's null, an error has already been reported to the user
            if (file.getName().toLowerCase().endsWith(".gz") || file.getName().toLowerCase().endsWith(".xz"))
                _formatLbl.setText("Compressed " + format.getDisplayName());
            else
                _formatLbl.setText(format.getDisplayName());
            _numLinesLbl.setText("<not evaluated yet>");
            _fileSizeLbl.setText(Standalone.formatFileSize(file.length()));
            _northLayout.show(_northPnl, _NORTH_PANEL_ID_ANALYSIS_RESULTS);
            _centerPnl.setVisible(true);
            _centerLayout.show(_centerPnl, _CENTER_PANEL_ID_OPTIONS);
            if (_targetFld != null) {
                _targetFld.setText(invertFilename(new File(_sourceFld.getText())));
                if (_targetFld.getText().endsWith(".gz"))
                    _compressionBox.setSelectedItem(_COMPRESSION_GZIP);
                else if (_targetFld.getText().endsWith(".xz"))
                    _compressionBox.setSelectedItem(_COMPRESSION_XZ);
                else
                    _compressionBox.setSelectedItem(_COMPRESSION_NONE);
            }
        }
    }

    protected abstract NaaccrFormat getFormatForInputFile(File file);

    private void performAnalysis() {
        if (_targetFld != null && new File(_targetFld.getText()).exists()) {
            int result = JOptionPane.showConfirmDialog(this, "Target file already exists, are you sure you want to replace it?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION)
                return;
        }

        _centerPnl.setVisible(true);
        _centerLayout.show(_centerPnl, _CENTER_PANEL_ID_PROCESSING);
        _processingResultLbl.setText(null);
        _northProcessingPnl.setVisible(true);
        _northProcessingLayout.show(_northProcessingPnl, _NORTH_PROCESSING_PANEL_ID_ANALYSIS);

        _warningsTextArea.setText(null);
        _warningsTextArea.setForeground(new Color(150, 0, 0));
        _warningsSummaryTextArea.setText("Processing not done...");
        _warningsSummaryTextArea.setForeground(Color.GRAY);

        _analysisBar.setMinimum(0);
        _analysisBar.setIndeterminate(true);

        final File srcFile = new File(_sourceFld.getText());
        final long start = System.currentTimeMillis();

        _analysisWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                int numLines = 0;
                try (LineNumberReader reader = new LineNumberReader(NaaccrXmlUtils.createReader(srcFile))) {
                    String line = reader.readLine();
                    while (line != null) {
                        numLines++;
                        line = reader.readLine();
                    }
                    _numLinesLbl.setText(Standalone.formatNumber(numLines));
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    _analysisBar.setMinimum(0);
                    _analysisBar.setIndeterminate(true);
                    performProcessing(srcFile, System.currentTimeMillis() - start);
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

    private void performProcessing(final File srcFile, final long analysisTime) {

        _northProcessingLayout.show(_northProcessingPnl, _NORTH_PROCESSING_PANEL_ID_PROGRESS);

        _processingBar.setMinimum(0);
        _processingBar.setMaximum(Integer.valueOf(_numLinesLbl.getText().replaceAll(",", "")));
        _processingBar.setValue(0);

        _maxWarningsReached = _maxWarningsDiscAdded = false;
        _warningStats.clear();
        _warningStatsDetails.clear();

        _processingWorker = new SwingWorker<Void, Patient>() {
            @Override
            protected Void doInBackground() throws Exception {
                final File targetFile = _targetFld == null ? null : new File(fixFileExtension(_targetFld.getText(), (String)_compressionBox.getSelectedItem()));

                NaaccrDictionary userDictionary = null;
                if (!_dictionaryFld.getText().isEmpty())
                    userDictionary = NaaccrXmlDictionaryUtils.readDictionary(new File(_dictionaryFld.getText()));

                final long start = System.currentTimeMillis();
                runProcessing(srcFile, targetFile, _guiOptions.getOptions(), userDictionary, new NaaccrObserver() {
                    @Override
                    public void patientRead(Patient patient) {
                        publish(patient);
                    }

                    @Override
                    public void patientWritten(Patient patient) {
                    }
                });

                // update GUI
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        long processingTime = System.currentTimeMillis() - start;
                        String size = targetFile == null ? null : Standalone.formatFileSize(targetFile.length());
                        String path = targetFile == null ? null : targetFile.getPath();
                        _processingResultLbl.setText(getProcessingResultText(path, analysisTime, processingTime, size));
                        _northProcessingLayout.show(_northProcessingPnl, _NORTH_PROCESSING_PANEL_ID_RESULTS);
                    }
                });

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (_warningsTextArea.getText().isEmpty()) {
                                _warningsTextArea.setForeground(Color.GRAY);
                                _warningsTextArea.setText("Found no warning, well done!");
                            }

                            if (_warningStats.isEmpty()) {
                                _warningsTextArea.setForeground(Color.GRAY);
                                _warningsSummaryTextArea.setText("Found no warning, well done!");
                            }
                            else {
                                _warningsSummaryTextArea.setForeground(Color.BLACK);
                                StringBuilder buf = new StringBuilder("Validation warning counts (0 counts not displayed):\n\n");
                                for (String code : NaaccrErrorUtils.getAllValidationErrors().keySet()) {
                                    int count = _warningStats.containsKey(code) ? _warningStats.get(code).get() : 0;
                                    if (count > 0) {
                                        buf.append("   ").append(code).append(": ").append(Standalone.formatNumber(count)).append("\n");
                                        if (_warningStatsDetails.containsKey(code)) {
                                            List<String> list = new ArrayList<>(_warningStatsDetails.get(code));
                                            Collections.sort(list);
                                            buf.append("      ").append(list).append("\n");
                                        }
                                    }
                                }
                                _warningsSummaryTextArea.setText(buf.toString());
                            }
                        }
                    });
                }
                catch (CancellationException | InterruptedException e) {
                    _warningsSummaryTextArea.setText("Processing interrupted...");
                }
                catch (ExecutionException e) {
                    reportProcessingError(e.getCause().getMessage());
                    _warningsSummaryTextArea.setText("Processing error...");
                }
                finally {
                    _processingWorker = null;
                }
            }

            @Override
            protected void process(final List<Patient> patients) {
                final StringBuilder buf = new StringBuilder();

                // extract errors
                for (Patient patient : patients) {
                    for (NaaccrValidationError error : patient.getAllValidationErrors()) {
                        // this will be shown in the warnings view
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

                        // this will be used in the summary view
                        AtomicInteger count = _warningStats.get(error.getCode());
                        if (count == null)
                            _warningStats.put(error.getCode(), new AtomicInteger(1));
                        else
                            count.incrementAndGet();

                        // let's also keep track of more detailed information
                        if (error.getNaaccrId() != null) {
                            Set<String> set = _warningStatsDetails.get(error.getCode());
                            if (set == null) {
                                set = new HashSet<>();
                                _warningStatsDetails.put(error.getCode(), set);
                            }
                            set.add(error.getNaaccrId());
                        }
                    }
                }

                // update GUI (process bar and text area)
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // technically this should use the "endLineNumber", not the "startLineNumber", but that's close enough for a progress bar...
                        int processedLineNumber = 0;
                        for (Patient patient : patients)
                            processedLineNumber = Math.max(processedLineNumber, patient.getStartLineNumber());
                        _processingBar.setValue(processedLineNumber);
                        if (!_maxWarningsReached) {
                            _warningsTextArea.append(buf.toString());
                            if (_warningsTextArea.getLineCount() > 5000)
                                _maxWarningsReached = true;
                        }
                        else if (!_maxWarningsDiscAdded) {
                            _warningsTextArea.append(
                                    "Reached maximum number of warnings that can be displayed; use the summary instead (available once the processing is done)...");
                            _maxWarningsDiscAdded = true;
                        }
                    }
                });
            }
        };
        _processingWorker.execute();
    }

    protected abstract void runProcessing(File source, File target, NaaccrOptions options, NaaccrDictionary dictionary, NaaccrObserver observer) throws NaaccrIOException;

    protected String getProcessingResultText(String path, long analysisTime, long processingTime, String size) {
        String analysis = Standalone.formatTime(analysisTime);
        String processing = Standalone.formatTime(processingTime);
        String total = Standalone.formatTime(analysisTime + processingTime);
        return "Successfully created \"" + path + "\" (" + size + ") in " + total + " (anaysis: " + analysis + ", processing: " + processing + ")";
    }

    protected void reportAnalysisError(String error) {
        _centerPnl.setVisible(false);
        _analysisBar.setIndeterminate(false);
        _analysisErrorLbl.setText(error == null || error.isEmpty() ? "unexpected error" : error);
        _northLayout.show(_northPnl, _NORTH_PANEL_ID_ERROR);
    }

    protected void reportProcessingError(String error) {
        _processingErrorLbl.setText(error == null || error.isEmpty() ? "unexpected error" : error);
        _northProcessingLayout.show(_northProcessingPnl, _NORTH_PROCESSING_PANEL_ID_ERROR);
    }

    private String fixFileExtension(String filename, String compression) {
        String result = filename;

        if (_COMPRESSION_GZIP.equals(compression)) {
            if (result.endsWith(".xz"))
                result = result.replace(".xz", "");
            if (!result.endsWith(".gz"))
                result = result + ".gz";
        }
        else if (_COMPRESSION_XZ.equals(compression)) {
            if (result.endsWith(".gz"))
                result = result.replace(".gz", "");
            if (!result.endsWith(".xz"))
                result = result + ".xz";
        }
        else if (_COMPRESSION_NONE.equals(compression)) {
            if (result.endsWith(".gz"))
                result = result.replace(".gz", "");
            else if (result.endsWith(".xz"))
                result = filename.replace(".xz", "");
        }

        return result;
    }
}
