package com.mumble.client;

import com.mumble.model.Message;
import com.mumble.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClientConnection {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5555;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private Consumer<Message> onMessageReceived;
    private Runnable onDisconnected;

    // Buffer messages that arrive before the UI callback is set
    private final List<Message> messageBuffer = new ArrayList<>();
    private volatile boolean callbackReady = false;

    public void setOnMessageReceived(Consumer<Message> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
        // Replay any buffered messages that arrived before the callback was set
        synchronized (messageBuffer) {
            callbackReady = true;
            for (Message msg : messageBuffer) {
                onMessageReceived.accept(msg);
            }
            messageBuffer.clear();
        }
    }

    public void setOnDisconnected(Runnable onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    public boolean connect() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            return false;
        }
    }

    public User login(String username, String password) {
        if (socket == null || socket.isClosed()) return null;
        try {
            out.writeObject("LOGIN");
            out.writeObject(username);
            out.writeObject(password);
            
            String response = (String) in.readObject();
            if ("AUTH_SUCCESS".equals(response)) {
                User user = (User) in.readObject();
                startListening();
                return user;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean register(String username, String password) {
        if (socket == null || socket.isClosed()) return false;
        try {
            out.writeObject("REGISTER");
            out.writeObject(username);
            out.writeObject(password);
            
            String response = (String) in.readObject();
            return "REG_SUCCESS".equals(response);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (socket != null && !socket.isClosed()) {
                    Message message = (Message) in.readObject();
                    if (callbackReady && onMessageReceived != null) {
                        onMessageReceived.accept(message);
                    } else {
                        // Buffer it — the UI callback isn't set yet
                        synchronized (messageBuffer) {
                            if (callbackReady && onMessageReceived != null) {
                                onMessageReceived.accept(message);
                            } else {
                                messageBuffer.add(message);
                            }
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Disconnected from server.");
                if (onDisconnected != null) {
                    onDisconnected.run();
                }
            }
        }).start();
    }

    public synchronized void sendMessage(Message message) {
        if (out != null) {
            try {
                out.writeObject(message);
                out.flush();
                out.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
