package com.mumble.server;

import com.mumble.model.SystemMessage;
import com.mumble.model.TextMessage;
import com.mumble.model.User;
import com.mumble.service.AuthService;
import com.mumble.service.FileStorageService;
import com.mumble.service.StorageService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 5555;
    private static final String GROUPS_FILE = "groups.json";

    private final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groups = new ConcurrentHashMap<>();
    private final StorageService storageService;
    private final AuthService authService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ChatServer() {
        this.storageService = new FileStorageService();
        this.authService = new AuthService(storageService);

        // Reset all users to offline on startup
        for (User u : storageService.getAllUsers()) {
            if (u.isOnline()) {
                u.setOnline(false);
                storageService.saveUser(u);
            }
        }

        // Load persisted groups
        loadGroups();
    }

    public void start() {
        System.out.println("Mumble Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this, authService, storageService);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addClient(String username, ClientHandler handler) {
        // Send all currently-online users to the new client
        for (String onlineUser : activeClients.keySet()) {
            if (!onlineUser.equals(username)) {
                SystemMessage onlineNotif = new SystemMessage("SERVER", "STATUS_CHANGE", onlineUser + " is online");
                onlineNotif.setTargetUsername(onlineUser);
                handler.sendMessage(onlineNotif);
            }
        }

        // Send all groups this user belongs to
        for (Map.Entry<String, Set<String>> entry : groups.entrySet()) {
            if (entry.getValue().contains(username)) {
                SystemMessage groupNotif = new SystemMessage("SERVER", "GROUP_CREATED", entry.getKey());
                groupNotif.setTargetUsername(String.join(",", entry.getValue()));
                handler.sendMessage(groupNotif);
            }
        }

        activeClients.put(username, handler);
        User u = storageService.getUser(username);
        if (u != null) {
            u.setOnline(true);
            storageService.saveUser(u);
        }
        broadcastSystemEvent("STATUS_CHANGE", username + " is online", username);
    }

    public void removeClient(String username) {
        if (username != null) {
            activeClients.remove(username);
            User u = storageService.getUser(username);
            if (u != null) {
                u.setOnline(false);
                storageService.saveUser(u);
            }
            broadcastSystemEvent("STATUS_CHANGE", username + " went offline", username);
        }
    }

    public void routeMessage(TextMessage message) {
        String receiver = message.getReceiver();
        if (receiver == null || receiver.trim().isEmpty()) {
            for (ClientHandler handler : activeClients.values()) {
                handler.sendMessage(message);
            }
        } else if (receiver.startsWith("group:")) {
            String groupName = receiver.substring(6);
            Set<String> members = groups.get(groupName);
            if (members != null) {
                for (String member : members) {
                    ClientHandler h = activeClients.get(member);
                    if (h != null) h.sendMessage(message);
                }
            }
        } else {
            ClientHandler sender = activeClients.get(message.getSender());
            if (sender != null) sender.sendMessage(message);
            if (!receiver.equals(message.getSender())) {
                ClientHandler recv = activeClients.get(receiver);
                if (recv != null) recv.sendMessage(message);
            }
        }
        new Thread(() -> {
            try { storageService.saveMessage(message); }
            catch (Exception e) { System.err.println("Storage error: " + e.getMessage()); }
        }).start();
    }

    public void routeSystemMessage(SystemMessage sm) {
        String action = sm.getSystemAction();

        if ("TYPING".equals(action)) {
            String target = sm.getTargetUsername();
            if (target == null) {
                for (Map.Entry<String, ClientHandler> entry : activeClients.entrySet()) {
                    if (!entry.getKey().equals(sm.getSender())) entry.getValue().sendMessage(sm);
                }
            } else if (target.startsWith("group:")) {
                String groupName = target.substring(6);
                Set<String> members = groups.get(groupName);
                if (members != null) {
                    for (String member : members) {
                        if (!member.equals(sm.getSender())) {
                            ClientHandler h = activeClients.get(member);
                            if (h != null) h.sendMessage(sm);
                        }
                    }
                }
            } else {
                ClientHandler targetHandler = activeClients.get(target);
                if (targetHandler != null) {
                    sm.setTargetUsername(sm.getSender());
                    targetHandler.sendMessage(sm);
                }
            }
        } else if ("CREATE_GROUP".equals(action)) {
            String groupName = sm.getSystemContent();
            String[] memberArr = sm.getTargetUsername().split(",");
            Set<String> members = ConcurrentHashMap.newKeySet();
            members.add(sm.getSender());
            for (String m : memberArr) {
                String member = m.trim();
                if (!member.isEmpty()) members.add(member);
            }
            groups.put(groupName, members);
            saveGroups(); // Persist to disk!
            System.out.println("Group created: " + groupName + " with members: " + members);

            SystemMessage notif = new SystemMessage("SERVER", "GROUP_CREATED", groupName);
            notif.setTargetUsername(String.join(",", members));
            for (String member : members) {
                ClientHandler h = activeClients.get(member);
                if (h != null) h.sendMessage(notif);
            }
        }
    }

    private void broadcastSystemEvent(String action, String content, String targetUsername) {
        SystemMessage sm = new SystemMessage("SERVER", action, content);
        sm.setTargetUsername(targetUsername);
        for (ClientHandler handler : activeClients.values()) {
            handler.sendMessage(sm);
        }
    }

    // ─── Groups persistence ─────────────────────────
    private void saveGroups() {
        try {
            // Convert Set<String> to List<String> for clean JSON
            Map<String, List<String>> serializable = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : groups.entrySet()) {
                serializable.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            try (Writer writer = new FileWriter(GROUPS_FILE)) {
                gson.toJson(serializable, writer);
            }
            System.out.println("Groups saved to " + GROUPS_FILE);
        } catch (IOException e) {
            System.err.println("Failed to save groups: " + e.getMessage());
        }
    }

    private void loadGroups() {
        File file = new File(GROUPS_FILE);
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                for (Map.Entry<String, List<String>> entry : loaded.entrySet()) {
                    Set<String> members = ConcurrentHashMap.newKeySet();
                    members.addAll(entry.getValue());
                    groups.put(entry.getKey(), members);
                }
                System.out.println("Loaded " + groups.size() + " groups from " + GROUPS_FILE);
            }
        } catch (Exception e) {
            System.err.println("Failed to load groups: " + e.getMessage());
        }
    }

    public Map<String, Set<String>> getGroups() {
        return groups;
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}
