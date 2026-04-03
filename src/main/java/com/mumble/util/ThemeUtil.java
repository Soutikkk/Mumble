package com.mumble.util;

import java.awt.Color;

/**
 * Premium black theme for Mumble.
 * Inspired by Linear / Notion / Discord.
 *
 * Layer stack (dark mode):
 *   #09090b  — deepest background (app canvas)
 *   #111113  — chat area
 *   #18181b  — sidebar
 *   #1c1c1f  — input bar / header
 *   #27272a  — bubble/card surface
 *   #3f3f46  — borders (very subtle)
 *
 * Accent:
 *   #1d4ed8  — primary blue (sent bubbles, Send button, selections)
 *   #1e40af  — hover / darker blue
 *
 * Text:
 *   #fafafa  — primary (pure near-white)
 *   #a1a1aa  — secondary / muted
 *   #52525b  — faint / timestamps / placeholders
 */
public class ThemeUtil {
    public static boolean isDarkMode = true;

    // ─── Dark Mode — Layered Black ───────────────────────────────
    public static final Color DARK_BG            = new Color(0x09, 0x09, 0x0b); // App canvas
    public static final Color DARK_BG_SECONDARY  = new Color(0x11, 0x11, 0x13); // Chat area
    public static final Color DARK_PANEL         = new Color(0x18, 0x18, 0x1b); // Sidebar
    public static final Color DARK_CARD          = new Color(0x27, 0x27, 0x2a); // Received bubble / card
    public static final Color DARK_SURFACE       = new Color(0x1c, 0x1c, 0x1f); // Input bar / header

    public static final Color DARK_FG            = new Color(0xfa, 0xfa, 0xfa); // Primary text
    public static final Color DARK_FG_SECONDARY  = new Color(0xa1, 0xa1, 0xaa); // Muted text
    public static final Color DARK_FG_FAINT      = new Color(0x52, 0x52, 0x5b); // Timestamps / hints

    public static final Color DARK_PRIMARY       = new Color(0x1d, 0x4e, 0xd8); // #1d4ed8 deep blue
    public static final Color DARK_PRIMARY_HOVER = new Color(0x1e, 0x40, 0xaf); // #1e40af darker blue

    // Accent alias (same as primary — no separate cyan accent anymore)
    public static final Color DARK_ACCENT        = new Color(0x1d, 0x4e, 0xd8);

    public static final Color DARK_BORDER        = new Color(0x3f, 0x3f, 0x46); // Subtle zinc border
    public static final Color DARK_INPUT_BG      = new Color(0x1c, 0x1c, 0x1f); // Input background

    public static final Color DARK_SENT_BUBBLE   = new Color(0x1d, 0x4e, 0xd8); // Deep blue sent
    public static final Color DARK_RECV_BUBBLE   = new Color(0x27, 0x27, 0x2a); // Zinc card received

    // Gradient stubs — kept for compat, aliased to flat colors
    public static final Color DARK_GRADIENT_TOP  = new Color(0x18, 0x18, 0x1b);
    public static final Color DARK_GRADIENT_BOT  = new Color(0x09, 0x09, 0x0b);

    public static final Color DARK_ONLINE_GREEN  = new Color(0x22, 0xc5, 0x5e); // #22c55e
    public static final Color DARK_OFFLINE_GRAY  = new Color(0x52, 0x52, 0x5b); // #52525b

