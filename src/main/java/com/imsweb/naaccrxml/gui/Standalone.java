/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import org.apache.commons.io.IOUtils;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.gui.pages.DictionariesPage;
import com.imsweb.naaccrxml.gui.pages.DictionaryEditorPage;
import com.imsweb.naaccrxml.gui.pages.FlatToXmlPage;
import com.imsweb.naaccrxml.gui.pages.XmlToFlatPage;
import com.imsweb.naaccrxml.gui.pages.XmlToXmlPage;
import com.imsweb.naaccrxml.gui.pages.XmlValidationPage;

public class Standalone extends JFrame implements ActionListener {

    // would be nice to read this from the Manifest file in the JAR...
    public static final String VERSION = getVersion();

    private CardLayout _layout;
    private JPanel _centerPnl;
    private JLabel _currentPageIdLbl, _currentPageDescLbl;
    private List<JButton> _buttons = new ArrayList<>();

    public Standalone() {
        this.setTitle("NAACCR XML Utility " + VERSION);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());

        JMenuBar bar = new JMenuBar();
        // file
        JMenu fileMenu = new JMenu(" File ");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        bar.add(fileMenu);
        JMenuItem exitItem = new JMenuItem("Exit       ");
        exitItem.setActionCommand("menu-exit");
        exitItem.addActionListener(this);
        fileMenu.add(exitItem);
        // tools
        JMenu toolsMenu = new JMenu(" Tools ");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        bar.add(toolsMenu);
        JMenuItem sasMenu = new JMenu("Create SAS Definition ");
        toolsMenu.add(sasMenu);
        for (String version : NaaccrFormat.getSupportedVersions()) {
            JMenuItem sasItem = new JMenuItem("NAACCR " + version);
            sasItem.setActionCommand("menu-sas-" + version);
            sasItem.addActionListener(this);
            sasMenu.add(sasItem);
        }
        // help
        JMenu helpMenu = new JMenu(" Help ");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        JMenuItem helpItem = new JMenuItem("View Help       ");
        helpItem.setActionCommand("menu-help");
        helpItem.addActionListener(this);
        helpMenu.add(helpItem);
        helpMenu.addSeparator();
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setActionCommand("menu-about");
        aboutItem.addActionListener(this);
        helpMenu.add(aboutItem);
        bar.add(helpMenu);
        this.setJMenuBar(bar);

        JPanel northPnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
        northPnl.setBackground(new Color(133, 180, 205));
        northPnl.setBorder(new CompoundBorder(new MatteBorder(1, 1, 1, 1, Color.GRAY), new EmptyBorder(0, 5, 0, 5)));
        _currentPageIdLbl = new JLabel();
        _currentPageIdLbl.setFont(_currentPageIdLbl.getFont().deriveFont(Font.BOLD));
        northPnl.add(_currentPageIdLbl);
        _currentPageDescLbl = new JLabel();
        northPnl.add(_currentPageDescLbl);
        this.add(northPnl, BorderLayout.NORTH);

