package com.mumble.model;

/**
 * Represents a system event, like typing indicators, online status changes, or read receipts.
 */
public class SystemMessage extends Message {
    private static final long serialVersionUID = 1L;

    private String systemAction; // "TYPING", "STATUS_CHANGE", "M_DELIVERED", "M_READ"
    private String systemContent; 
    private String targetMessageId; // Only applicable mapping if systemAction is receipt update
    private String targetUsername; // Could be the user changing status

    public SystemMessage(String sender, String systemAction, String systemContent) {
        super(sender, "SYSTEM");
        this.systemAction = systemAction;
        this.systemContent = systemContent;
    }

    public String getSystemAction() {
        return systemAction;
    }

    public void setSystemAction(String systemAction) {
        this.systemAction = systemAction;
    }

    public String getSystemContent() {
        return systemContent;
    }

    public void setSystemContent(String systemContent) {
        this.systemContent = systemContent;
    }

    public String getTargetMessageId() {
        return targetMessageId;
    }

    public void setTargetMessageId(String targetMessageId) {
        this.targetMessageId = targetMessageId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    @Override
    public String getDisplayContent() {
        return systemContent; // E.g. "Soutik is typing..."
    }
}
