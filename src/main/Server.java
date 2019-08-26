package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private ServerSocket server;
    private boolean isHandlingRequests;
    private static Profile[] listOfProfiles = {new Profile("user1", "CompSys725", "CSE725")
                                                                                                        };
    private final static String POS_GREETING = "+UOA-XX SFTP Service";
    private final static String NEG_GREETING = "-UOA-XX Out to Lunch";

    // constructor with port
    private Server() {
    }

    private void start(int port) throws IOException {
        server = new ServerSocket(port);
        isHandlingRequests = true;
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

        while (this.isHandlingRequests) {
            new ClientHandler(server.accept()).start();
        }
    }

    private void stop() throws IOException {
        server.close();
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;

        private DataInputStream dataFromClientToServer;
        private DataOutputStream dataToClientFromServer;
        private SendMode sendMode;

        private String user;
        private String pass;
        private String account;

        String currentWorkingDirectory = System.getProperty("user.dir");

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
System.out.println(currentWorkingDirectory);
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
                    System.out.println(clientSocket + " sent: " + messageFromClient);

                    String[] commandAsTokens = parseCommandFromClient(messageFromClient);
                    String upperCommandFromClient = commandAsTokens[0].toUpperCase();

                    switch (upperCommandFromClient) {
                        case "USER":
                            userCommand(commandAsTokens);
                            break;
                        case "ACCT":
                            acctCommand(commandAsTokens);
                            break;
                        case "PASS":
                            passCommand(commandAsTokens);
                            break;
                        case "TYPE":
                            typeCommand(commandAsTokens);
                            break;
                        case "LIST":
                            listCommand(commandAsTokens);
                            break;
                        case "CDIR":
                            cdirCommand(commandAsTokens);
                            break;
                        case "KILL":
                            killCommand(commandAsTokens);
                            break;
                        case "NAME":
                            nameCommand(commandAsTokens);
                            break;
                        case "DONE":
                            doneCommand(commandAsTokens);
                            break;
                        case "RETR":
                            retrCommand(commandAsTokens);
                            break;
                        case "STOR":
                            storCommand(commandAsTokens);
                            break;
//                        case "TOBE":
//                            ;
//                        case "SEND":
//                            ;
//                        case "STOP":
//                            ;
//                        case "SIZE":
//                            ;
                        default:
                            String message = "-Command not recognised, please try again.";
                            sendTextToClient(message);
                            System.out.println(clientSocket + " sent bad command: " + messageFromClient);
                            break;
                    }
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
            String success = "+User-id valid, send account and password";
            String error = "-Invalid user-id, try again";

            if (isFullyAuthenticated() || userHasBeenGiven()) {
                String alreadyLoggedIn = "!" + user + " logged in";
                sendTextToClient(alreadyLoggedIn);
            } else {
                if (args.length == 2) {
                    if(userIdExists(args[1])) {
                        user = args[1];
                        sendTextToClient(success);
                    } else {
                        sendTextToClient(error);
                    }
                }
                else {
                    sendTextToClient(error);
                }
            }
        }

        private void acctCommand(String[] args) throws IOException {
            String loggedIn = "! Account valid, logged-in";
            String success = "+Account valid, send password";
            String error = "-Invalid account, try again";
            String userNotSpecified = "-User not specified";

            if (isFullyAuthenticated()) {
                sendTextToClient(loggedIn);
            } else {
                if (userHasBeenGiven()) {
                    if(args.length == 2) {
                        if(accountExistsForId(args[1])) {
                            if(passHasBeenGiven()){
                                account = args[1];
                                sendTextToClient(loggedIn);
                            } else {
                                sendTextToClient(success);
                            }
                        } else {
                            sendTextToClient(error);
                        }
                    } else {
                        sendTextToClient(error);
                    }
                } else {
                    sendTextToClient(userNotSpecified);
                }
            }
        }

        private void passCommand(String[] args) throws IOException {
            String loggedIn = "! Logged in";
            String success = "+Send account";
            String error = "-Wrong password, try again";
            String userNotSpecified = "-User not specified";

            if (isFullyAuthenticated()) {
                sendTextToClient(loggedIn);
            } else {
                if (userHasBeenGiven()) {
                    if(args.length == 2) {
                        if(passCorrect(args[1])) {
                            if(accountHasBeenGiven()){
                                pass = args[1];
                                sendTextToClient(loggedIn);
                            } else {
                                sendTextToClient(success);
                            }
                        } else {
                            sendTextToClient(error);
                        }
                    } else {
                        sendTextToClient(error);
                    }
                } else {
                    sendTextToClient(userNotSpecified);
                }
            }
        }

        private void typeCommand(String[] args) throws IOException {
            String successA = "+Using Ascii mode";
            String successB = "+Using Binary mode";
            String successC = "+Using Continuous mode";
            String error = "-Type not valid";
            if (args.length == 2) {
                if (args[1].toUpperCase().equals("A")) {
                    sendMode = SendMode.ASCII;
                    sendTextToClient(successA);
                } else if (args[1].toUpperCase().equals("B")) {
                    sendMode = SendMode.BINARY;
                    sendTextToClient(successB);
                } else if (args[1].toUpperCase().equals("C")) {
                    sendMode = SendMode.CONTINUOUS;
                    sendTextToClient(successC);
                } else {
                    sendTextToClient(error);
                }
            }
            else {
                sendTextToClient(error);
            }
        }

        private void listCommand(String[] args) throws IOException {
            String success = "+";
            String error = "-";
            if (args.length == 2) {
                String arg = args[1].toUpperCase();
                switch(arg) {
                    case "F":;
                    case "V":;
                    default:
                        File curDir = new File(currentWorkingDirectory);
                        String[] filesInThisDir = curDir.list();
                        sendTextToClient(formatDirectoryFiles(filesInThisDir));
                }
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

        private void sendAuthErrorToClient() throws IOException {
            String authErrorMessage = "-Please authenticate first";
            sendTextToClient(authErrorMessage);
        }

        private boolean userIdExists(String idTocheck){
            for(Profile p : listOfProfiles) {
                if(idTocheck.equals(p.getUserID())) {
                    return true;
                }
            }
            return false;
        }

        private boolean passCorrect(String passToCheck){
            for(Profile p : listOfProfiles) {
                if(user.equals(p.getUserID())) {
                    if (passToCheck.equals(p.getPassword())){
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean accountExistsForId(String accountToCheck){
            for(Profile p : listOfProfiles) {
                if(user.equals(p.getUserID())) {
                    if (accountToCheck.equals(p.getAccount())){
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean userHasBeenGiven() {
            if(user==null){
                return false;
            }
            return true;
        }

        private boolean passHasBeenGiven() {
            if(pass==null){
                return false;
            }
            return true;
        }

        private boolean accountHasBeenGiven() {
            if(account==null){
                return false;
            }
            return true;
        }

        private boolean isFullyAuthenticated() {
            if(user==null || account==null || pass==null) {
                return false;
            }
            return true;
        }

        private void callFromCommandLine(String command) {

        }

        private String getOperatingSystem() {
            String os = System.getProperty("os.name");
            System.out.println("Using System Property: " + os);
            return os;
        }

        private String formatDirectoryFiles(String[] listOfFiles) {
            StringBuilder filesAsString = new StringBuilder();
            filesAsString.append("+" + currentWorkingDirectory + System.lineSeparator());
            for (String s : listOfFiles) {
                filesAsString.append(s + System.lineSeparator());
            }
            filesAsString.append("\0");
            return filesAsString.toString();
        }
    }

    public static void main(String args[]) throws IOException {
        int inputPort = 5000;
        Server server = new Server();
        server.start(5000 + inputPort - inputPort);
    }
}
