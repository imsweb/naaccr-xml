/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class AboutDialog extends JDialog {

    public AboutDialog(Window owner) {
        super(owner);

        this.setTitle("About this tool");
        this.setModal(true);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());
        
        JPanel contentPnl = new JPanel(new BorderLayout());
        contentPnl.setOpaque(true);
        contentPnl.setBackground(new Color(167, 191, 205));
        contentPnl.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.getContentPane().add(contentPnl, BorderLayout.CENTER);
        
        JPanel centerPnl = new JPanel();
        centerPnl.setLayout(new BoxLayout(centerPnl, BoxLayout.Y_AXIS));
        centerPnl.setBorder(new CompoundBorder(new LineBorder(Color.GRAY), new EmptyBorder(10, 25, 10, 25)));
        contentPnl.add(centerPnl, BorderLayout.CENTER);
        
        centerPnl.add(buildTextPnl("NAACCR XML Utility", true));
        centerPnl.add(buildTextPnl(Standalone.VERSION, false));
        centerPnl.add(Box.createVerticalStrut(25));
        centerPnl.add(buildTextPnl("Provided by the", false));
        centerPnl.add(Box.createVerticalStrut(3));
        centerPnl.add(buildTextPnl("NAACCR XML Work Group", true));
        centerPnl.add(Box.createVerticalStrut(25));
        centerPnl.add(buildTextPnl("Developed by", false));
        centerPnl.add(Box.createVerticalStrut(3));
        centerPnl.add(buildTextPnl("Information Management Services, Inc.", true));
        centerPnl.add(Box.createVerticalStrut(6));
        centerPnl.add(buildTextPnl("under contract to the", false));
        centerPnl.add(Box.createVerticalStrut(3));
        centerPnl.add(buildTextPnl("National Cancer Institute", true));
        
        
    }

    private JPanel buildTextPnl(String text, boolean bold) {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN));
        pnl.add(lbl);
        return pnl;
    }
}
