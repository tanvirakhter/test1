package com.ServerClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientGUI {
    private JFrame frame;
    private PrintWriter writer;

    public ClientGUI() {
        frame = new JFrame("Client Chat");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center the frame on the screen

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true); // Wrap text
        chatArea.setWrapStyleWord(true); // Wrap at word boundaries
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove border

        JTextField inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(inputField.getText(), chatArea);
                inputField.setText("");
            }
        });

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(inputField.getText(), chatArea);
                inputField.setText("");
            }
        });

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        connectToServer(chatArea);
    }

    private void sendMessage(String message, JTextArea chatArea) {
        writer.println(message);
        chatArea.append("You: " + message + "\n");
    }

    private void connectToServer(JTextArea chatArea) {
        try {
            Socket socket = new Socket("localhost", 12345);
            writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("COORDINATOR_CHANGE|")) {
                                // Coordinator change message received
                                String newCoordinator = line.substring("COORDINATOR_CHANGE|".length());
                                chatArea.append("Coordinator changed to: " + newCoordinator + "\n");
                            } else {
                                // Regular message
                                chatArea.append("Server: " + line + "\n");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ClientGUI();
            }
        });
    }
}
