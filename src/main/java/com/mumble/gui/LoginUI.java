package com.mumble.gui;

import com.mumble.client.ChatClient;
import com.mumble.model.User;
import com.mumble.util.ThemeUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Clean premium black Login UI.
 */
public class LoginUI extends BaseUI {
    private final ChatClient client;
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;

    public LoginUI(ChatClient client) {
        super("Mumble - Login");
        this.client = client;
        setSize(460, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        // Flat dark background panel
        JPanel bgPanel = new JPanel(new GridBagLayout());
        bgPanel.setBackground(ThemeUtil.getBackgroundColor());

        // Card — uses BgSecondary (one step up from canvas)
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeUtil.getCardColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(ThemeUtil.getBorderColor());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(360, 420));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;

        // Logo — letter M, white
        JLabel logo = new JLabel("M", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 44));
        logo.setForeground(ThemeUtil.getForegroundColor());
        gbc.insets = new Insets(28, 40, 0, 40);
        gbc.gridy = 0;
        card.add(logo, gbc);

        JLabel title = new JLabel("Welcome to Mumble", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ThemeUtil.getForegroundColor());
        gbc.insets = new Insets(6, 40, 2, 40);
        gbc.gridy = 1;
        card.add(title, gbc);

        JLabel subtitle = new JLabel("Sign in to continue", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(ThemeUtil.getFgSecondary());
        gbc.insets = new Insets(0, 40, 22, 40);
        gbc.gridy = 2;
        card.add(subtitle, gbc);

        // Username
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        usernameLabel.setForeground(ThemeUtil.getFgSecondary());
        gbc.insets = new Insets(0, 40, 4, 40);
        gbc.gridy = 3;
        card.add(usernameLabel, gbc);

        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(280, 38));
        usernameField.setBackground(ThemeUtil.getSurfaceColor());
        usernameField.setForeground(ThemeUtil.getForegroundColor());
        usernameField.setCaretColor(ThemeUtil.getPrimaryColor());
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtil.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        gbc.insets = new Insets(0, 40, 14, 40);
        gbc.gridy = 4;
        card.add(usernameField, gbc);

        // Password
        JLabel pwLabel = new JLabel("Password");
        pwLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pwLabel.setForeground(ThemeUtil.getFgSecondary());
        gbc.insets = new Insets(0, 40, 4, 40);
        gbc.gridy = 5;
        card.add(pwLabel, gbc);

        JPanel pwWrap = new JPanel(new BorderLayout(5, 0));
        pwWrap.setOpaque(false);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(240, 38));
        passwordField.setBackground(ThemeUtil.getSurfaceColor());
        passwordField.setForeground(ThemeUtil.getForegroundColor());
        passwordField.setCaretColor(ThemeUtil.getPrimaryColor());
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtil.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Custom painted eye toggle button
        JToggleButton eyeBtn = new JToggleButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(ThemeUtil.getFgSecondary());
                g2.setStroke(new BasicStroke(1.5f));
                // Draw eye shape
                g2.drawArc(cx - 10, cy - 5, 20, 10, 0, 360);
                // Pupil
                if (isSelected()) {
                    g2.fillOval(cx - 3, cy - 3, 6, 6);
                } else {
                    g2.drawOval(cx - 3, cy - 3, 6, 6);
                    // Strikethrough line when hidden
                    g2.drawLine(cx - 10, cy + 6, cx + 10, cy - 6);
                }
                g2.dispose();
            }
        };
        eyeBtn.setPreferredSize(new Dimension(42, 38));
        eyeBtn.setBackground(ThemeUtil.getInputBg());
        eyeBtn.setFocusPainted(false);
        eyeBtn.setBorder(BorderFactory.createLineBorder(ThemeUtil.getBorderColor(), 1, true));
        eyeBtn.setToolTipText("Show/Hide Password");
        eyeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeBtn.addActionListener(e -> {
            passwordField.setEchoChar(eyeBtn.isSelected() ? (char) 0 : '\u2022');
            eyeBtn.repaint();
        });

        pwWrap.add(passwordField, BorderLayout.CENTER);
        pwWrap.add(eyeBtn, BorderLayout.EAST);
        gbc.insets = new Insets(0, 40, 12, 40);
        gbc.gridy = 6;
        card.add(pwWrap, gbc);

        // Status label
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(239, 68, 68));
        gbc.insets = new Insets(0, 40, 5, 40);
        gbc.gridy = 7;
        card.add(statusLabel, gbc);

        // Login Button — solid primary blue, hover darkens
        loginButton = new JButton("Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ThemeUtil.getPrimaryHover() : ThemeUtil.getPrimaryColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setForeground(Color.WHITE);
        loginButton.setContentAreaFilled(false);
        loginButton.setOpaque(false);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { loginButton.repaint(); }
            public void mouseExited(java.awt.event.MouseEvent e)  { loginButton.repaint(); }
        });
        loginButton.addActionListener(e -> attemptLogin());
        gbc.insets = new Insets(8, 40, 10, 40);
        gbc.gridy = 8;
        card.add(loginButton, gbc);

        // Register link — faint secondary color
        registerButton = new JButton("Don't have an account? Create one");
        registerButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        registerButton.setForeground(ThemeUtil.getFgSecondary());
        registerButton.setContentAreaFilled(false);
        registerButton.setBorderPainted(false);
        registerButton.setFocusPainted(false);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { registerButton.setForeground(ThemeUtil.getForegroundColor()); }
            public void mouseExited(java.awt.event.MouseEvent e)  { registerButton.setForeground(ThemeUtil.getFgSecondary()); }
        });
        registerButton.addActionListener(e -> { new RegisterUI(client); dispose(); });
        gbc.insets = new Insets(0, 40, 20, 40);
        gbc.gridy = 9;
        card.add(registerButton, gbc);

        bgPanel.add(card);
        setContentPane(bgPanel);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }
        loginButton.setEnabled(false);
        loginButton.setText("Connecting...");
        new Thread(() -> {
            User user = client.getConnection().login(username, password);
            SwingUtilities.invokeLater(() -> {
                if (user != null) {
                    client.setCurrentUser(user);
                    new ChatUI(client);
                    dispose();
                } else {
                    statusLabel.setText("Invalid username or password.");
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                }
            });
        }).start();
    }

    @Override
    protected void onThemeChanged() {
        getContentPane().repaint();
    }
}
