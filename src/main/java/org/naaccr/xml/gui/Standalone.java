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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.Thread.UncaughtExceptionHandler;
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

import org.naaccr.xml.gui.pages.FlatToXmlPage;
import org.naaccr.xml.gui.pages.XmlToFlatPage;

// icons: https://www.iconfinder.com/iconsets/ellegant
public class Standalone extends JFrame {
    
    private CardLayout _layout;
    private JPanel _centerPnl;
    private JLabel _titleLbl;
    private List<JButton> _buttons = new ArrayList<>();

    public Standalone() {
        this.setTitle("NAACCR XML Utility v0.4");
        this.setPreferredSize(new Dimension(1000, 700));
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());

        JPanel northPnl = new JPanel(new FlowLayout(FlowLayout.LEADING));
        northPnl.setBackground(new Color(134, 178, 205));
        northPnl.setBorder(new CompoundBorder(new MatteBorder(1, 1, 1, 1, Color.GRAY), new EmptyBorder(0, 5, 0, 5)));
        _titleLbl = new JLabel();
        northPnl.add(_titleLbl);
        this.add(northPnl, BorderLayout.NORTH);
        
        JToolBar toolbar = new JToolBar();
        toolbar.setOpaque(true);
        toolbar.setBackground(new Color(167, 191, 205));
        toolbar.setFloatable(false);
        toolbar.setBorder(new CompoundBorder(new MatteBorder(0, 1, 1, 1, Color.GRAY), new EmptyBorder(5, 10, 5, 10)));
        
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.add(createToolbarButton("Flat to XML", "flat_to_xml.png", "flat-to-xml", "transform a NAACCR Flat file into the corresponding NAACCR XML file."));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("XML to Flat", "xml_to_flat.png", "xml-to-flat", "TODO"));
        toolbar.add(Box.createVerticalStrut(15));
        toolbar.add(createToolbarButton("Dictionary", "dictionary.png", "dictionary", "TODO"));
        //toolbar.add(Box.createVerticalStrut(15));
        //toolbar.add(createToolbarButton("Samples", "dictionary.png", "samples"));
        this.getContentPane().add(toolbar, BorderLayout.WEST);

        _centerPnl = new JPanel();
        _centerPnl.setBorder(null);
        _layout = new CardLayout();
        _centerPnl.setLayout(_layout);
        _centerPnl.add("flat-to-xml", new FlatToXmlPage());
        _centerPnl.add("xml-to-flat", new XmlToFlatPage());
        this.getContentPane().add(_centerPnl, BorderLayout.CENTER);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, final Throwable e) {
                //e.printStackTrace(); // TODO FPD
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String msg = "An unexpected error happened, it is recommended to close the application.\n\n   Error: " + (e.getMessage() == null ? "null access" : e.getMessage());
                        JOptionPane.showMessageDialog(Standalone.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
        
        _buttons.get(0).doClick();
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
                _titleLbl.setText(text + ": " + description);
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

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.pack();
                frame.setLocation(200, 200);
                frame.setVisible(true);
            }
        });
    }
}
