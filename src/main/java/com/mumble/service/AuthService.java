package com.mumble.service;

import com.mumble.model.User;

public class AuthService {
    private final StorageService storageService;

    public AuthService(StorageService storageService) {
        this.storageService = storageService;
    }

    public User login(String username, String password) {
        User user = storageService.getUser(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public boolean register(String username, String password) {
        if (storageService.getUser(username) != null) {
            return false; // User already exists
        }
        User newUser = new User(username, password);
        storageService.saveUser(newUser);
        return true;
    }
}
