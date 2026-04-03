package com.mumble.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mumble.model.Message;
import com.mumble.model.User;
import com.mumble.util.MessageAdapter;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of StorageService using JSON files.
 */
public class FileStorageService implements StorageService {
    private static final String USERS_FILE = "users.json";
    private static final String MESSAGES_FILE = "messages.json";
    
    private final Gson gson;
    private final ReadWriteLock userLock = new ReentrantReadWriteLock();
    private final ReadWriteLock messageLock = new ReentrantReadWriteLock();

    public FileStorageService() {
        this.gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Message.class, new MessageAdapter())
                .setPrettyPrinting()
                .create();
    }

    @Override
    public void saveUser(User user) {
        userLock.writeLock().lock();
        try {
            List<User> users = getAllUsersInternal();
            users.removeIf(u -> u.getUsername().equals(user.getUsername()));
            users.add(user);
            writeToFile(USERS_FILE, gson.toJson(users));
        } finally {
            userLock.writeLock().unlock();
        }
    }

    @Override
    public User getUser(String username) {
        userLock.readLock().lock();
        try {
            return getAllUsersInternal().stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
        } finally {
            userLock.readLock().unlock();
        }
    }

    @Override
    public List<User> getAllUsers() {
        userLock.readLock().lock();
        try {
            return getAllUsersInternal();
        } finally {
            userLock.readLock().unlock();
        }
    }

    @Override
    public void saveMessage(Message message) {
        messageLock.writeLock().lock();
        try {
            List<Message> messages = getAllMessagesInternal();
            
            // If it's a text message that's being updated (like read receipt), we must replace it based on ID
            if (message instanceof com.mumble.model.TextMessage) {
                com.mumble.model.TextMessage tm = (com.mumble.model.TextMessage) message;
                boolean replaced = false;
                for (int i = 0; i < messages.size(); i++) {
                    Message m = messages.get(i);
                    if (m instanceof com.mumble.model.TextMessage && ((com.mumble.model.TextMessage) m).getId().equals(tm.getId())) {
                        messages.set(i, tm);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) {
                    messages.add(message);
                }
            } else {
                messages.add(message);
            }
            writeToFile(MESSAGES_FILE, gson.toJson(messages));
        } finally {
            messageLock.writeLock().unlock();
        }
    }

    @Override
    public List<Message> getAllMessages() {
        messageLock.readLock().lock();
        try {
            return getAllMessagesInternal();
        } finally {
            messageLock.readLock().unlock();
        }
    }

    private List<User> getAllUsersInternal() {
        Type listType = new TypeToken<ArrayList<User>>(){}.getType();
        List<User> users = readFromFile(USERS_FILE, listType);
        return users != null ? users : new ArrayList<>();
    }

    private List<Message> getAllMessagesInternal() {
        Type listType = new TypeToken<ArrayList<Message>>(){}.getType();
        List<Message> messages = readFromFile(MESSAGES_FILE, listType);
        return messages != null ? messages : new ArrayList<>();
    }

    private void writeToFile(String filename, String content) {
        try (Writer writer = new FileWriter(filename)) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private <T> T readFromFile(String filename, Type typeOfT) {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, typeOfT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
