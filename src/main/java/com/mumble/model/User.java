package com.mumble.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates the user logic and fields.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private boolean online;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.online = false;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
