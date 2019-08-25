package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Server {
    private ServerSocket server;
    private boolean isHandlingRequests;
    private ArrayList<Authentication> ListOfAuthenticatedUsers;
    private final static String POS_GREETING = "+UOA-XX SFTP Service";
    private final static String NEG_GREETING = "-UOA-XX Out to Lunch";

    private String username = "superuser";
    private String password = "CompSys725";
    private long userID = 1;

    private Authentication auth = new Authentication(username, password, userID);


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

        private String user;
        private String pass;
        private long id;
        private boolean isAuthenticated = false;

        private ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                boolean connectionIsOpen = true;
                // create in/output streams to send byte data to/from client
                dataFromClientToServer = new DataInputStream(clientSocket.getInputStream());
                dataToClientFromServer = new DataOutputStream(clientSocket.getOutputStream());
                sendTextToClient(Server.POS_GREETING);

                while (connectionIsOpen) {
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
                        case "TOBE":
                            ;
                        case "SEND":
                            ;
                        case "STOP":
                            ;
                        case "SIZE":
                            ;
                        default:
                            String message = "Command not recognised, please try again.";
                            System.out.println(message);
                    }

                    String messageToClient = "Message Received" ;
                    sendTextToClient(messageToClient);
                    System.out.println("Sent return message to client: " + clientSocket);
                    System.out.println("");
                }
            } catch (EOFException e) {
                System.out.println("End Of File detected");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void userCommand(String[] args) throws IOException {
            String loggedIn = "!" + args[1] + " logged in";
            String success = "+User-id valid, send account and password";
            String error = "-Invalid user-id, try again";

            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void acctCommand(String[] args) throws IOException {
            String loggedIn = "! Account valid, logged-in";
            String success = "+Account valid, send password";
            String error = "-Invalid account, try again";

            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void passCommand(String[] args) throws IOException {
            String loggedIn = "! Logged in";
            String success = "+Send account";
            String error = "-Wrong password, try again";
            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void typeCommand(String[] args) throws IOException {
            String successA = "+Using Ascii mode";
            String successB = "+Using Binary mode";
            String successC = "+Using Continuous mode";
            String error = "-Type not valid";
            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void listCommand(String[] args) throws IOException {
            String success = "+";
            String error = "-";
            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void cdirCommand(String[] args) throws IOException {
            String success = "+";
            String error = "-";
            String loggedIn = "!";
            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void killCommand(String[] args) throws IOException {
            String success = "+";
            String error = "-";
            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void nameCommand(String[] args) throws IOException {
            String success = "+";
            String error = "-";
            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void doneCommand(String[] args) throws IOException {
            String success = "+Your account has been charged";
            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void retrCommand(String[] args) throws IOException {
            String success = "+";
            String error = "-";
            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
            }
        }

        private void storCommand(String[] args) throws IOException {
            String success = "+";
            String error = "-";
            if (args.length == 2) {
                sendTextToClient(Server.POS_GREETING);
            }
            else {
                sendTextToClient(Server.POS_GREETING);
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

        private void sendTextToClient(String string) throws IOException {
            byte[] stringAsByteArray = string.getBytes("ISO646-US");
            byte[] byteArrayWithNull = new byte[stringAsByteArray.length + 1];
            System.arraycopy(stringAsByteArray,0, byteArrayWithNull, 0, stringAsByteArray.length);
            dataToClientFromServer.write(byteArrayWithNull, 0, byteArrayWithNull.length);
            dataToClientFromServer.flush();
        }
    }

    public static void main(String args[]) throws IOException {
        int inputPort = 5000;
        Server server = new Server();
        server.start(5000 + inputPort - inputPort);
    }
}
