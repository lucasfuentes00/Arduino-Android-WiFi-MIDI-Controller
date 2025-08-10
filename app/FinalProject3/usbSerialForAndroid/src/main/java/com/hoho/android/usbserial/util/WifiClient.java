package com.hoho.android.usbserial.util;

import java.io.*;
import java.net.*;

public class WifiClient {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private BufferedReader userInput;

    // Constructor to initialize the connection
    public WifiClient(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);  // Connect to the server
        out = new PrintWriter(socket.getOutputStream(), true);  // To send data
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  // To receive data
        userInput = new BufferedReader(new InputStreamReader(System.in));  // To read user input
    }

    // Method to send a message to the server
    public void sendMessage(String message) {
        out.println(message);  // Send message to the server
    }

    // Method to receive messages from the server
    public void receiveMessages() {
        String serverMessage;
        try {
            while ((serverMessage = in.readLine()) != null) {
                System.out.println("Server says: " + serverMessage);
            }
        } catch (IOException e) {
            System.err.println("Error receiving message: " + e.getMessage());
        }
    }

    // Method to handle sending and receiving messages in parallel
    public void startClient() {
        // Thread for receiving messages
        Thread receiveThread = new Thread(() -> {
            receiveMessages();
        });

        // Start the receiving thread
        receiveThread.start();

        // Main loop to handle sending messages
        String message;
        try {
            while (true) {
                System.out.print("Enter message to send to the server (type 'exit' to quit): ");
                message = userInput.readLine();
                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }
                sendMessage(message);  // Send message to the server
            }
        } catch (IOException e) {
            System.err.println("Error reading user input: " + e.getMessage());
        } finally {
            try {
                socket.close();  // Close the socket connection when done
                System.out.println("Connection closed.");
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // Main method to initialize and start the client
    public static void main(String[] args) {
        try {
            // Replace with the actual server's IP address and port
            WifiClient client = new WifiClient("192.168.1.100", 12345);  // Replace with actual IP and port
            client.startClient();  // Start the client to send/receive messages
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