        JToolBar toolbar = new JToolBar();
        toolbar.setOpaque(true);
        toolbar.setBackground(new Color(167, 191, 205));
        toolbar.setFloatable(false);
        toolbar.setBorder(new CompoundBorder(new MatteBorder(0, 1, 1, 1, Color.GRAY), new EmptyBorder(5, 10, 5, 10)));

        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.add(createToolbarButton("Flat to XML", "flat_to_xml", "transform a given NAACCR Flat file (fixed-columns) into the corresponding NAACCR XML file"));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("XML to Flat", "xml_to_flat", "transform a given NAACCR XML file into the corresponding Flat file (fixed-columns)"));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("XML to XML", "xml_to_xml", "re-create a given NAACCR XML file using different options"));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("XML Validation", "validate", "validate a given NAACCR XML file"));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("Standard<br/>Dictionaries", "dictionaries", "view the standard NAACCR dictionaries used to process NAACCR XML data"));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("Dictionary<br/>Editor", "edit", "create your own user-defined dictionary"));
        this.getContentPane().add(toolbar, BorderLayout.WEST);

        _centerPnl = new JPanel();
        _centerPnl.setBorder(null);
        _layout = new CardLayout();
        _centerPnl.setLayout(_layout);
        _centerPnl.add("flat_to_xml", new FlatToXmlPage());
        _centerPnl.add("xml_to_flat", new XmlToFlatPage());
        _centerPnl.add("xml_to_xml", new XmlToXmlPage());
        _centerPnl.add("validate", new XmlValidationPage());
        _centerPnl.add("dictionaries", new DictionariesPage());
        _centerPnl.add("edit", new DictionaryEditorPage());
        this.getContentPane().add(_centerPnl, BorderLayout.CENTER);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> SwingUtilities.invokeLater(() -> {
            boolean isLocationException = e instanceof IllegalComponentStateException; // https://bugs.openjdk.java.net/browse/JDK-8179665
            if (!isLocationException) {
                String msg = "An unexpected error happened, it is recommended to close the application.\n\n   Error: " + (e.getMessage() == null ? "null access" : e.getMessage());
                JOptionPane.showMessageDialog(Standalone.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }));

        SwingUtilities.invokeLater(() -> _buttons.get(0).doClick());
    }

    public static String getVersion() {
        String version = null;

        // this will make it work when running from the JAR file
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("NAACCR-XML-VERSION")) {
            if (is != null)
                version = IOUtils.readLines(is, StandardCharsets.US_ASCII).get(0);
        }
        catch (IOException e) {
            version = null;
        }

        // this will make it work when running from an IDE
        if (version == null) {
            try (FileInputStream is = new FileInputStream(System.getProperty("user.dir") + File.separator + "VERSION")) {
                version = IOUtils.readLines(is, StandardCharsets.US_ASCII).get(0);
            }
            catch (IOException e) {
                version = null;
            }
        }

        if (version == null)
            version = "??";

        return "v" + version;
    }

    @SuppressWarnings("ConstantConditions")
    private JButton createToolbarButton(final String text, final String pageId, final String description) {
        JButton btn = new JButton();
        btn.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("gui/icons/" + pageId + "_inactive.png")));
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setBorder(new EmptyBorder(10, 5, 5, 5));
        btn.setText("<html><center><b>" + text + "<b></center></html>");
        btn.setForeground(Color.GRAY);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setActionCommand(pageId);
        btn.addActionListener(e -> {
            _currentPageIdLbl.setText(text.replace("<br/>", " ") + " : ");
            _currentPageDescLbl.setText(description);
            _layout.show(_centerPnl, pageId);
            for (JButton btn1 : _buttons) {
                if (btn1.getActionCommand().equals(pageId)) {
                    btn1.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("gui/icons/" + btn1.getActionCommand() + "_active.png")));
                    btn1.setForeground(Color.BLACK);
                }
                else {
                    btn1.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("gui/icons/" + btn1.getActionCommand() + "_inactive.png")));
                    btn1.setForeground(Color.GRAY);
                }
            }
        });
        _buttons.add(btn);
        return btn;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ("menu-exit".equals(cmd))
            System.exit(0);
        else if (cmd.startsWith("menu-sas-")) {
            String naaccrVersion = cmd.replace("menu-sas-", "");

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Select Target File");
            fileChooser.setApproveButtonToolTipText("Create CSV");
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), "naaccr-xml-sas-def-" + naaccrVersion + ".map"));
            if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION) {
                File targetFile = fileChooser.getSelectedFile();
                if (targetFile.exists()) {
                    int result = JOptionPane.showConfirmDialog(this, "Target file already exists, are you sure you want to replace it?", "Confirmation",
                            JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION)
                        return;
                }
                SasDefinitionDialog dlg = new SasDefinitionDialog(this, naaccrVersion, targetFile);
                dlg.pack();
                Point center = new Point(this.getLocationOnScreen().x + this.getWidth() / 2, this.getLocationOnScreen().y + this.getHeight() / 2);
                dlg.setLocation(center.x - dlg.getWidth() / 2, center.y - dlg.getHeight() / 2);
                SwingUtilities.invokeLater(() -> dlg.setVisible(true));
            }
        }
        else if ("menu-help".equals(cmd)) {
            try {
                File targetFile = File.createTempFile("naaccr-xml-help", ".html");
                targetFile.deleteOnExit();
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("gui/help/help.html");
                OutputStream os = new FileOutputStream(targetFile);
                IOUtils.copy(is, os);
                is.close();
                os.close();
                Desktop.getDesktop().open(targetFile);
            }
            catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Unable to display help.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if ("menu-about".equals(cmd)) {
            final JDialog dlg = new AboutDialog(this);
            dlg.pack();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Point center = new Point(screenSize.width / 2, screenSize.height / 2);
            dlg.setLocation(center.x - dlg.getWidth() / 2, center.y - dlg.getHeight() / 2);
            SwingUtilities.invokeLater(() -> dlg.setVisible(true));
        }
    }

    public static JLabel createItalicLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC));
        return lbl;
    }

    public static JLabel createBoldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        return lbl;
    }

    /**
     * Format the passed number, added commas for the decimal parts.
     * <p/>
     * Created on Dec 3, 2008 by depryf
     * @param num number to format
     * @return formatted number
     */
    public static String formatNumber(int num) {
        DecimalFormat format = new DecimalFormat();
        format.setDecimalSeparatorAlwaysShown(false);
        return format.format(num);
    }

    /**
     * Formats a time given in millisecond. The output will be "X hours Y min Z sec", unless X, Y or Z is 0 in which
     * case that part of the string will be omitted.
     * <p/>
     * Created on May 3, 2004 by Fabian Depry
     * @param timeInMilli time in milli-seconds
     * @return a <code>String</code> representing the formatted time...
     */
    @SuppressWarnings("UnusedDeclaration")
    public static String formatTime(long timeInMilli) {
        long hourBasis = 60;

        StringBuilder formattedTime = new StringBuilder();

        long secTmp = timeInMilli / 1000;
        long sec = secTmp % hourBasis;
        long minTmp = secTmp / hourBasis;
        long min = minTmp % hourBasis;
        long hour = minTmp / hourBasis;

        if (hour > 0) {
            formattedTime.append(hour).append(" hour");
            if (hour > 1)
                formattedTime.append("s");
        }

        if (min > 0) {
            if (formattedTime.length() > 0)
                formattedTime.append(", ");
            formattedTime.append(min).append(" minute");
            if (min > 1)
                formattedTime.append("s");
        }

        if (sec > 0) {
            if (formattedTime.length() > 0)
                formattedTime.append(", ");
            formattedTime.append(sec).append(" second");
            if (sec > 1)
                formattedTime.append("s");
        }

        if (formattedTime.length() > 0)
            return formattedTime.toString();

        return "< 1 second";
    }

    /**
     * Takes a string with a byte count and converts it into a "nice" representation of size.
     * <p/>
     * 124 b <br>
     * 34 KB <br>
     * 12 MB <br>
     * 2 GB
     * <p/>
     * Created on May 281, 2004 by Chuck May
     * @param size size to format
     * @return <code>String</code> with the formatted size
     */
    public static String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        else if (size < 1024 * 1024)
            return new DecimalFormat("#.# KB").format((double)size / 1024);
        else if (size < 1024 * 1024 * 1024)
            return new DecimalFormat("#.# MB").format((double)size / 1024 / 1024);

        return new DecimalFormat("#.# GB").format((double)size / 1024 / 1024 / 1024);
    }

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

        final JFrame frame = new Standalone();
        frame.pack();

        // start in the middle of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point center = new Point(screenSize.width / 2, screenSize.height / 2);
        frame.setLocation(center.x - frame.getWidth() / 2, center.y - frame.getHeight() / 2);

        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }
}
