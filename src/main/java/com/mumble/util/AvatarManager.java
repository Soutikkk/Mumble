package com.mumble.util;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * AvatarManager — central registry for user profile pictures.
 *
 * Features:
 *  - In-memory storage of per-user avatars (BufferedImage)
 *  - Circular clipping of any uploaded image
 *  - Resize to requested size
 *  - File-chooser based upload flow
 *  - Renders a circular avatar panel (JPanel) compatible with any container
 */
public class AvatarManager {

    // ─── Singleton ────────────────────────────────────────────────
    private static final AvatarManager INSTANCE = new AvatarManager();
    public static AvatarManager getInstance() { return INSTANCE; }
    private AvatarManager() {}

    // ─── Avatar storage ───────────────────────────────────────────
    // Maps username → raw (non-clipped) scaled square image
    private final Map<String, BufferedImage> avatarMap = new HashMap<>();

    // ─── API ──────────────────────────────────────────────────────

    /**
     * Opens a JFileChooser and lets the user pick an image for {@code username}.
     * @param parent   The parent component for the file chooser.
     * @param username The user to update.
     * @return true if an image was successfully loaded.
     */
    public boolean pickAndSetAvatar(Component parent, String username) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Profile Picture");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Images (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return false;

        File file = chooser.getSelectedFile();
        try {
            BufferedImage raw = ImageIO.read(file);
            if (raw == null) {
                JOptionPane.showMessageDialog(parent,
                        "Could not read image file.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            setAvatar(username, raw);
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Failed to load image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Stores a raw image for the given user. The image is scaled to 128×128 for storage.
     */
    public void setAvatar(String username, BufferedImage img) {
        avatarMap.put(username, scaleSquare(img, 128));
    }

    /**
     * Returns true if the user has a custom avatar set.
     */
    public boolean hasAvatar(String username) {
        return avatarMap.containsKey(username);
    }

    /**
     * Returns a circular (anti-aliased) avatar image for {@code username} at the given size.
     * If no avatar is set, returns null.
     *
     * @param username Target user
     * @param size     Pixel diameter of the circle
     */
    public BufferedImage getCircularAvatar(String username, int size) {
        BufferedImage src = avatarMap.get(username);
        if (src == null) return null;
        return makeCircular(scaleSquare(src, size), size);
    }

    /**
     * Creates a JPanel that renders a circular avatar for {@code username}.
     * If no custom avatar, falls back to the initial-letter colored circle.
     *
     * @param username   User to render
     * @param size       Diameter in pixels
     * @param fallbackBg Fallback background color (e.g. hash-based avatar color)
     */
    public JPanel buildAvatarPanel(String username, int size, Color fallbackBg) {
        BufferedImage circular = getCircularAvatar(username, size);

        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (circular != null) {
                    // Draw pre-clipped circular image
                    g2.drawImage(circular, 0, 0, size, size, null);
                } else {
                    // Fallback: colored circle with initial letter
                    g2.setColor(fallbackBg);
                    g2.fillOval(0, 0, size - 1, size - 1);
                    g2.setColor(Color.WHITE);
                    String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
                    int fontSize = Math.max(9, (int)(size * 0.45));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (size - fm.stringWidth(initial)) / 2;
                    int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(initial, tx, ty);
                }
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() { return new Dimension(size, size); }
            @Override
            public Dimension getMinimumSize()   { return new Dimension(size, size); }
            @Override
            public Dimension getMaximumSize()   { return new Dimension(size, size); }
        };
    }

    // ─── Internal image helpers ───────────────────────────────────

    /** Scales an image to a square of {@code size × size} pixels. */
    private static BufferedImage scaleSquare(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);

        // Crop-to-square: find the centered square inside src
        int sw = src.getWidth(), sh = src.getHeight();
        int cropSize = Math.min(sw, sh);
        int cx = (sw - cropSize) / 2;
        int cy = (sh - cropSize) / 2;

        g2.drawImage(src, 0, 0, size, size, cx, cy, cx + cropSize, cy + cropSize, null);
        g2.dispose();
        return out;
    }

    /** Applies a circular clip mask to a square image. */
    private static BufferedImage makeCircular(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Float(0, 0, size, size));
        g2.drawImage(src, 0, 0, size, size, null);
        g2.dispose();
        return out;
    }
}