    // ─── Light Mode (unchanged functionally, toned down) ────────
    public static final Color LIGHT_BG            = new Color(0xfa, 0xfa, 0xfa);
    public static final Color LIGHT_BG_SECONDARY  = new Color(0xf4, 0xf4, 0xf5);
    public static final Color LIGHT_FG            = new Color(0x09, 0x09, 0x0b);
    public static final Color LIGHT_FG_SECONDARY  = new Color(0x71, 0x71, 0x7a);
    public static final Color LIGHT_FG_FAINT      = new Color(0xa1, 0xa1, 0xaa);
    public static final Color LIGHT_PANEL         = new Color(0xff, 0xff, 0xff);
    public static final Color LIGHT_CARD          = new Color(0xf4, 0xf4, 0xf5);
    public static final Color LIGHT_SURFACE       = new Color(0xf4, 0xf4, 0xf5);
    public static final Color LIGHT_PRIMARY       = new Color(0x1d, 0x4e, 0xd8);
    public static final Color LIGHT_PRIMARY_HOVER = new Color(0x1e, 0x40, 0xaf);
    public static final Color LIGHT_ACCENT        = new Color(0x1d, 0x4e, 0xd8);
    public static final Color LIGHT_BORDER        = new Color(0xe4, 0xe4, 0xe7);
    public static final Color LIGHT_INPUT_BG      = new Color(0xf4, 0xf4, 0xf5);
    public static final Color LIGHT_SENT_BUBBLE   = new Color(0x1d, 0x4e, 0xd8);
    public static final Color LIGHT_RECV_BUBBLE   = new Color(0xf4, 0xf4, 0xf5);
    public static final Color LIGHT_GRADIENT_TOP  = new Color(0xf4, 0xf4, 0xf5);
    public static final Color LIGHT_GRADIENT_BOT  = new Color(0xfa, 0xfa, 0xfa);
    public static final Color LIGHT_ONLINE_GREEN  = new Color(0x16, 0xa3, 0x4a);
    public static final Color LIGHT_OFFLINE_GRAY  = new Color(0xa1, 0xa1, 0xaa);

    // ─── Accessors ───────────────────────────────────────────────
    public static Color getBackgroundColor()   { return isDarkMode ? DARK_BG            : LIGHT_BG; }
    public static Color getBgSecondary()       { return isDarkMode ? DARK_BG_SECONDARY  : LIGHT_BG_SECONDARY; }
    public static Color getSurfaceColor()      { return isDarkMode ? DARK_SURFACE       : LIGHT_SURFACE; }
    public static Color getForegroundColor()   { return isDarkMode ? DARK_FG            : LIGHT_FG; }
    public static Color getFgSecondary()       { return isDarkMode ? DARK_FG_SECONDARY  : LIGHT_FG_SECONDARY; }
    public static Color getFgFaint()           { return isDarkMode ? DARK_FG_FAINT      : LIGHT_FG_FAINT; }
    public static Color getPanelColor()        { return isDarkMode ? DARK_PANEL         : LIGHT_PANEL; }
    public static Color getCardColor()         { return isDarkMode ? DARK_CARD          : LIGHT_CARD; }
    public static Color getPrimaryColor()      { return isDarkMode ? DARK_PRIMARY       : LIGHT_PRIMARY; }
    public static Color getPrimaryHover()      { return isDarkMode ? DARK_PRIMARY_HOVER : LIGHT_PRIMARY_HOVER; }
    public static Color getAccentColor()       { return isDarkMode ? DARK_ACCENT        : LIGHT_ACCENT; }
    public static Color getBorderColor()       { return isDarkMode ? DARK_BORDER        : LIGHT_BORDER; }
    public static Color getInputBg()           { return isDarkMode ? DARK_INPUT_BG      : LIGHT_INPUT_BG; }
    public static Color getSentBubbleColor()   { return isDarkMode ? DARK_SENT_BUBBLE   : LIGHT_SENT_BUBBLE; }
    public static Color getRecvBubbleColor()   { return isDarkMode ? DARK_RECV_BUBBLE   : LIGHT_RECV_BUBBLE; }
    public static Color getGradientTop()       { return isDarkMode ? DARK_GRADIENT_TOP  : LIGHT_GRADIENT_TOP; }
    public static Color getGradientBot()       { return isDarkMode ? DARK_GRADIENT_BOT  : LIGHT_GRADIENT_BOT; }
    public static Color getOnlineColor()       { return isDarkMode ? DARK_ONLINE_GREEN  : LIGHT_ONLINE_GREEN; }
    public static Color getOfflineColor()      { return isDarkMode ? DARK_OFFLINE_GRAY  : LIGHT_OFFLINE_GRAY; }
}
