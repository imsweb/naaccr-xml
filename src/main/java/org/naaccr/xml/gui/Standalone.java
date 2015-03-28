/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package org.naaccr.xml.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

import org.naaccr.xml.gui.pages.DictionariesPage;
import org.naaccr.xml.gui.pages.FlatToXmlPage;
import org.naaccr.xml.gui.pages.SamplesPage;
import org.naaccr.xml.gui.pages.XmlToFlatPage;

// icons: https://www.iconfinder.com/iconsets/ellegant
// TODO FPD add a validation page that starts with a simple editable XML file, but allows to load another XML file (should give a warning for large files)
// TODO FDP use darker icons for non-selected pages
// TODO FPD what about help and about? Might need a menu with "File" and "Help"
// TODO FPD change processing page so it shows the created file path and it's size, on the top of the warnings (instead of hiding the progress bar panel)
// TODO FPD should the processing page show the number of patient/tumors for XML files instead of the number of lines?
public class Standalone extends JFrame {

    private CardLayout _layout;
    private JPanel _centerPnl;
    private JLabel _currentPageIdLbl, _currentPageDescLbl;
    private List<JButton> _buttons = new ArrayList<>();

    public Standalone() {
        this.setTitle("NAACCR XML Utility v0.4");
        this.setMinimumSize(new Dimension(1000, 700));
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());

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
        toolbar.add(createToolbarButton("Flat to XML", "flat_to_xml.png", "flat-to-xml", "transform a NAACCR Flat file into the corresponding NAACCR XML file."));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("XML to Flat", "xml_to_flat.png", "xml-to-flat", "transform a NAACCR XML file inot the corresponding Flat file."));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("Dictionary", "dictionary.png", "dictionary", "standard NAACCR dictionaries."));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("Samples", "samples.png", "samples", "examples of typicial NAACCR XML files."));
        this.getContentPane().add(toolbar, BorderLayout.WEST);

        _centerPnl = new JPanel();
        _centerPnl.setBorder(null);
        _layout = new CardLayout();
        _centerPnl.setLayout(_layout);
        _centerPnl.add("flat-to-xml", new FlatToXmlPage());
        _centerPnl.add("xml-to-flat", new XmlToFlatPage());
        _centerPnl.add("dictionary", new DictionariesPage());
        _centerPnl.add("samples", new SamplesPage());
        this.getContentPane().add(_centerPnl, BorderLayout.CENTER);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, final Throwable e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String msg = "An unexpected error happened, it is recommended to close the application.\n\n   Error: " + (e.getMessage() == null ? "null access" : e.getMessage());
                        JOptionPane.showMessageDialog(Standalone.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                _buttons.get(0).doClick();
            }
        });
    }

    private JButton createToolbarButton(final String text, String icon, final String pageId, final String description) {
        JButton btn = new JButton();
        btn.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("icons/" + icon)));
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setBorder(new EmptyBorder(10, 5, 5, 5));
        btn.setText("<html><b>" + text + "<b></html>");
        btn.setForeground(Color.GRAY);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setActionCommand(text);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _currentPageIdLbl.setText(text + " : ");
                _currentPageDescLbl.setText(description);
                _layout.show(_centerPnl, pageId);
                for (JButton btn : _buttons)
                    btn.setForeground(btn.getActionCommand().equals(text) ? Color.BLACK : Color.GRAY);
            }
        });
        _buttons.add(btn);
        return btn;
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
        
        // try to be smart about the initial size of the frame
        frame.pack();
        frame.setPreferredSize(new Dimension(Math.min(frame.getPreferredSize().width, 1100), Math.min(frame.getPreferredSize().height, 700)));
        frame.pack();

        // start in the middle of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point center = new Point(screenSize.width / 2, screenSize.height / 2);
        frame.setLocation(center.x - frame.getWidth() / 2, center.y - frame.getHeight() / 2);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
    }
}
