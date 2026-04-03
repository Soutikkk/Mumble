package com.mumble.service;

import com.mumble.model.Message;
import com.mumble.model.User;

import java.util.List;

/**
 * Interface defining storage operations (Abstraction).
 */
public interface StorageService {
    void saveUser(User user);
    User getUser(String username);
    List<User> getAllUsers();

    void saveMessage(Message message);
    List<Message> getAllMessages();
}
