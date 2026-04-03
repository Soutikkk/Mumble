package com.mumble;

import com.mumble.client.ChatClient;
import com.mumble.server.ChatServer;

import javax.swing.*;

import com.formdev.flatlaf.FlatDarkLaf;

public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        
        String[] options = {"Start Server", "Start Client"};
        int choice = JOptionPane.showOptionDialog(null, 
                "What would you like to start?", 
                "Mumble App Selection", 
                JOptionPane.DEFAULT_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null, options, options[0]);

        if (choice == 0) {
            // Start Server
            System.out.println("Starting Server...");
            ChatServer.main(new String[]{});
        } else if (choice == 1) {
            // Start Client
            System.out.println("Starting Client...");
            ChatClient.main(new String[]{});
        }
    }
}
