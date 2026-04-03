package com.mumble.util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Emoji utilities for Mumble.
 *
 * Uses real Unicode emoji codepoints (U+1Fxxx range).
 * Provides font resolution so components can request an emoji-capable font.
 */
public class EmojiUtil {

    // ─── Emoji data by category ───────────────────────────────────
    public static final String[] SMILEYS = {
        "\uD83D\uDE00", // 😀 Grinning
        "\uD83D\uDE02", // 😂 Tears of joy
        "\uD83D\uDE0A", // 😊 Smiling
        "\uD83D\uDE0D", // 😍 Heart eyes
        "\uD83D\uDE18", // 😘 Kissing heart
        "\uD83D\uDE04", // 😄 Grin
        "\uD83D\uDE01", // 😁 Beaming
        "\uD83D\uDE06", // 😆 Squinting
        "\uD83D\uDE05", // 😅 Sweat smile
        "\uD83E\uDD23", // 🤣 ROFL
        "\uD83D\uDE42", // 🙂 Slightly smiling
        "\uD83D\uDE43", // 🙃 Upside down
        "\uD83D\uDE11", // 😑 Expressionless
        "\uD83D\uDE10", // 😐 Neutral
        "\uD83D\uDE36", // 😶 No mouth
        "\uD83D\uDE44", // 🙄 Eye roll
        "\uD83D\uDE2F", // 😯 Hushed
        "\uD83E\uDD14", // 🤔 Thinking
    };

    public static final String[] EMOTIONS = {
        "\uD83D\uDE22", // 😢 Crying
        "\uD83D\uDE2D", // 😭 Loudly crying
        "\uD83D\uDE29", // 😩 Weary
        "\uD83D\uDE28", // 😨 Fearful
        "\uD83D\uDE31", // 😱 Screaming
        "\uD83D\uDE21", // 😡 Angry
        "\uD83D\uDE20", // 😠 Pouting
        "\uD83E\uDD2C", // 🤬 Swearing
        "\uD83D\uDE14", // 😔 Pensive
        "\uD83D\uDE15", // 😕 Confused
        "\uD83D\uDE1F", // 😟 Worried
        "\uD83D\uDE33", // 😳 Flushed
        "\uD83D\uDE08", // 😈 Devil
        "\uD83D\uDCA4", // 💤 Zzz
        "\uD83E\uDD22", // 🤢 Nauseated
        "\uD83E\uDD27", // 🤧 Sneezing
        "\uD83D\uDE37", // 😷 Mask
        "\uD83E\uDD75", // 🥵 Hot face
    };

    public static final String[] HANDS = {
        "\uD83D\uDC4D", // 👍 Thumbs up
        "\uD83D\uDC4E", // 👎 Thumbs down
        "\uD83D\uDC4F", // 👏 Clapping
        "\uD83D\uDE4C", // 🙌 Raising hands
        "\uD83E\uDD1D", // 🤝 Handshake
        "\uD83D\uDE4F", // 🙏 Praying
        "\uD83D\uDC4B", // 👋 Wave
        "\uD83E\uDD1A", // 🤙 Call me
        "\uD83D\uDC46", // 👆 Point up
        "\uD83D\uDC47", // 👇 Point down
        "\uD83D\uDC48", // 👈 Point left
        "\uD83D\uDC49", // 👉 Point right
        "\uD83E\uDD1E", // 🤞 Crossed fingers
        "\uD83D\uDC4C", // 👌 OK
        "\uD83E\uDD23", // 🤣 Pinched fingers (reuse slot)
        "\u270A",       // ✊ Fist
        "\uD83D\uDCAA", // 💪 Muscle
        "\uD83D\uDC50", // 👐 Open hands
    };

    public static final String[] SYMBOLS = {
        "\u2764\uFE0F",  // ❤️ Heart
        "\uD83D\uDC94",  // 💔 Broken heart
        "\uD83D\uDC99",  // 💙 Blue heart
        "\uD83D\uDC9A",  // 💚 Green heart
        "\uD83D\uDC9B",  // 💛 Yellow heart
        "\uD83D\uDC9C",  // 💜 Purple heart
        "\uD83D\uDD25",  // 🔥 Fire
        "\u2B50",        // ⭐ Star
        "\uD83C\uDF89",  // 🎉 Party
        "\uD83C\uDF81",  // 🎁 Gift
        "\uD83D\uDE80",  // 🚀 Rocket
        "\uD83D\uDC4A",  // 👊 Punch
        "\uD83D\uDCE2",  // 📢 Loudspeaker
        "\uD83D\uDC80",  // 💀 Skull
        "\uD83D\uDEAB",  // 🚫 No entry
        "\u2705",        // ✅ Check
        "\u274C",        // ❌ Cross
        "\uD83D\uDEA8",  // 🚨 SOS
    };

    /** Flat list for legacy compat — all categories combined */
    public static final String[] COMMON_EMOJIS = concat(SMILEYS, EMOTIONS, HANDS, SYMBOLS);

    /** Category names (matches category arrays above) */
    public static final String[] CATEGORY_NAMES  = { "Smileys", "Emotions", "Hands", "Symbols" };

    /** Category arrays in order */
    public static final String[][] CATEGORIES = { SMILEYS, EMOTIONS, HANDS, SYMBOLS };

    // ─── Emoji-capable font resolution ───────────────────────────

    /**
     * Returns the best emoji-capable font available on this system.
     * Priority: Segoe UI Emoji (Windows) → Noto Color Emoji (Linux) → Dialog fallback.
     *
     * @param style  Font.PLAIN / Font.BOLD
     * @param size   point size
     */
    public static Font getEmojiFont(int style, float size) {
        String[] candidates = {
            "Segoe UI Emoji",    // Windows 8.1+
            "Noto Color Emoji",  // Linux / Android
            "Apple Color Emoji", // macOS
            "EmojiOne Mozilla",  // Firefox bundled
            "Segoe UI Symbol",   // Older Windows fallback
            "Dialog"             // Always present (may render boxes)
        };
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> available = new java.util.HashSet<>(
            java.util.Arrays.asList(ge.getAvailableFontFamilyNames()));
        for (String name : candidates) {
            if (available.contains(name)) {
                return new Font(name, style, (int) size);
            }
        }
        return new Font("Dialog", style, (int) size);
    }

    /** Derives an emoji font at the given size using the resolved family. */
    public static Font getEmojiFont(float size) {
        return getEmojiFont(Font.PLAIN, size);
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private static String[] concat(String[]... arrays) {
        int total = 0;
        for (String[] a : arrays) total += a.length;
        String[] result = new String[total];
        int pos = 0;
        for (String[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }
}
