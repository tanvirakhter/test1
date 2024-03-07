package com.ServerClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

//@author tanvirakhtershakib

public class Client {
    private String serverAddress;
    private int serverPort;
    
    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
    
    public void start() {
        try {
            Socket socket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);
            
            Thread inputThread = new Thread(new InputThread(socket));
            inputThread.start();
            
            Thread outputThread = new Thread(new OutputThread(socket));
            outputThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static class InputThread implements Runnable {
        private Socket socket;
        
        public InputThread(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Received: " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static class OutputThread implements Runnable {
        private Socket socket;
        
        public OutputThread(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                String line;
                while ((line = consoleReader.readLine()) != null) {
                    writer.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        String serverAddress = "localhost"; 
        int serverPort = 12345; 
        Client client = new Client(serverAddress, serverPort);
        client.start();
    }
}
