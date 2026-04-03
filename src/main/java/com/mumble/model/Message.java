package com.mumble.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base abstract class for Messages, demonstrating Abstraction and Inheritance.
 */
public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sender;
    private LocalDateTime timestamp;
    private String type; // e.g. "TEXT", "SYSTEM"

    public Message(String sender, String type) {
        this.sender = sender;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Polymorphic method to get what specifically should be displayed in the UI.
     */
    public abstract String getDisplayContent();
}
