package com.mumble.client;

import com.mumble.gui.LoginUI;
import com.mumble.model.User;

import javax.swing.SwingUtilities;

public class ChatClient {
    private ClientConnection connection;
    private User currentUser;

    public ChatClient() {
        connection = new ClientConnection();
    }
    
    public void start() {
        if (connection.connect()) {
            SwingUtilities.invokeLater(() -> new LoginUI(this));
        } else {
            System.err.println("Failed to connect to Mumble Server.");
            System.exit(1);
        }
    }

    public ClientConnection getConnection() {
        return connection;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    // Used for testing 2 applications in main execution
    public static void main(String[] args) {
        new ChatClient().start();
    }
}
