package com.mumble.server;

import com.mumble.model.Message;
import com.mumble.model.SystemMessage;
import com.mumble.model.TextMessage;
import com.mumble.model.User;
import com.mumble.service.AuthService;
import com.mumble.service.StorageService;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private final AuthService authService;
    private final StorageService storageService;
    
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, ChatServer server, AuthService authService, StorageService storageService) {
        this.socket = socket;
        this.server = server;
        this.authService = authService;
        this.storageService = storageService;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Authentication Loop
            boolean authenticated = false;
            while (!authenticated && running) {
                String requestType = (String) in.readObject();
                String reqUsername = (String) in.readObject();
                String reqPass = (String) in.readObject();

                if ("LOGIN".equals(requestType)) {
                    User user = authService.login(reqUsername, reqPass);
                    if (user != null) {
                        out.writeObject("AUTH_SUCCESS");
                        out.writeObject(user);
                        out.flush();
                        this.username = user.getUsername();
                        server.addClient(this.username, this);
                        authenticated = true;
                        System.out.println("User logged in: " + username);

                        // Send chat history to this client
                        sendChatHistory();
                    } else {
                        out.writeObject("AUTH_FAIL");
                        out.flush();
                    }
                } else if ("REGISTER".equals(requestType)) {
                    boolean success = authService.register(reqUsername, reqPass);
                    out.writeObject(success ? "REG_SUCCESS" : "REG_FAIL");
                    out.flush();
                }
            }

            // Message Processing Loop
            while (running) {
                Message message = (Message) in.readObject();
                if (message instanceof TextMessage) {
                    server.routeMessage((TextMessage) message);
                } else if (message instanceof SystemMessage) {
                    server.routeSystemMessage((SystemMessage) message);
                }
            }
        } catch (EOFException e) {
            System.out.println("Client " + username + " disconnected cleanly.");
        } catch (IOException e) {
            System.out.println("Client " + username + " connection lost: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Protocol error from " + username + ": " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error in handler for " + username);
            e.printStackTrace();
        } finally {
            running = false;
            closeEverything();
        }
    }

    /**
     * Send recent chat history (TextMessages only) to this client on login.
     */
    private void sendChatHistory() {
        try {
            List<Message> allMessages = storageService.getAllMessages();
            // Collect all group names this user belongs to
            java.util.Set<String> myGroups = new java.util.HashSet<>();
            for (java.util.Map.Entry<String, java.util.Set<String>> entry : server.getGroups().entrySet()) {
                if (entry.getValue().contains(username)) {
                    myGroups.add("group:" + entry.getKey());
                }
            }
            int sent = 0;
            int start = Math.max(0, allMessages.size() - 200);
            for (int i = start; i < allMessages.size(); i++) {
                Message m = allMessages.get(i);
                if (m instanceof TextMessage) {
                    TextMessage tm = (TextMessage) m;
                    String receiver = tm.getReceiver();
                    boolean relevant = false;
                    if (receiver == null) {
                        relevant = true; // Global message
                    } else if (receiver.equals(username)) {
                        relevant = true; // Direct message to me
                    } else if (tm.getSender().equals(username)) {
                        relevant = true; // I sent this
                    } else if (myGroups.contains(receiver)) {
                        relevant = true; // Group message for a group I'm in
                    }
                    if (relevant) {
                        sendMessage(tm);
                        sent++;
                    }
                }
            }
            System.out.println("Sent " + sent + " history messages to " + username);
        } catch (Exception e) {
            System.err.println("Error sending chat history to " + username + ": " + e.getMessage());
        }
    }

    public synchronized void sendMessage(Message message) {
        if (!running) return;
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.out.println("Failed to send to " + username + ": " + e.getMessage());
            running = false;
            closeEverything();
        }
    }

    private void closeEverything() {
        server.removeClient(username);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Silently close
        }
    }
}
