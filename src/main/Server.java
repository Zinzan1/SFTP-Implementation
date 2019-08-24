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

    private void start(int port) throws IOException {
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
            try {
                Server.this.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        isHandlingRequests = true;
        int count = 1;

        while (this.isHandlingRequests) {
            new ClientHandler(server.accept()).start();
            System.out.println("Handling client " + count);
        }
    }

    private void stop() throws IOException {
        server.close();
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;

        private DataInputStream dataFromClientToServer;
        private DataOutputStream dataToClientFromServer;
        private SendMode sendMode = SendMode.A;

        private ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                boolean connectionIsOpen = true;

                while (connectionIsOpen) {
                    // create in/output streams to send byte data to/from client
                    dataFromClientToServer = new DataInputStream(clientSocket.getInputStream());
                    dataToClientFromServer = new DataOutputStream(clientSocket.getOutputStream());

                    byte[] messageBuffer = new byte[1000];
                    int bytesRead  = 0;
                    boolean nullDetected = false;

                    while(!nullDetected){
                        byte messageByte = dataFromClientToServer.readByte();
                        if(messageByte != 0) {
                            messageBuffer[bytesRead] = messageByte;
                            bytesRead++;
                        } else {
                            nullDetected = true;
                            bytesRead = 0;
                        }
                    }

                    String messageFromClient = new String(removeNull(messageBuffer));
                    System.out.println("Received [ " + messageFromClient + " ] from: " + clientSocket);

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

                    String messageToClient = "Message Received" ;
                    byte[] stringAsByteArray = messageToClient.getBytes("ISO646-US");
                    byte[] byteArrayWithNull = new byte[stringAsByteArray.length + 1];
                    System.arraycopy(stringAsByteArray,0, byteArrayWithNull, 0, stringAsByteArray.length);
                    dataToClientFromServer.write(byteArrayWithNull, 0, byteArrayWithNull.length);
                    dataToClientFromServer.flush();
                    System.out.println("Sent return message to client: " + clientSocket);
                    System.out.println("");
                }
            } catch (EOFException e) {
                System.out.println("End Of File detected");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                return;
            }
        }

        private void userCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void acctCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void passCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void typeCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void listCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void cdirCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void killCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void nameCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void doneCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void retrCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }

        private void storCommand(String[] args) throws IOException {
            if (args.length == 2) {
                dataToClientFromServer.writeUTF("Message received");
            }
            else {
                dataToClientFromServer.writeUTF("Message received");
            }
        }




        private String[] parseCommandFromClient(String commandFromClient) {
            String[] tokenizedCommand = commandFromClient.trim().split("\\s+");
//            for (String command : tokenizedCommand) {
////                System.out.println(command);
////            }
            return tokenizedCommand;
        }

        private byte[] removeNull(byte[] array) {
            int sizeOfArray = 0;
            byte value = 0;

            for (int i=0; i < array.length; i++){
                value = array[i];
                if (value == 0) {
                    sizeOfArray = i;
                    break;
                }
            }

            byte[] nullRemovedByteArray = new byte[sizeOfArray];

            for (int j=0; j < sizeOfArray; j++){
                nullRemovedByteArray[j] = array[j];
            }

            return  nullRemovedByteArray;
        }
    }

    public static void main(String args[]) throws IOException {
        int inputPort = 5000;

        Server server = new Server();
        server.start(5000 + inputPort - inputPort);
    }
}
