package com.mumble.gui;

import com.mumble.util.ThemeUtil;

import javax.swing.*;

import java.awt.*;

/**
 * Base abstract JFrame for all Mumble windows.
 * Provides common dark theme propagation (Inheritance).
 */
public abstract class BaseUI extends JFrame {

    public BaseUI(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Recursively applies theme colors to all child components.
     */
    public void applyTheme(Container container) {
        container.setBackground(ThemeUtil.getBackgroundColor());
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(ThemeUtil.getPanelColor());
                applyTheme((Container) c);
            } else if (c instanceof JLabel) {
                c.setForeground(ThemeUtil.getForegroundColor());
            } else if (c instanceof JTextField || c instanceof JTextArea || c instanceof JPasswordField) {
                c.setBackground(ThemeUtil.getInputBg());
                c.setForeground(ThemeUtil.getForegroundColor());
                ((JComponent) c).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeUtil.getBorderColor(), 1, true),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)
                ));
            } else if (c instanceof JButton) {
                JButton btn = (JButton) c;
                btn.setBackground(ThemeUtil.getPrimaryColor());
                btn.setForeground(Color.WHITE);
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            } else if (c instanceof JScrollPane) {
                c.setBackground(ThemeUtil.getPanelColor());
                applyTheme((Container) c);
            } else if (c instanceof JViewport) {
                c.setBackground(ThemeUtil.getPanelColor());
                applyTheme((Container) c);
            } else if (c instanceof JList) {
                c.setBackground(ThemeUtil.getPanelColor());
                c.setForeground(ThemeUtil.getForegroundColor());
            }
        }
    }
    
    protected abstract void onThemeChanged();

    public void toggleTheme() {
        ThemeUtil.isDarkMode = !ThemeUtil.isDarkMode;
        applyTheme(this.getContentPane());
        onThemeChanged();
        SwingUtilities.updateComponentTreeUI(this);
    }
}
