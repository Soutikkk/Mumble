package com.mumble.gui;

import com.mumble.client.ChatClient;
import com.mumble.model.Message;
import com.mumble.model.SystemMessage;
import com.mumble.model.TextMessage;
import com.mumble.util.AvatarManager;
import com.mumble.util.EmojiUtil;
import com.mumble.util.PinnedMessageManager;
import com.mumble.util.ThemeUtil;
import com.mumble.util.VoicePlayer;
import com.mumble.util.VoiceRecorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatUI extends BaseUI {
    private final ChatClient client;
    private String currentChatUser = null;
    private final Map<String, Boolean> userStatusMap = new HashMap<>(); 
    
    // Avatar colors — muted, work against black backgrounds
    private static final Color[] AVATAR_COLORS = {
        new Color(79,  84,  170),  // Muted indigo
        new Color(157, 58,  101),  // Muted rose
        new Color(22,  130, 95),   // Muted emerald
        new Color(160, 105, 30),   // Muted amber
        new Color(100, 70,  180),  // Muted violet
        new Color(22,  130, 140),  // Muted teal
        new Color(170, 55,  55),   // Muted red
        new Color(42,  100, 195),  // Muted blue
    };
    
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JPanel messagesPanel;
    private JScrollPane chatScrollPane;
    
    private JTextField inputField;
    private JLabel typingLabel;
    private JPanel leftPanel;
    private JTextField searchField;
    private JLabel chatHeaderLabel;
    private JButton emojiButton;
    
    private Timer typingTimer;
    private boolean isTyping = false;
    private TextMessage replyingTo = null;
    private JLabel replyBanner = null;

    private final List<TextMessage> messages = new ArrayList<>();

    // ─── Feature managers ────────────────────────────────
    private final AvatarManager    avatarManager = AvatarManager.getInstance();
    private final PinnedMessageManager pinnedMgr = PinnedMessageManager.getInstance();

    // ─── Voice recording state ───────────────────────────
    private VoiceRecorder voiceRecorder;
    private boolean isRecordingVoice = false;
    private JButton micButton;

    // ─── Pinned message bar ──────────────────────────────
    private JPanel pinnedBar;
    private JLabel pinnedLabel;
    // The TextMessage IDs in the pinned bar’s current chat for click-scroll
    private TextMessage lastScrolledPin = null;

    // ─── Emoji picker ───────────────────────────────────
    private EmojiPicker emojiPicker;

    // ─── Emoji-capable fonts resolved once ──────────────────
    private static final Font EMOJI_INPUT_FONT = EmojiUtil.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_BODY_FONT  = EmojiUtil.getEmojiFont(Font.PLAIN, 13);

    public ChatUI(ChatClient client) {
        super("Mumble - " + client.getCurrentUser().getUsername());
        this.client = client;
        setSize(1050, 700);
        setLocationRelativeTo(null);
        
        initComponents();
        setupNetworking();
        setVisible(true);
        
        userList.setSelectedIndex(0);
    }
    
    private Color getAvatarColor(String username) {
        return AVATAR_COLORS[Math.abs(username.hashCode()) % AVATAR_COLORS.length];
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.getBackgroundColor());

        // ════════════ LEFT SIDEBAR ════════════
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(255, 0));
        leftPanel.setBackground(ThemeUtil.getPanelColor());
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeUtil.getBorderColor()));

        // Sidebar Header — app name + avatar
        JPanel sidebarHeader = new JPanel(new BorderLayout(8, 0));
        sidebarHeader.setOpaque(true);
        sidebarHeader.setBackground(ThemeUtil.getPanelColor());
        sidebarHeader.setPreferredSize(new Dimension(0, 58));
        sidebarHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeUtil.getBorderColor()),
            new EmptyBorder(8, 14, 8, 14)));

        // FEATURE: Profile Picture — clickable avatar in sidebar header
        String me = client.getCurrentUser().getUsername();
        JPanel selfAvatar = buildSelfAvatarButton(me);
        sidebarHeader.add(selfAvatar, BorderLayout.WEST);

        JLabel appName = new JLabel("Mumble");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        appName.setForeground(ThemeUtil.getForegroundColor());
        sidebarHeader.add(appName, BorderLayout.CENTER);

        // Painted theme toggle icon
        JButton themeBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                if (ThemeUtil.isDarkMode) {
                    g2.setColor(new Color(250, 220, 100));
                    g2.fillOval(cx - 8, cy - 8, 16, 16);
                    g2.setColor(ThemeUtil.getGradientTop());
                    g2.fillOval(cx - 3, cy - 10, 14, 14);
                } else {
                    g2.setColor(new Color(250, 180, 50));
                    g2.fillOval(cx - 6, cy - 6, 12, 12);
                    g2.setStroke(new BasicStroke(1.5f));
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8;
                        g2.drawLine(cx + (int)(9*Math.cos(a)), cy + (int)(9*Math.sin(a)),
                                    cx + (int)(12*Math.cos(a)), cy + (int)(12*Math.sin(a)));
                    }
                }
                g2.dispose();
            }
        };
        themeBtn.setPreferredSize(new Dimension(36, 36));
        themeBtn.setFocusPainted(false);
        themeBtn.setContentAreaFilled(false);
        themeBtn.setBorderPainted(false);
        themeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        themeBtn.setToolTipText("Toggle Theme");
        themeBtn.addActionListener(e -> { toggleTheme(); themeBtn.repaint(); });
        sidebarHeader.add(themeBtn, BorderLayout.EAST);
        leftPanel.add(sidebarHeader, BorderLayout.NORTH);

        // Search + Create Group
        JPanel searchArea = new JPanel(new BorderLayout(0, 6));
        searchArea.setBorder(new EmptyBorder(10, 12, 8, 12));
        searchArea.setBackground(ThemeUtil.getPanelColor());

        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.setOpaque(false);
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBackground(ThemeUtil.getSurfaceColor());
        searchField.setForeground(ThemeUtil.getForegroundColor());
        searchField.setCaretColor(ThemeUtil.getPrimaryColor());
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtil.getBorderColor(), 1, true),
            new EmptyBorder(7, 11, 7, 11)));
        searchField.putClientProperty("JTextField.placeholderText", "Search user...");
        searchField.addActionListener(e -> performSearch());

        // Find button — minimal, surface-colored
        JButton findBtn = new JButton("Find") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ThemeUtil.getPrimaryColor() : ThemeUtil.getSurfaceColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        findBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        findBtn.setForeground(ThemeUtil.getFgSecondary());
        findBtn.setContentAreaFilled(false);
        findBtn.setFocusPainted(false);
        findBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtil.getBorderColor(), 1, true),
            new EmptyBorder(6, 11, 6, 11)));
        findBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        findBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { findBtn.setForeground(Color.WHITE); findBtn.repaint(); }
            public void mouseExited(MouseEvent e)  { findBtn.setForeground(ThemeUtil.getFgSecondary()); findBtn.repaint(); }
        });
        findBtn.addActionListener(e -> performSearch());
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(findBtn, BorderLayout.EAST);
        searchArea.add(searchRow, BorderLayout.NORTH);

        // Create Group — ghost button style
        JButton createGroupBtn = new JButton("+ New Group") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ThemeUtil.getSurfaceColor() : new Color(0,0,0,0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        createGroupBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        createGroupBtn.setForeground(ThemeUtil.getFgSecondary());
        createGroupBtn.setContentAreaFilled(false);
        createGroupBtn.setFocusPainted(false);
        createGroupBtn.setBorder(new EmptyBorder(5, 2, 5, 2));
        createGroupBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createGroupBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { createGroupBtn.setForeground(ThemeUtil.getForegroundColor()); createGroupBtn.repaint(); }
            public void mouseExited(MouseEvent e)  { createGroupBtn.setForeground(ThemeUtil.getFgSecondary()); createGroupBtn.repaint(); }
        });
        createGroupBtn.addActionListener(e -> createGroupChat());
        searchArea.add(createGroupBtn, BorderLayout.SOUTH);

        // User list
        userListModel = new DefaultListModel<>();
        userListModel.addElement("Global Chat");
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setCellRenderer(new UserListRenderer());
        userList.setBackground(ThemeUtil.getPanelColor());
        userList.setFixedCellHeight(48);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    if (selected.equals("Global Chat")) {
                        currentChatUser = null;
                        chatHeaderLabel.setText("# Global Chat");
                    } else if (selected.startsWith("[G] ")) {
                        currentChatUser = "group:" + selected.substring(4);
                        chatHeaderLabel.setText("Group: " + selected.substring(4));
                    } else {
                        currentChatUser = selected;
                        chatHeaderLabel.setText(selected);
                    }
                    replyingTo = null;
                    if (replyBanner != null) replyBanner.setVisible(false);
                    typingLabel.setText(" ");
                    refreshChatArea();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(userList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setBackground(ThemeUtil.getPanelColor());
        listWrapper.add(searchArea, BorderLayout.NORTH);
        listWrapper.add(listScroll, BorderLayout.CENTER);
        leftPanel.add(listWrapper, BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);

        // ════════════ RIGHT PANEL ════════════
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(ThemeUtil.getBackgroundColor());

        // Chat Header — matches surface layer
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setOpaque(true);
        chatHeader.setBackground(ThemeUtil.getSurfaceColor());
        chatHeader.setPreferredSize(new Dimension(0, 52));
        chatHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeUtil.getBorderColor()),
            new EmptyBorder(10, 20, 10, 20)));
        chatHeaderLabel = new JLabel("# Global Chat");
        chatHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chatHeaderLabel.setForeground(ThemeUtil.getForegroundColor());
        chatHeader.add(chatHeaderLabel, BorderLayout.WEST);

        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        typingLabel.setForeground(ThemeUtil.getFgFaint());
        chatHeader.add(typingLabel, BorderLayout.EAST);
        rightPanel.add(chatHeader, BorderLayout.NORTH);

        // FEATURE: Pinned Message Bar (hidden until there are pins)
        pinnedBar = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x1c, 0x1c, 0x1f)); // surface
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ThemeUtil.getBorderColor());
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        pinnedBar.setOpaque(false);
        pinnedBar.setBorder(new EmptyBorder(6, 14, 6, 14));
        pinnedBar.setVisible(false);

        JLabel pinIcon = new JLabel("📌");
        pinIcon.setFont(EmojiUtil.getEmojiFont(13f));
        pinnedBar.add(pinIcon, BorderLayout.WEST);

        pinnedLabel = new JLabel(" ");
        pinnedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pinnedLabel.setForeground(ThemeUtil.getFgSecondary());
        pinnedLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pinnedBar.add(pinnedLabel, BorderLayout.CENTER);

        // Register pin change listener to auto-refresh bar
        pinnedMgr.setListener(key -> SwingUtilities.invokeLater(() -> refreshPinnedBar()));

        // Wrap header + pinned bar in a NORTH compound
        JPanel northStack = new JPanel(new BorderLayout());
        northStack.setOpaque(false);
        northStack.add(chatHeader, BorderLayout.NORTH);
        northStack.add(pinnedBar, BorderLayout.SOUTH);
        rightPanel.add(northStack, BorderLayout.NORTH);

        // Messages area — deepest background layer
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(ThemeUtil.getBackgroundColor());
        messagesPanel.setBorder(new EmptyBorder(14, 16, 14, 16));

        chatScrollPane = new JScrollPane(messagesPanel);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getViewport().setBackground(ThemeUtil.getBackgroundColor());
        // Style scrollbar to be thin and dark
        chatScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        chatScrollPane.getVerticalScrollBar().setBackground(ThemeUtil.getBackgroundColor());
        rightPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Bottom area: reply banner + input bar
        JPanel bottomArea = new JPanel(new BorderLayout());
        bottomArea.setBackground(ThemeUtil.getBackgroundColor()); // match chat area
        bottomArea.setBorder(new EmptyBorder(4, 16, 20, 16));

        // Reply banner
        replyBanner = new JLabel(" ");
        replyBanner.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        replyBanner.setForeground(ThemeUtil.getFgSecondary());
        replyBanner.setBorder(new EmptyBorder(0, 18, 8, 18));
        replyBanner.setVisible(false);
        bottomArea.add(replyBanner, BorderLayout.NORTH);

        // Input Bar — now a rounded floating bar
        JPanel bottomBar = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeUtil.getSurfaceColor()); // zinc dark
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // subtle border
                g2.setColor(ThemeUtil.getBorderColor());
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(6, 12, 6, 12));

        // Attachment button — icon-style inside the bar
        JButton attachBtn = new JButton("+");
        attachBtn.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        attachBtn.setForeground(ThemeUtil.getFgSecondary());
        attachBtn.setContentAreaFilled(false);
        attachBtn.setOpaque(false);
        attachBtn.setFocusPainted(false);
        attachBtn.setBorderPainted(false);
        attachBtn.setBorder(new EmptyBorder(0, 2, 0, 4));
        attachBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        attachBtn.setToolTipText("Attach File");
        attachBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { attachBtn.setForeground(ThemeUtil.getForegroundColor()); }
            public void mouseExited(MouseEvent e)  { attachBtn.setForeground(ThemeUtil.getFgSecondary()); }
        });
        attachBtn.addActionListener(e -> sendFile());
        bottomBar.add(attachBtn, BorderLayout.WEST);

        // Input field — borderless inside the rounded bar
        inputField = new JTextField();
        inputField.setFont(EMOJI_INPUT_FONT);
        inputField.setBackground(new Color(0,0,0,0)); // fully transparent, surface color handles bg
        inputField.setOpaque(false);
        inputField.setForeground(ThemeUtil.getForegroundColor());
        inputField.setCaretColor(ThemeUtil.getPrimaryColor());
        inputField.setBorder(new EmptyBorder(8, 4, 8, 4)); // padding within bar
        inputField.putClientProperty("JTextField.placeholderText", "Type a message...");
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!isTyping) {
                    isTyping = true;
                    SystemMessage typingMsg = new SystemMessage(client.getCurrentUser().getUsername(), "TYPING", "typing...");
                    typingMsg.setTargetUsername(currentChatUser);
                    client.getConnection().sendMessage(typingMsg);
                }
                if (typingTimer != null) typingTimer.restart();
            }
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendMessage();
            }
        });

        typingTimer = new Timer(2000, e -> isTyping = false);
        typingTimer.setRepeats(false);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actionPanel.setOpaque(false);

        // Voice message mic button
        micButton = new JButton("🎤") {  // 🎤
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isRecordingVoice
                    ? new Color(0xdc, 0x26, 0x26)
                    : (getModel().isRollover() ? new Color(0x3f, 0x3f, 0x46) : new Color(0, 0, 0, 0)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        micButton.setFont(EmojiUtil.getEmojiFont(16f));
        micButton.setForeground(Color.WHITE);
        micButton.setContentAreaFilled(false);
        micButton.setFocusPainted(false);
        micButton.setBorderPainted(false);
        micButton.setBorder(new EmptyBorder(4, 7, 4, 7));
        micButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        micButton.setToolTipText("Hold to record voice");
        micButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { startVoiceRecording(); }
            public void mouseReleased(MouseEvent e) { stopAndSendVoice(); }
            public void mouseEntered(MouseEvent e) { micButton.repaint(); }
            public void mouseExited(MouseEvent e)  { micButton.repaint(); }
        });

        emojiButton = new JButton("\uD83D\uDE0A") {  // 😊
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()
                    ? new Color(0x3f, 0x3f, 0x46)  // zinc-700 on hover
                    : new Color(0, 0, 0, 0));      // default no button bg inside bar
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        emojiButton.setFont(EmojiUtil.getEmojiFont(18f));
        emojiButton.setForeground(Color.WHITE);
        emojiButton.setContentAreaFilled(false);
        emojiButton.setFocusPainted(false);
        emojiButton.setBorderPainted(false);
        emojiButton.setBorder(new EmptyBorder(4, 8, 4, 8));
        emojiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        emojiButton.setToolTipText("Emoji");
        emojiButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { emojiButton.repaint(); }
            public void mouseExited(MouseEvent e)  { emojiButton.repaint(); }
        });
        // Wire to EmojiPicker — inputField is ready at this point
        emojiPicker = new EmojiPicker(inputField, emojiButton);
        emojiButton.addActionListener(e -> emojiPicker.toggle());

        // Send button — solid deep blue, hover darkens
        JButton sendButton = new JButton("Send") {
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
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendButton.setForeground(Color.WHITE);
        sendButton.setContentAreaFilled(false);
        sendButton.setOpaque(false);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(new EmptyBorder(6, 18, 6, 18));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendMessage());
        sendButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { sendButton.repaint(); }
            public void mouseExited(MouseEvent e)  { sendButton.repaint(); }
        });

        actionPanel.add(micButton);
        actionPanel.add(emojiButton);
        actionPanel.add(sendButton);

        bottomBar.add(inputField, BorderLayout.CENTER);
        bottomBar.add(actionPanel, BorderLayout.EAST);
        bottomArea.add(bottomBar, BorderLayout.CENTER);
        rightPanel.add(bottomArea, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);
    }
    
    // ═══════ FILE SHARING ═══════
    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select file to send");
        chooser.setFileFilter(new FileNameExtensionFilter("All supported files", "pdf", "txt", "jpg", "jpeg", "png", "gif", "doc", "docx", "zip"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.length() > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "File too large (max 5MB).", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String encoded = Base64.getEncoder().encodeToString(bytes);
                String content = "[FILE:" + file.getName() + ":" + file.length() + ":" + encoded + "]";
                TextMessage tm = new TextMessage(client.getCurrentUser().getUsername(), currentChatUser, content);
                client.getConnection().sendMessage(tm);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Could not read file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private boolean isFileMessage(String content) {
        return content != null && content.startsWith("[FILE:") && content.endsWith("]");
    }
    
    private String getFileName(String content) {
        // [FILE:name:size:data]
        String inner = content.substring(6, content.length() - 1);
        return inner.split(":", 3)[0];
    }
    
    private void saveFileFromMessage(String content) {
        String inner = content.substring(6, content.length() - 1);
        String[] parts = inner.split(":", 3);
        String fileName = parts[0];
        String base64Data = parts[2];
        
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(fileName));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                Files.write(chooser.getSelectedFile().toPath(), bytes);
                JOptionPane.showMessageDialog(this, "File saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not save file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performSearch() {
        String searchUser = searchField.getText().trim();
        if (searchUser.isEmpty()) return;
        if (searchUser.equals(client.getCurrentUser().getUsername())) {
            JOptionPane.showMessageDialog(this, "You cannot chat with yourself.", "Invalid", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!userListModel.contains(searchUser)) userListModel.addElement(searchUser);
        userList.setSelectedValue(searchUser, true);
        searchField.setText("");
    }

    private void createGroupChat() {
        String groupName = JOptionPane.showInputDialog(this, "Enter group name:", "Create Group", JOptionPane.PLAIN_MESSAGE);
        if (groupName == null || groupName.trim().isEmpty()) return;
        String membersInput = JOptionPane.showInputDialog(this, "Enter usernames (comma-separated):", "Add Members", JOptionPane.PLAIN_MESSAGE);
        if (membersInput == null || membersInput.trim().isEmpty()) return;
        SystemMessage createMsg = new SystemMessage(client.getCurrentUser().getUsername(), "CREATE_GROUP", groupName.trim());
        createMsg.setTargetUsername(membersInput.trim());
        client.getConnection().sendMessage(createMsg);
    }

    private void setupNetworking() {
        client.getConnection().setOnMessageReceived(msg -> SwingUtilities.invokeLater(() -> handleIncomingMessage(msg)));
        client.getConnection().setOnDisconnected(() -> SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, "Disconnected from server.", "Error", JOptionPane.ERROR_MESSAGE)));
    }

    // ═══════ NOTIFICATIONS ═══════
    private void showNotification(String sender, String preview) {
        // Don't notify if this chat is currently focused
        if (currentChatUser != null && currentChatUser.equals(sender)) return;
        if (currentChatUser == null && sender == null) return; // global, already viewing
        
        // Create a floating notification
        JWindow notifWindow = new JWindow();
        JPanel notifPanel = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeUtil.getPanelColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(ThemeUtil.getAccentColor());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        notifPanel.setOpaque(false);
        notifPanel.setBorder(new EmptyBorder(10, 14, 10, 14));
        
        // Avatar circle
        JPanel avatar = createAvatarCircle(sender != null ? sender : "G", 28);
        notifPanel.add(avatar, BorderLayout.WEST);
        
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("New message from " + (sender != null ? sender : "Global"));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(ThemeUtil.getForegroundColor());
        JLabel previewLabel = new JLabel(preview.length() > 40 ? preview.substring(0, 40) + "..." : preview);
        previewLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        previewLabel.setForeground(ThemeUtil.getFgSecondary());
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(previewLabel, BorderLayout.SOUTH);
        notifPanel.add(textPanel, BorderLayout.CENTER);
        
        notifWindow.setContentPane(notifPanel);
        notifWindow.setSize(320, 60);
        
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        notifWindow.setLocation(screen.width - 340, 20);
        notifWindow.setAlwaysOnTop(true);
        notifWindow.setVisible(true);
        
        // Auto dismiss after 3 seconds
        Timer dismiss = new Timer(3000, e -> notifWindow.dispose());
        dismiss.setRepeats(false);
        dismiss.start();
    }

    private void handleIncomingMessage(Message msg) {
        if (msg instanceof TextMessage) {
            TextMessage tm = (TextMessage) msg;
            boolean isUpdate = false;
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getId().equals(tm.getId())) {
                    messages.set(i, tm);
                    isUpdate = true;
                    break;
                }
            }
            if (!isUpdate) {
                messages.add(tm);
                // Auto receipts for private messages TO me
                String me = client.getCurrentUser().getUsername();
                if (tm.getReceiver() != null && !tm.getReceiver().startsWith("group:") &&
                    tm.getReceiver().equals(me) && !tm.getSender().equals(me)) {
                    if (currentChatUser != null && currentChatUser.equals(tm.getSender())) {
                        tm.setRead(true); tm.setDelivered(true);
                    } else {
                        tm.setDelivered(true);
                    }
                    client.getConnection().sendMessage(tm);
                    
                    // Show notification for messages not in current view
                    if (currentChatUser == null || !currentChatUser.equals(tm.getSender())) {
                        String preview = isFileMessage(tm.getContent()) ? "Sent a file" : tm.getContent();
                        showNotification(tm.getSender(), preview);
                    }
                }
                // Global message notification
                if (tm.getReceiver() == null && !tm.getSender().equals(me) && currentChatUser != null) {
                    showNotification(null, tm.getSender() + ": " + tm.getContent());
                }
            }
            // Add user to sidebar
            if (tm.getReceiver() != null && !tm.getReceiver().startsWith("group:")) {
                String me = client.getCurrentUser().getUsername();
                String otherUser = tm.getSender().equals(me) ? tm.getReceiver() : tm.getSender();
                if (!otherUser.equals(me) && !userListModel.contains(otherUser)) userListModel.addElement(otherUser);
            }
            refreshChatArea();
            
        } else if (msg instanceof SystemMessage) {
            SystemMessage sm = (SystemMessage) msg;
            switch (sm.getSystemAction()) {
                case "STATUS_CHANGE":
                    String username = sm.getTargetUsername();
                    if (username != null && !username.equals(client.getCurrentUser().getUsername())) {
                        userStatusMap.put(username, sm.getSystemContent().contains("online"));
                        if (!userListModel.contains(username)) userListModel.addElement(username);
                        userList.repaint();
                    }
                    break;
                case "TYPING":
                    handleTypingIndicator(sm);
                    break;
                case "GROUP_CREATED":
                    String gName = sm.getSystemContent();
                    String displayName = "[G] " + gName;
                    if (!userListModel.contains(displayName)) userListModel.addElement(displayName);
                    break;
            }
        }
    }

    private void handleTypingIndicator(SystemMessage sm) {
        String typer = sm.getSender();
        String target = sm.getTargetUsername();
        boolean showTyping = false;
        if (currentChatUser == null && target == null) showTyping = true;
        else if (currentChatUser != null && (currentChatUser.equals(target) || currentChatUser.equals(typer))) showTyping = true;
        if (showTyping && replyingTo == null) {
            String name = typer.equals("SERVER") ? target : typer;
            typingLabel.setText(name + " is typing...");
            Timer t = new Timer(2500, e -> { if (replyingTo == null) typingLabel.setText(" "); });
            t.setRepeats(false);
            t.start();
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        TextMessage tm = new TextMessage(client.getCurrentUser().getUsername(), currentChatUser, text);
        if (replyingTo != null) {
            tm.setReplyToMessageId(replyingTo.getId());
            replyingTo = null;
            if (replyBanner != null) replyBanner.setVisible(false);
        }
        client.getConnection().sendMessage(tm);
        inputField.setText("");
        isTyping = false;
        if (typingTimer != null) typingTimer.stop();
    }

    private void refreshChatArea() {
        messagesPanel.removeAll();
        String me = client.getCurrentUser().getUsername();
        String lastSender = null;
        java.time.LocalDateTime lastTime = null;

        for (TextMessage tm : messages) {
            boolean show = false;
            String receiver = tm.getReceiver();
            if (currentChatUser == null && receiver == null) show = true;
            else if (currentChatUser != null && receiver != null) {
                if (currentChatUser.startsWith("group:") && receiver.equals(currentChatUser)) show = true;
                else if (!currentChatUser.startsWith("group:")) {
                    if ((tm.getSender().equals(me) && receiver.equals(currentChatUser)) ||
                        (tm.getSender().equals(currentChatUser) && receiver.equals(me))) {
                        show = true;
                        if (tm.getSender().equals(currentChatUser) && receiver.equals(me) && !tm.isRead()) {
                            tm.setRead(true);
                            client.getConnection().sendMessage(tm);
                        }
                    }
                }
            }
            if (show) {
                // Group messages from same sender within 5 minutes
                boolean isConsecutive = lastSender != null && lastSender.equals(tm.getSender()) &&
                                        lastTime != null && java.time.Duration.between(lastTime, tm.getTimestamp()).toMinutes() < 5;
                
                messagesPanel.add(Box.createVerticalStrut(isConsecutive ? 2 : 14));
                messagesPanel.add(createMessageBubble(tm, isConsecutive));
                
                lastSender = tm.getSender();
                lastTime = tm.getTimestamp();
            }
        }
        messagesPanel.revalidate();
        messagesPanel.repaint();
        SwingUtilities.invokeLater(() -> chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum()));
    }



    // ─── Message Bubble ───────────────────────────────
    private JPanel createMessageBubble(TextMessage tm, boolean isConsecutive) {
        boolean isMe = tm.getSender().equals(client.getCurrentUser().getUsername());
        boolean isFile = !tm.isDeleted() && isFileMessage(tm.getContent());
        
        JPanel bubble = new JPanel() {
            private boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color bg = isMe ? ThemeUtil.getSentBubbleColor() : ThemeUtil.getRecvBubbleColor();
                if (isHovered) {
                    // Slight highlight on hover
                    bg = isMe ? ThemeUtil.getPrimaryHover() : new Color(0x2d, 0x2d, 0x30);
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                if (!isMe) {
                    g2.setColor(isHovered ? ThemeUtil.getAccentColor() : ThemeUtil.getBorderColor());
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                }
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BorderLayout(0, 2));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Sender name (only for first message in group, and only in non-private chats)
        if (!isMe && !isConsecutive && (currentChatUser == null || currentChatUser.startsWith("group:"))) {
            JLabel sender = new JLabel(tm.getSender());
            sender.setFont(new Font("Segoe UI", Font.BOLD, 11));
            sender.setForeground(ThemeUtil.getAccentColor()); // Use accent color for names to pop
            sender.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(sender);
            content.add(Box.createVerticalStrut(2));
        }

        // Reply context
        if (tm.getReplyToMessageId() != null && !tm.isDeleted()) {
            JPanel replyBox = new JPanel(new BorderLayout());
            replyBox.setOpaque(false);
            replyBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ThemeUtil.getAccentColor()),
                new EmptyBorder(3, 8, 3, 8)));
            replyBox.setMaximumSize(new Dimension(300, 40));
            JLabel replyText = new JLabel(getMessageContentById(tm.getReplyToMessageId()));
            replyText.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            replyText.setForeground(isMe ? new Color(200, 210, 255) : ThemeUtil.getFgSecondary());
            replyBox.add(replyText, BorderLayout.CENTER);
            replyBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(replyBox);
            content.add(Box.createVerticalStrut(3));
        }

        // Body
        boolean isVoice = !tm.isDeleted() && isVoiceMsgMessage(tm.getContent());
        if (isVoice) {
            JPanel voicePanel = buildVoiceContent(tm.getContent(), isMe);
            voicePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(voicePanel);
        } else if (isFile) {
            String fileName = getFileName(tm.getContent());
            String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
            String icon = "jpg,jpeg,png,gif".contains(ext) ? "🖼" : "📎";

            JLabel fileLabel = new JLabel(icon + " " + fileName);
            fileLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            fileLabel.setForeground(isMe ? Color.WHITE : ThemeUtil.getAccentColor());
            fileLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            fileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            fileLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { saveFileFromMessage(tm.getContent()); }
            });
            content.add(fileLabel);
        } else {
            Font bodyFont = tm.isDeleted() ? EMOJI_BODY_FONT.deriveFont(Font.ITALIC) : EMOJI_BODY_FONT;
            // 430px wide is roughly 55-60% of typical chat area (700-800px)
            JLabel body = new JLabel("<html><div style='width:430px; line-height:1.2; font-family: Segoe UI;'>" + 
                                   escapeHtml(tm.getDisplayContent()) + "</div></html>");
            body.setFont(bodyFont);
            body.setForeground(isMe ? Color.WHITE : (tm.isDeleted() ? ThemeUtil.getFgSecondary() : ThemeUtil.getForegroundColor()));
            body.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(body);
        }
        bubble.add(content, BorderLayout.CENTER);

        // Footer: time + ticks (always bottom-right for cleaner look)
        String timeStr = tm.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        footer.setOpaque(false);
        
        JLabel timeLabel = new JLabel(timeStr);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        // #6b7280 equivalent for timestamp
        timeLabel.setForeground(new Color(107, 114, 128)); 
        footer.add(timeLabel);

        if (isMe && !tm.isDeleted()) {
            JPanel tickPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setStroke(new BasicStroke(1.2f));
                    if (tm.isRead()) {
                        g2.setColor(new Color(96, 165, 250)); // Bright blue for read
                        drawTick(g2, 2, 5); drawTick(g2, 8, 5);
                    } else if (tm.isDelivered()) {
                        g2.setColor(new Color(156, 163, 175)); // Gray for delivered
                        drawTick(g2, 2, 5); drawTick(g2, 8, 5);
                    } else {
                        g2.setColor(new Color(107, 114, 128)); // Dim for sent
                        drawTick(g2, 5, 5);
                    }
                    g2.dispose();
                }
                private void drawTick(Graphics2D g2, int x, int y) {
                    g2.drawLine(x, y, x+3, y+3); g2.drawLine(x+3, y+3, x+8, y-2);
                }
            };
            tickPanel.setOpaque(false);
            tickPanel.setPreferredSize(new Dimension(20, 10));
            footer.add(tickPanel);
        }
        bubble.add(footer, BorderLayout.SOUTH);

        // Wrapper/Alignment
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);
        
        Dimension bSize = bubble.getPreferredSize();
        // Constrain bubble max width (around 480px)
        int w = Math.min(480, bSize.width + 10);
        bubble.setMaximumSize(new Dimension(w, bSize.height));
        bubble.setPreferredSize(new Dimension(w, bSize.height));

        if (isMe) {
            wrapper.add(Box.createHorizontalGlue());
            wrapper.add(bubble);
            // Smaller offset if consecutive
            wrapper.add(Box.createHorizontalStrut(isConsecutive ? 8 : 12));
        } else {
            wrapper.add(Box.createHorizontalStrut(8));
            if (!isConsecutive) {
                JPanel avatarPanel = createAvatarCircle(tm.getSender(), 32);
                avatarPanel.setBorder(new EmptyBorder(0, 0, 0, 8));
                wrapper.add(avatarPanel);
            } else {
                // Placeholder space where avatar would be
                wrapper.add(Box.createHorizontalStrut(40));
            }
            wrapper.add(bubble);
            wrapper.add(Box.createHorizontalGlue());
        }
        
        // Context menu and other listeners...
        setupBubbleMenu(bubble, tm);

        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, bSize.height));
        return wrapper;
    }

    private void setupBubbleMenu(JPanel bubble, TextMessage tm) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem replyItem = new JMenuItem("Reply");
        replyItem.addActionListener(e -> {
            replyingTo = tm;
            replyBanner.setText("Replying to " + tm.getSender() + ": " + (isFileMessage(tm.getContent()) ? "file" : tm.getContent()));
            replyBanner.setVisible(true);
            inputField.requestFocus();
        });
        popup.add(replyItem);

        boolean currentlyPinned = pinnedMgr.isPinned(currentChatUser, tm.getId());
        JMenuItem pinItem = new JMenuItem(currentlyPinned ? "📌 Unpin" : "📌 Pin");
        pinItem.addActionListener(e -> {
            if (currentlyPinned) pinnedMgr.unpin(currentChatUser, tm.getId());
            else pinnedMgr.pin(currentChatUser, tm);
            refreshChatArea();
        });
        popup.add(pinItem);

        if (tm.getSender().equals(client.getCurrentUser().getUsername()) && !tm.isDeleted()) {
            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(e -> { tm.setDeleted(true); client.getConnection().sendMessage(tm); });
            popup.add(deleteItem);
        }
        
        boolean isFile = !tm.isDeleted() && isFileMessage(tm.getContent());
        if (isFile) {
            JMenuItem saveItem = new JMenuItem("Save File");
            saveItem.addActionListener(e -> saveFileFromMessage(tm.getContent()));
            popup.add(saveItem);
        }
        
        bubble.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) popup.show(e.getComponent(), e.getX(), e.getY()); }
            public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) popup.show(e.getComponent(), e.getX(), e.getY()); }
        });
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String getMessageContentById(String id) {
        for (TextMessage m : messages) {
            if (m.getId().equals(id)) {
                if (isFileMessage(m.getContent())) return "File: " + getFileName(m.getContent());
                return m.getContent();
            }
        }
        return "...";
    }

    @Override
    protected void onThemeChanged() {
        leftPanel.setBackground(ThemeUtil.getPanelColor());
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeUtil.getBorderColor()));
        userList.setBackground(ThemeUtil.getPanelColor());
        userList.setCellRenderer(new UserListRenderer());
        messagesPanel.setBackground(ThemeUtil.getBackgroundColor());
        chatScrollPane.getViewport().setBackground(ThemeUtil.getBackgroundColor());
        getContentPane().setBackground(ThemeUtil.getBackgroundColor());
        refreshChatArea();
    }

    // ─── Sidebar User List Renderer ────────────────────────────
    class UserListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String name = value.toString();

            JPanel cell = new JPanel(new BorderLayout(10, 0));
            cell.setBorder(new EmptyBorder(6, 10, 6, 10));

            if (name.equals("Global Chat")) {
                // Round # icon
                JPanel hashIcon = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        // Solid primary circle
                        g2.setColor(isSelected ? new Color(0xff,0xff,0xff,40) : ThemeUtil.getPrimaryColor());
                        g2.fillRoundRect(0, 2, 32, 32, 8, 8);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                        g2.drawString("#", 10, 24);
                        g2.dispose();
                    }
                };
                hashIcon.setOpaque(false);
                hashIcon.setPreferredSize(new Dimension(34, 36));
                cell.add(hashIcon, BorderLayout.WEST);

                JLabel nameLabel = new JLabel("Global Chat");
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                nameLabel.setForeground(isSelected ? Color.WHITE : ThemeUtil.getForegroundColor());
                cell.add(nameLabel, BorderLayout.CENTER);

            } else if (name.startsWith("[G] ")) {
                // Group icon
                JPanel groupIcon = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(0x6b, 0x72, 0x80));
                        g2.fillRoundRect(0, 2, 32, 32, 8, 8);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                        g2.drawString("G", 10, 24);
                        g2.dispose();
                    }
                };
                groupIcon.setOpaque(false);
                groupIcon.setPreferredSize(new Dimension(34, 36));
                cell.add(groupIcon, BorderLayout.WEST);

                JLabel nameLabel = new JLabel(name.substring(4));
                nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                nameLabel.setForeground(isSelected ? Color.WHITE : ThemeUtil.getForegroundColor());
                cell.add(nameLabel, BorderLayout.CENTER);

            } else {
                // User avatar circle + online dot
                boolean isOnline = userStatusMap.getOrDefault(name, false);
                JPanel avatarWithDot = new JPanel(null) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        // Avatar circle (solid, deterministic color)
                        Color aColor = getAvatarColor(name);
                        g2.setColor(aColor);
                        g2.fillOval(0, 2, 32, 32);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                        String init = name.substring(0, 1).toUpperCase();
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(init, (32 - fm.stringWidth(init)) / 2,
                                      2 + (32 - fm.getHeight()) / 2 + fm.getAscent());
                        // Status dot (bottom-right of avatar)
                        Color dotColor = isOnline ? ThemeUtil.getOnlineColor() : ThemeUtil.getOfflineColor();
                        Color bg = isSelected ? ThemeUtil.getSurfaceColor() : ThemeUtil.getPanelColor();
                        g2.setColor(dotColor);
                        g2.fillOval(22, 24, 10, 10);
                        g2.setColor(bg);
                        g2.setStroke(new BasicStroke(2.5f));
                        g2.drawOval(22, 24, 10, 10);
                        g2.dispose();
                    }
                };
                avatarWithDot.setOpaque(false);
                avatarWithDot.setPreferredSize(new Dimension(36, 36));
                cell.add(avatarWithDot, BorderLayout.WEST);

                // Name + status text
                JPanel textCol = new JPanel();
                textCol.setOpaque(false);
                textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
                JLabel nameLabel = new JLabel(name);
                nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                nameLabel.setForeground(isSelected ? Color.WHITE : ThemeUtil.getForegroundColor());
                JLabel statusTxt = new JLabel(isOnline ? "Online" : "Offline");
                statusTxt.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                statusTxt.setForeground(isOnline ? ThemeUtil.getOnlineColor() : ThemeUtil.getFgSecondary());
                textCol.add(nameLabel);
                textCol.add(statusTxt);
                cell.add(textCol, BorderLayout.CENTER);
            }

            // Selected: barely-visible dark surface highlight — no bright blue.
            if (isSelected) {
                cell.setBackground(ThemeUtil.getSurfaceColor());  // One step lighter than sidebar
            } else {
                cell.setBackground(ThemeUtil.getPanelColor());
            }
            cell.setOpaque(true);
            return cell;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FEATURE 1: Profile Picture — Self Avatar Button
    // ═══════════════════════════════════════════════════════════

    /**
     * Builds the clickable self-avatar panel shown in the sidebar header.
     * Click opens a file chooser to change the profile picture.
     */
    private JPanel buildSelfAvatarButton(String username) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() { return new Dimension(38, 38); }
        };
        wrapper.setOpaque(false);
        wrapper.setCursor(new Cursor(Cursor.HAND_CURSOR));
        wrapper.setToolTipText("Change profile picture");
        refreshSelfAvatarPanel(wrapper, username);
        wrapper.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (avatarManager.pickAndSetAvatar(ChatUI.this, username)) {
                    refreshSelfAvatarPanel(wrapper, username);
                    userList.repaint(); // refresh sidebar avatars
                    refreshChatArea();  // refresh bubble avatars
                }
            }
        });
        return wrapper;
    }

    private void refreshSelfAvatarPanel(JPanel wrapper, String username) {
        wrapper.removeAll();
        JPanel avatar = avatarManager.buildAvatarPanel(username, 36, getAvatarColor(username));
        avatar.setOpaque(false);
        wrapper.add(avatar, BorderLayout.CENTER);
        wrapper.revalidate();
        wrapper.repaint();
    }

    /** Override createAvatarCircle to use AvatarManager if a custom pic is set. */
    private JPanel createAvatarCircle(String name, int size) {
        JPanel panel = avatarManager.buildAvatarPanel(name, size, getAvatarColor(name));
        panel.setOpaque(false);
        panel.setBorder(null);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    // FEATURE 2: Voice Messages
    // ═══════════════════════════════════════════════════════════

    private void startVoiceRecording() {
        if (isRecordingVoice) return;
        try {
            voiceRecorder = new VoiceRecorder();
            voiceRecorder.startRecording();
            isRecordingVoice = true;
            typingLabel.setText("🎤 Recording...");  // visual indicator
            micButton.repaint();
        } catch (Exception ex) {
            typingLabel.setText("Mic unavailable");
            isRecordingVoice = false;
        }
    }

    private void stopAndSendVoice() {
        if (!isRecordingVoice || voiceRecorder == null) return;
        isRecordingVoice = false;
        micButton.repaint();
        typingLabel.setText(" ");

        byte[] wav = voiceRecorder.stopAndGetWav();
        if (wav == null || wav.length == 0) return;

        // Encode WAV as Base64 and embed in a TextMessage content string
        String encoded = Base64.getEncoder().encodeToString(wav);
        double duration = VoiceRecorder.wavDurationSeconds(wav);
        String durStr = String.format("%.1f", duration);
        String content = "[VOICE:" + durStr + ":" + encoded + "]";

        TextMessage tm = new TextMessage(
                client.getCurrentUser().getUsername(), currentChatUser, content);
        client.getConnection().sendMessage(tm);
        voiceRecorder = null;
    }

    private boolean isVoiceMsgMessage(String content) {
        return content != null && content.startsWith("[VOICE:") && content.endsWith("]");
    }

    /** Builds the playable voice message bubble content panel. */
    private JPanel buildVoiceContent(String content, boolean isMe) {
        // Parse duration and base64 data
        String inner = content.substring(7, content.length() - 1); // strip [VOICE: and ]
        int colon = inner.indexOf(':');
        String durStr = colon > 0 ? inner.substring(0, colon) : "0.0";
        String b64    = colon > 0 ? inner.substring(colon + 1) : inner;

        byte[] wav;
        try { wav = Base64.getDecoder().decode(b64); }
        catch (Exception ex) { wav = new byte[0]; }
        final byte[] wavFinal = wav;

        // Build panel: mic icon + duration label + play/stop button
        JPanel vc = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        vc.setOpaque(false);

        JLabel micLbl = new JLabel("🎤");
        micLbl.setFont(EmojiUtil.getEmojiFont(16f));
        vc.add(micLbl);

        JLabel durLbl = new JLabel(durStr + "s");
        durLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        durLbl.setForeground(isMe ? new Color(200, 215, 255) : ThemeUtil.getFgSecondary());
        vc.add(durLbl);

        // Play / Stop toggle button
        JButton playBtn = new JButton("▶") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isMe ? new Color(0xff, 0xff, 0xff, 40) : new Color(0x3f, 0x3f, 0x46));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        playBtn.setFont(EmojiUtil.getEmojiFont(14f));
        playBtn.setForeground(Color.WHITE);
        playBtn.setContentAreaFilled(false);
        playBtn.setFocusPainted(false);
        playBtn.setBorderPainted(false);
        playBtn.setBorder(new EmptyBorder(3, 8, 3, 8));
        playBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hold reference for stop
        VoicePlayer[] playerRef = { null };
        playBtn.addActionListener(e -> {
            if (playerRef[0] != null && playerRef[0].isPlaying()) {
                playerRef[0].stop();
                playBtn.setText("▶");
                return;
            }
            VoicePlayer vp = new VoicePlayer(wavFinal);
            playerRef[0] = vp;
            vp.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> playBtn.setText("▶")));
            vp.play();
            playBtn.setText("⏹");  // ⏹ while playing
        });

        vc.add(playBtn);
        return vc;
    }

    // ═══════════════════════════════════════════════════════════
    // FEATURE 3: Pin Messages — Pinned Bar Refresh
    // ═══════════════════════════════════════════════════════════

    /**
     * Rebuilds the pinned message bar from PinnedMessageManager.
     * Shows the most recently pinned message. Clicking cycles through pins
     * and scrolls the messages panel to the original bubble.
     */
    private void refreshPinnedBar() {
        java.util.List<TextMessage> pins = pinnedMgr.getPins(currentChatUser);
        if (pins.isEmpty()) {
            pinnedBar.setVisible(false);
            return;
        }
        // Show latest pin summary
        TextMessage latest = pins.get(pins.size() - 1);
        String preview = isVoiceMsgMessage(latest.getContent()) ? "Voice message"
                       : isFileMessage(latest.getContent())    ? "File: " + getFileName(latest.getContent())
                       : latest.getContent();
        if (preview.length() > 55) preview = preview.substring(0, 52) + "...";
        int count = pins.size();
        pinnedLabel.setText(count > 1
            ? "[" + count + " pins] " + preview
            : preview);

        // Click: cycle through pins and scroll to each one
        for (java.awt.event.MouseListener ml : pinnedLabel.getMouseListeners()) pinnedLabel.removeMouseListener(ml);
        pinnedLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                java.util.List<TextMessage> current = pinnedMgr.getPins(currentChatUser);
                if (current.isEmpty()) return;
                // Find which one to scroll to next
                int idx = 0;
                if (lastScrolledPin != null) {
                    for (int i = 0; i < current.size(); i++) {
                        if (current.get(i).getId().equals(lastScrolledPin.getId())) {
                            idx = (i + 1) % current.size();
                            break;
                        }
                    }
                }
                lastScrolledPin = current.get(idx);
                scrollToMessage(lastScrolledPin);
            }
        });

        pinnedBar.setVisible(true);
        pinnedBar.revalidate();
        pinnedBar.repaint();
    }

    /**
     * Scrolls the chat scrollpane to the bubble that contains the given TextMessage.
     * We locate the bubble by index in messages vs. rendered components in messagesPanel.
     */
    private void scrollToMessage(TextMessage target) {
        // Count which rendered position this message is at
        String me = client.getCurrentUser().getUsername();
        int renderIdx = 0;
        for (TextMessage tm : messages) {
            if (!isMessageVisibleInCurrentChat(tm, me)) continue;
            if (tm.getId().equals(target.getId())) break;
            renderIdx++;
        }
        // messagesPanel children: strut, bubble (2 per message)
        int compIdx = renderIdx * 2 + 1;
        Component[] comps = messagesPanel.getComponents();
        if (compIdx < comps.length) {
            comps[compIdx].requestFocusInWindow();
            Rectangle bounds = comps[compIdx].getBounds();
            chatScrollPane.getViewport().setViewPosition(new Point(0, Math.max(0, bounds.y - 20)));
        }
    }

    /** Helper used by scrollToMessage to test visibility without re-running full logic. */
    private boolean isMessageVisibleInCurrentChat(TextMessage tm, String me) {
        String receiver = tm.getReceiver();
        if (currentChatUser == null && receiver == null) return true;
        if (currentChatUser != null && receiver != null) {
            if (currentChatUser.startsWith("group:") && receiver.equals(currentChatUser)) return true;
            if (!currentChatUser.startsWith("group:")) {
                return (tm.getSender().equals(me) && receiver.equals(currentChatUser))
                    || (tm.getSender().equals(currentChatUser) && receiver.equals(me));
            }
        }
        return false;
    }
}
