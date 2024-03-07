package com.ServerClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private ServerSocket serverSocket;
    private int port;
    private Map<String, Socket> activeMembers;
    private String coordinator;
    
    public Server(int port) {
        this.port = port;
        this.activeMembers = new HashMap<>();
        this.coordinator = null;
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                // Handle client connection
                Thread clientHandlerThread = new Thread(new ClientHandler(clientSocket));
                clientHandlerThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private synchronized void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            String line;
            while ((line = reader.readLine()) != null) {
                
                String[] parts = line.split("\\|", 2);
                String command = parts[0];
                String parameters = parts.length > 1 ? parts[1] : "";
                
                switch (command) {
                    case "JOIN":
                        handleJoin(parameters, clientSocket, writer);
                        break;
                    case "LIST":
                        handleList(writer);
                        break;
                    case "MESSAGE":
                        handleMessage(parameters);
                        break;
                    case "QUIT":
                        handleQuit(parameters);
                        break;
                    case "CHANGE_COORDINATOR":
                        handleChangeCoordinator(parameters, writer);
                        break;
                    default:
                        writer.println("ERROR|Unknown command: " + command);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void handleJoin(String parameters, Socket clientSocket, PrintWriter writer) {
        String[] parts = parameters.split(",");
        if (parts.length != 3) {
            writer.println("ERROR|Invalid parameters for JOIN command");
            return;
        }
        
        String id = parts[0];
        String ip = parts[1];
        int port = Integer.parseInt(parts[2]);
        
        if (activeMembers.containsKey(id)) {
            writer.println("ERROR|ID already exists");
            return;
        }
        
        activeMembers.put(id, clientSocket);
        
        if (coordinator == null) {
            coordinator = id;
            writer.println("COORDINATOR");
        } else {
            writer.println("COORDINATOR|" + coordinator);
        }
        
        System.out.println("New member joined: " + id + " (" + ip + ":" + port + ")");
    }
    
    private void handleList(PrintWriter writer) {
        writer.println("LIST|" + coordinator);
        for (String id : activeMembers.keySet()) {
            Socket socket = activeMembers.get(id);
            String ip = socket.getInetAddress().getHostAddress();
            int port = socket.getPort();
            writer.println(id + "," + ip + "," + port);
        }
    }
    
    private void handleMessage(String parameters) {
        String[] parts = parameters.split("\\|", 3);
        if (parts.length != 3) {
            return;
        }
        
        String from = parts[0];
        String to = parts[1];
        String message = parts[2];
        
        if ("ALL".equals(to)) {
            for (Socket socket : activeMembers.values()) {
                try {
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    writer.println("MESSAGE|" + from + "|" + message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Socket socket = activeMembers.get(to);
            if (socket != null) {
                try {
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    writer.println("MESSAGE|" + from + "|" + message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void handleQuit(String parameters) {
        activeMembers.remove(parameters);
        if (coordinator.equals(parameters)) {
            coordinator = activeMembers.keySet().iterator().next(); 
        }
        System.out.println("Member left: " + parameters);
    }
    
    private void handleChangeCoordinator(String newCoordinatorId, PrintWriter writer) {
        if (activeMembers.containsKey(newCoordinatorId)) {
            coordinator = newCoordinatorId;
            writer.println("COORDINATOR|" + coordinator);
            System.out.println("Coordinator changed to: " + newCoordinatorId);
            broadcastMessage("COORDINATOR_CHANGE|" + coordinator);
        } else {
            writer.println("ERROR|Specified ID does not exist in active members.");
        }
    }
    
    private void broadcastMessage(String message) {
        for (Socket socket : activeMembers.values()) {
            try {
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            handleClient(clientSocket);
        }
    }
    
    public static void main(String[] args) {
        int port = 12345; 
        Server server = new Server(port);
        server.start();
    }
}
