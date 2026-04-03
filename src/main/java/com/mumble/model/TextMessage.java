package com.mumble.model;

import java.util.UUID;

/**
 * Represents a standard text chat message.
 */
public class TextMessage extends Message {
    private static final long serialVersionUID = 1L;

    private String id;
    private String content;
    private String receiver; // The destination username, or null for broadcast
    private String replyToMessageId; // For replying to a specific message
    private boolean isDelivered;
    private boolean isRead;
    private boolean isDeleted;

    public TextMessage(String sender, String receiver, String content) {
        super(sender, "TEXT");
        this.id = UUID.randomUUID().toString();
        this.receiver = receiver;
        this.content = content;
        this.isDelivered = false;
        this.isRead = false;
        this.isDeleted = false;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    // Required for deserializing existing messages accurately
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getDisplayContent() {
        if (isDeleted) {
            return "This message was deleted";
        }
        return content;
    }
}
