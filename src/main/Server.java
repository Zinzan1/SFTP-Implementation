package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Server {
    //initialize socket and input stream
    private ServerSocket server;
    private boolean isHandlingRequests;
    private ArrayList<Authentication> ListOfAuthenticatedUsers;


    // constructor with port
    private Server() {
    }

    private void start(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started: Host=" + server.getInetAddress().getHostAddress() + " Port=" + server.getLocalPort());

            new Thread(() -> {
                String inputFromUser = "";
                Scanner scan = new Scanner(System.in);
                System.out.println("Type 'exit' to close the server");

                while (!inputFromUser.toLowerCase().equals("exit")) {
                    inputFromUser = scan.nextLine();
                }

                System.out.println("Closing server");
                Server.this.stop();
            }).start();

            isHandlingRequests = true;
            int count = 1;

            while (this.isHandlingRequests) {
                new ClientHandler(server.accept()).start();
                System.out.println("Handling client " + count);
            }

        } catch (SocketException e) {
            System.out.println("Server closed");
        } catch ( IOException e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;

        private ObjectInputStream dataFromClientToServer;
        private ObjectOutputStream dataToClientFromServer;
        private SendMode sendMode = SendMode.A;

        private ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                boolean connectionIsOpen = true;

                while (connectionIsOpen) {
                    dataFromClientToServer = new ObjectInputStream(clientSocket.getInputStream());

                    // read the list of messages from the socket
                    String messageFromClient = (String) dataFromClientToServer.readObject();
                    System.out.println("Received [ " + messageFromClient + " ] from: " + clientSocket);

                    // create an object output stream from the output stream so we can send an object through it
                    dataToClientFromServer = new ObjectOutputStream(clientSocket.getOutputStream());

                    String[] commandAsTokens = parseCommandFromClient(messageFromClient);
                    int numberOfTokens = commandAsTokens.length;
                    String upperCommandFromClient = commandAsTokens[0].toUpperCase();

                    switch (upperCommandFromClient) {
                        case "USER":
                            userCommand(commandAsTokens);
                            ;
                        case "ACCT":
                            ;
                        case "PASS":
                            ;
                        case "TYPE":
                            ;
                        case "LIST":
                            ;
                        case "CDIR":
                            ;
                        case "KILL":
                            ;
                        case "NAME":
                            ;
                        case "DONE":
                            // close connection
                            System.out.println("Closing connection");
                            clientSocket.close();
                            break;
                        case "RETR":
                            ;
                        case "STOR":
                            ;
                        default:
                            String message = "Command not recognised, please try again.";
                            System.out.println(message);
                    }

                    dataToClientFromServer.writeObject("Message received");
                    System.out.println("Sent return message to client: " + clientSocket);
                    System.out.println("");
                }
            } catch (EOFException e) {
                System.out.println("Client disconnected");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                return;
            }
        }

        private void userCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void acctCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void passCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void typeCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void listCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void cdirCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void killCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void nameCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void doneCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void retrCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }

        private void storCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeObject("Message received");
            }
            else {
                dataToClientFromServer.writeObject("Message received");
            }
        }




        private String[] parseCommandFromClient(String commandFromClient) {
            String[] tokenizedCommand = commandFromClient.trim().split("\\s+");
            for (String command : tokenizedCommand) {
                System.out.println(command);
            }
            return tokenizedCommand;
        }

    }

    public static void main(String args[]) {
        int inputPort = 5000;

        Server server = new Server();
        server.start(5000 + inputPort - inputPort);
    }
}
