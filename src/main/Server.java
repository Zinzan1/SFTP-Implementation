package main;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class Server {
    //initialize socket and input stream
    private ServerSocket server;
    private boolean isHandlingRequests;

    // constructor with port
    public Server() {
    }

    public void start(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");

            new Thread(() -> {
                String inputFromUser = "";
                Scanner scan = new Scanner(System.in);
                System.out.println("Type 'exit' to close the server");

                while (!inputFromUser.toLowerCase().equals("exit")) {
                    inputFromUser = scan.nextLine();
                }

                System.out.println("Disconnecting from server");
                Server.this.stop();
            }).start();

            isHandlingRequests = true;
            int count = 1;

            while (this.isHandlingRequests) {
                new ClientHandler(server.accept()).start();
                System.out.println("Handling request " + count);
            }

        } catch (SocketException e) {
            System.out.println("Disconnected from server");
        } catch ( IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream inputStream = clientSocket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                // read the list of messages from the socket
                String messageFromClient = (String) objectInputStream.readObject();
                System.out.println("Received [ " + messageFromClient + " ] from: " + clientSocket);

                currentThread().sleep(2000);

                OutputStream outputStream = clientSocket.getOutputStream();
                // create an object output stream from the output stream so we can send an object through it
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject("Message received");
                System.out.println("Sent return message to client: " + clientSocket);

                System.out.println("Closing connection");

                // close connection
                clientSocket.close();
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        Server server = new Server();
        server.start(5000);
    }
}
