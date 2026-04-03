package com.mumble.gui;

import com.mumble.util.EmojiUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * EmojiPicker — a self-contained popup panel for emoji selection.
 *
 * Usage:
 *   EmojiPicker picker = new EmojiPicker(inputField, emojiButton);
 *   emojiButton.addActionListener(e -> picker.toggle());
 *
 * The picker inserts the selected emoji at the current caret position
 * of the supplied JTextField.
 */
public class EmojiPicker {

    private final JTextField targetField;
    private final Component anchor;
    private final JPopupMenu popup;

    // Resolved once at construction — avoids repeated font lookups
    private static final Font EMOJI_FONT      = EmojiUtil.getEmojiFont(22f);
    private static final Font CATEGORY_FONT   = new Font("Segoe UI", Font.BOLD, 11);

    // Colors (all sourced from ThemeUtil so they respect theme changes)
    private static final Color BG_POPUP      = new Color(0x18, 0x18, 0x1b); // zinc-900
    private static final Color BG_CELL       = new Color(0x27, 0x27, 0x2a); // zinc-800
    private static final Color BG_CELL_HOVER = new Color(0x3f, 0x3f, 0x46); // zinc-700
    private static final Color BG_TAB_ACT    = new Color(0x1d, 0x4e, 0xd8); // primary blue
    private static final Color BG_TAB_IDLE   = new Color(0x27, 0x27, 0x2a);
    private static final Color FG_TAB_ACT    = Color.WHITE;
    private static final Color FG_TAB_IDLE   = new Color(0xa1, 0xa1, 0xaa);
    private static final Color BORDER_COLOR  = new Color(0x3f, 0x3f, 0x46);

    // How many columns in the emoji grid
    private static final int COLS = 9;

    public EmojiPicker(JTextField targetField, Component anchor) {
        this.targetField = targetField;
        this.anchor = anchor;
        this.popup = buildPopup();
    }

    /** Show or hide the picker relative to the anchor component. */
    public void toggle() {
        if (popup.isVisible()) {
            popup.setVisible(false);
        } else {
            popup.show(anchor, 0, -popup.getPreferredSize().height - 4);
        }
    }

    // ─── Builder ────────────────────────────────────────────────

    private JPopupMenu buildPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        menu.setBackground(BG_POPUP);
        menu.setLayout(new BorderLayout());

        // ── Tab bar ──────────────────────────────────────────────
        JPanel tabBar = new JPanel(new GridLayout(1, EmojiUtil.CATEGORY_NAMES.length, 0, 0));
        tabBar.setBackground(BG_POPUP);
        tabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        // Content area switches per tab
        JPanel contentWrapper = new JPanel(new CardLayout());
        contentWrapper.setBackground(BG_POPUP);
        contentWrapper.setPreferredSize(new Dimension(360, 220));

        // Build one grid panel per category
        for (int ci = 0; ci < EmojiUtil.CATEGORIES.length; ci++) {
            String[] emojis = EmojiUtil.CATEGORIES[ci];
            JPanel grid = buildEmojiGrid(emojis, menu);
            JScrollPane scroll = new JScrollPane(grid);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
            scroll.getViewport().setBackground(BG_POPUP);
            scroll.setBackground(BG_POPUP);
            contentWrapper.add(scroll, EmojiUtil.CATEGORY_NAMES[ci]);
        }

        // Build tab buttons — switch displayed category on click
        ButtonGroup tabGroup = new ButtonGroup();
        for (int ci = 0; ci < EmojiUtil.CATEGORY_NAMES.length; ci++) {
            final String catName = EmojiUtil.CATEGORY_NAMES[ci];

            JToggleButton tab = new JToggleButton(EmojiUtil.CATEGORY_NAMES[ci]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSelected() ? BG_TAB_ACT : BG_TAB_IDLE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            tab.setFont(CATEGORY_FONT);
            tab.setForeground(ci == 0 ? FG_TAB_ACT : FG_TAB_IDLE);
            tab.setContentAreaFilled(false);
            tab.setFocusPainted(false);
            tab.setBorderPainted(false);
            tab.setBorder(new EmptyBorder(6, 8, 6, 8));
            tab.setCursor(new Cursor(Cursor.HAND_CURSOR));
            tab.setSelected(ci == 0);
            tab.addActionListener(e -> {
                ((CardLayout) contentWrapper.getLayout()).show(contentWrapper, catName);
                // Update all tab foregrounds
                for (int i = 0; i < tabBar.getComponentCount(); i++) {
                    JToggleButton t = (JToggleButton) tabBar.getComponent(i);
                    t.setForeground(t.isSelected() ? FG_TAB_ACT : FG_TAB_IDLE);
                    t.repaint();
                }
            });
            tabGroup.add(tab);
            tabBar.add(tab);
        }

        menu.add(tabBar, BorderLayout.NORTH);
        menu.add(contentWrapper, BorderLayout.CENTER);
        return menu;
    }

    /** Builds a grid of emoji buttons for one category. */
    private JPanel buildEmojiGrid(String[] emojis, JPopupMenu menu) {
        int rows = (int) Math.ceil((double) emojis.length / COLS);
        JPanel grid = new JPanel(new GridLayout(rows, COLS, 3, 3));
        grid.setBackground(BG_POPUP);
        grid.setBorder(new EmptyBorder(8, 8, 8, 8));

        for (String emoji : emojis) {
            JButton btn = buildEmojiButton(emoji, menu);
            grid.add(btn);
        }

        // Fill remaining cells for a complete grid
        int remainder = (rows * COLS) - emojis.length;
        for (int i = 0; i < remainder; i++) {
            JPanel spacer = new JPanel();
            spacer.setOpaque(false);
            grid.add(spacer);
        }

        return grid;
    }

    /** Creates a single styled emoji button cell. */
    private JButton buildEmojiButton(String emoji, JPopupMenu menu) {
        JButton btn = new JButton(emoji) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BG_CELL_HOVER : BG_CELL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(EMOJI_FONT);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(4, 4, 4, 4));
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(emoji);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.repaint(); }
            public void mouseExited(MouseEvent e)  { btn.repaint(); }
        });

        btn.addActionListener(e -> {
            insertEmoji(emoji);
            menu.setVisible(false);
            targetField.requestFocus();
        });

        return btn;
    }

    /** Inserts emoji at the current caret position of the target field. */
    private void insertEmoji(String emoji) {
        int pos = targetField.getCaretPosition();
        String text = targetField.getText();
        String newText = text.substring(0, pos) + emoji + text.substring(pos);
        targetField.setText(newText);
        // Advance caret past the inserted emoji (emoji may be 1 or 2 chars due to surrogate pairs)
        int newPos = Math.min(pos + emoji.length(), newText.length());
        targetField.setCaretPosition(newPos);
    }
}
