package com.mumble.util;

import com.mumble.model.TextMessage;

import java.util.*;

/**
 * PinnedMessageManager — stores the set of pinned messages per chat context.
 *
 * Design decisions:
 *  - Per-chat key: "global" for Global Chat, username for DMs, "group:name" for groups.
 *  - Multiple pins allowed per chat (no arbitrary cap).
 *  - Insertion-ordered via LinkedHashSet.
 *  - Pure in-memory: pins are session-local (extend with persistence as needed).
 *
 * Usage:
 *   PinnedMessageManager pm = PinnedMessageManager.getInstance();
 *   pm.pin("alice", message);
 *   pm.unpin("alice", message.getId());
 *   List<TextMessage> pins = pm.getPins("alice");
 *   pm.isPinned("alice", message.getId());
 */
public class PinnedMessageManager {

    // ─── Singleton ────────────────────────────────────────────────
    private static final PinnedMessageManager INSTANCE = new PinnedMessageManager();
    public static PinnedMessageManager getInstance() { return INSTANCE; }
    private PinnedMessageManager() {}

    // ─── Storage: chatKey → ordered set of pinned messages ────────
    // LinkedHashSet preserves pin order while preventing duplicates.
    private final Map<String, LinkedHashSet<TextMessage>> pins = new HashMap<>();

    // ─── Per-chat listener (one listener per chat, for the UI) ────
    public interface PinChangeListener {
        void onPinsChanged(String chatKey);
    }
    private PinChangeListener listener;
    public void setListener(PinChangeListener l) { this.listener = l; }

    // ─── API ──────────────────────────────────────────────────────

    /**
     * Pins a message in the given chat.
     * @param chatKey  Chat identifier (e.g. null → use "global", username, "group:X")
     * @param message  The message to pin.
     */
    public void pin(String chatKey, TextMessage message) {
        String key = resolveKey(chatKey);
        pins.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(message);
        notifyListener(key);
    }

    /**
     * Unpins a message by its ID.
     */
    public void unpin(String chatKey, String messageId) {
        String key = resolveKey(chatKey);
        LinkedHashSet<TextMessage> set = pins.get(key);
        if (set != null) {
            set.removeIf(m -> m.getId().equals(messageId));
            notifyListener(key);
        }
    }

    /**
     * Returns true if the message is pinned in this chat.
     */
    public boolean isPinned(String chatKey, String messageId) {
        LinkedHashSet<TextMessage> set = pins.get(resolveKey(chatKey));
        if (set == null) return false;
        return set.stream().anyMatch(m -> m.getId().equals(messageId));
    }

    /**
     * Returns an unmodifiable, insertion-ordered list of pinned messages for the chat.
     */
    public List<TextMessage> getPins(String chatKey) {
        LinkedHashSet<TextMessage> set = pins.get(resolveKey(chatKey));
        if (set == null || set.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(set));
    }

    /**
     * Returns the total number of pins for a chat.
     */
    public int getPinCount(String chatKey) {
        LinkedHashSet<TextMessage> set = pins.get(resolveKey(chatKey));
        return set == null ? 0 : set.size();
    }

    /**
     * Clears all pins for a given chat.
     */
    public void clearPins(String chatKey) {
        pins.remove(resolveKey(chatKey));
        notifyListener(resolveKey(chatKey));
    }

    // ─── Internal ─────────────────────────────────────────────────

    private String resolveKey(String chatKey) {
        return chatKey == null ? "global" : chatKey;
    }

    private void notifyListener(String key) {
        if (listener != null) listener.onPinsChanged(key);
    }
}
