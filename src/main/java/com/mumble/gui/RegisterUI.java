package com.mumble.gui;

import com.mumble.client.ChatClient;

import com.mumble.util.ThemeUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Register UI with gradient background matching LoginUI style.
 */
public class RegisterUI extends BaseUI {
    private final ChatClient client;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton registerButton;
    private JButton backButton;
    private JLabel statusLabel;

    public RegisterUI(ChatClient client) {
        super("Mumble - Register");
        this.client = client;
        setSize(460, 480);
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        JPanel bgPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, ThemeUtil.getGradientTop(), 0, getHeight(), ThemeUtil.getGradientBot());
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(ThemeUtil.getPanelColor().getRed(), ThemeUtil.getPanelColor().getGreen(), ThemeUtil.getPanelColor().getBlue(), 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(ThemeUtil.getBorderColor());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(360, 380));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;

        JLabel title = new JLabel("Create Account", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(ThemeUtil.getForegroundColor());
        gbc.insets = new Insets(25, 40, 15, 40);
        gbc.gridy = 0;
        card.add(title, gbc);

        JLabel uLabel = new JLabel("Username");
        uLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        uLabel.setForeground(ThemeUtil.getFgSecondary());
        gbc.insets = new Insets(0, 40, 4, 40);
        gbc.gridy = 1;
        card.add(uLabel, gbc);

        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(280, 38));
        usernameField.setBackground(ThemeUtil.getInputBg());
        usernameField.setForeground(ThemeUtil.getForegroundColor());
        usernameField.setCaretColor(ThemeUtil.getAccentColor());
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtil.getBorderColor(), 1, true),
            new EmptyBorder(5, 12, 5, 12)));
        gbc.insets = new Insets(0, 40, 12, 40);
        gbc.gridy = 2;
        card.add(usernameField, gbc);

        JLabel pLabel = new JLabel("Password");
        pLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pLabel.setForeground(ThemeUtil.getFgSecondary());
        gbc.insets = new Insets(0, 40, 4, 40);
        gbc.gridy = 3;
        card.add(pLabel, gbc);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(280, 38));
        passwordField.setBackground(ThemeUtil.getInputBg());
        passwordField.setForeground(ThemeUtil.getForegroundColor());
        passwordField.setCaretColor(ThemeUtil.getAccentColor());
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtil.getBorderColor(), 1, true),
            new EmptyBorder(5, 12, 5, 12)));
        gbc.insets = new Insets(0, 40, 12, 40);
        gbc.gridy = 4;
        card.add(passwordField, gbc);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(239, 68, 68));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.insets = new Insets(0, 40, 5, 40);
        gbc.gridy = 5;
        card.add(statusLabel, gbc);

        registerButton = new JButton("Register") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, ThemeUtil.getPrimaryColor(), getWidth(), 0, ThemeUtil.getAccentColor());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        registerButton.setForeground(Color.WHITE);
        registerButton.setContentAreaFilled(false);
        registerButton.setOpaque(false);
        registerButton.setFocusPainted(false);
        registerButton.setBorder(new EmptyBorder(12, 0, 12, 0));
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.addActionListener(e -> attemptRegister());
        gbc.insets = new Insets(8, 40, 10, 40);
        gbc.gridy = 6;
        card.add(registerButton, gbc);

        backButton = new JButton("Already have an account? Login");
        backButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backButton.setForeground(ThemeUtil.getAccentColor());
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> { new LoginUI(client); dispose(); });
        gbc.insets = new Insets(0, 40, 20, 40);
        gbc.gridy = 7;
        card.add(backButton, gbc);

        bgPanel.add(card);
        setContentPane(bgPanel);
    }

    private void attemptRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        registerButton.setEnabled(false);
        new Thread(() -> {
            boolean success = client.getConnection().register(username, password);
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    JOptionPane.showMessageDialog(this, "Registration successful! You can now login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    new LoginUI(client);
                    dispose();
                } else {
                    statusLabel.setText("Username already taken.");
                    registerButton.setEnabled(true);
                }
            });
        }).start();
    }

    @Override
    protected void onThemeChanged() {
        getContentPane().repaint();
    }
}
