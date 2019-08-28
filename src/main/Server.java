package main;

import com.sun.deploy.util.StringUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

// Below is the code for the server, including a main function.
// I have provided some basic comments for the code, but I have tried to use good variable names for self documenting
// code.
/***********************************************   Server Code   ******************************************************/
public class Server {
    private ServerSocket server;
    private boolean isHandlingRequests;
    private static Profile[] listOfProfiles = {new Profile("user1", "CompSys725", "CSE725")
                                                                                                        };
    private final static String POS_GREETING = "+UOA-XX SFTP Service";
    private final static String NEG_GREETING = "-UOA-XX Out to Lunch";
    private final static String userNotSpecified = "-User not specified";
    private final static String notLoggedIn = "-Not yet authenticated, please complete login process";
    private final static String superUserID = "super";

    // constructor with port
    private Server() {
    }

    // Method that starts the server
    private void start(int port) throws IOException {
        // Listens on server port
        server = new ServerSocket(port);
        isHandlingRequests = true;
        System.out.println("Server started: Host=" + server.getInetAddress().getHostAddress() + " Port=" + server.getLocalPort());

        // Thread to close the server (not used)
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

        // Create threads to handle requests.
        while (this.isHandlingRequests) {
            new ClientHandler(server.accept()).start();
        }
    }

    // Wrapper for closing the server
    private void stop() throws IOException {
        server.close();
    }


    /**************************************   Code that handles clients   *********************************************/
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        boolean connectionIsOpen = true;

        private DataInputStream dataFromClientToServer;
        private DataOutputStream dataToClientFromServer;
        private SendMode sendMode;

        private String user;
        private String pass;
        private String account;
        private boolean isSuperUser = false;

        private Profile currentProfileLoggedIn;

        String currentWorkingDirectory = System.getProperty("user.dir");

        // Constructor for creating a ClientHandler thread
        // This thread is alive as long as the connection is alive.
        private ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        // Need to override to do something useful.
        // This is where the computation happens
        @Override
        public void run() {
            try {
                // create in/output streams to send byte data to/from client
                dataFromClientToServer = new DataInputStream(clientSocket.getInputStream());
                dataToClientFromServer = new DataOutputStream(clientSocket.getOutputStream());
                sendTextToClient(Server.POS_GREETING);
                System.out.println(currentWorkingDirectory);

                // thread runs while connection is open
                while (connectionIsOpen) {
                    // waits for a message from the client
                    String messageFromClient = receiveTextFromClient();
                    // prints the message to server console.
                    // Note, the server will print lots of commands that are not in the specification.
                    // There was no specific instruction on this so I left the server as as.
                    System.out.println(clientSocket + " sent: " + messageFromClient);

                    // Parse the arguments from the client
                    String[] commandAsTokens = parseCommandFromClient(messageFromClient);
                    // Ignore case of the command tokens
                    String upperCommandFromClient = commandAsTokens[0].toUpperCase();

                    // Match the command to the specified commands
                    // Self explanatory.
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

                        // Default branch is taken if there are no matches
                        default:
                            String message = "-Command not recognised, please try again.";
                            sendTextToClient(message);
                            System.out.println(clientSocket + " sent bad command: " + messageFromClient);
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Prints to console when the thread is terminated.
            System.out.println("Thread Dead");
        }

        // Code for the USER command
        private void userCommand(String[] args) throws IOException {
            String success = "+User-id valid, send account and password";
            String error = "-Invalid user-id, try again";

            // Checks if the client has already authenticated or given a user prior ro calling this method
            if (isFullyAuthenticated() || userHasBeenGiven()) {
                String alreadyLoggedIn = "! " + user + " logged in";
                sendTextToClient(alreadyLoggedIn);
            } else {
                // Checks for the correct number of arguments
                if (args.length == 2) {
                    // Checks if the client is logging in with super privileges
                    if (args[1].equals(superUserID)) {
                        user = superUserID;
                        account = superUserID;
                        pass = superUserID;
                        isSuperUser = true;
                        String alreadyLoggedIn = "! " + user + " logged in";
                        sendTextToClient(alreadyLoggedIn);
                    }
                    // Checks if the user specified exists in the "database"
                    else if(userIdExists(args[1])) {
                        user = args[1];
                        sendTextToClient(success);
                    } else {
                        // If it does nto exist, then send an error message
                        sendTextToClient(error);
                    }
                } else {
                    // If incorrect number of args specified
                    sendTextToClient(error);
                }
            }
        }

        // Code for the ACCT command
        private void acctCommand(String[] args) throws IOException {
            String loggedIn = "! Account valid, logged-in";
            String success = "+Account valid, send password";
            String error = "-Invalid account, try again";

            // Checks if already authenticated
            if (isFullyAuthenticated()) {
                sendTextToClient(loggedIn);
            } else {
                // Checks if user has been given
                if (userHasBeenGiven()) {
                    if(args.length == 2) {
                        // Checks if the ACCT given matches the USER
                        if(accountExistsForId(args[1])) {
                            account = args[1];
                            if(passHasBeenGiven()){
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

        // Code for the PASS command - similar to ACCT
        private void passCommand(String[] args) throws IOException {
            String loggedIn = "! Logged in";
            String success = "+Send account";
            String error = "-Wrong password, try again";

            if (isFullyAuthenticated()) {
                sendTextToClient(loggedIn);
            } else {
                if (userHasBeenGiven()) {
                    if(args.length == 2) {
                        if(passCorrect(args[1])) {
                            pass = args[1];
                            if(accountHasBeenGiven()){
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

        // TYPE command - pretty useless for windows
        private void typeCommand(String[] args) throws IOException {
            String successA = "+Using Ascii mode";
            String successB = "+Using Binary mode";
            String successC = "+Using Continuous mode";
            String error = "-Type not valid";

            if (isFullyAuthenticated()) {
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
            } else {
                sendTextToClient(notLoggedIn);
            }
        }

        // LIST command
        private void listCommand(String[] args) throws IOException, InterruptedException {
            String wrongArg = "-Please provide 'F' or 'V' as the second argument";
            String error = "-cannot access '" + "your arg" + "': No such file or directory";

            File curDir = new File(currentWorkingDirectory);
            String[] filesInThisDir = curDir.list();

            if (isFullyAuthenticated()) {
                if (args.length == 2) {
                    String arg = args[1].toUpperCase();
                    String lsReturn;
                    switch(arg) {
                        case "F":
                            // Passes the call to windows cmd.exe
                            lsReturn = callLsFromCommandLine(currentWorkingDirectory, false);
                            sendTextToClient(lsReturn);
                            break;
                        case "V":
                            lsReturn = callLsFromCommandLine(currentWorkingDirectory, true);
                            sendTextToClient(lsReturn);
                            break;
                        default:
                            sendTextToClient(wrongArg);
                            break;
                    }
                }
                else if (args.length == 3){
                    String arg3 = args[1].toUpperCase();
                    String pathToDir = args[2];
                    String lsReturn;
                    switch(arg3) {
                        case "F":
                            lsReturn = callLsFromCommandLine(pathToDir, false);
                            sendTextToClient(lsReturn);
                            break;
                        case "V":
                            lsReturn = callLsFromCommandLine(pathToDir, true);
                            sendTextToClient(lsReturn);
                            break;
                        default:
                            sendTextToClient(wrongArg);
                            break;
                    }
                } else if (args.length == 1) {
                    sendTextToClient(wrongArg);
                } else {
                    sendTextToClient(error);
                }
            } else {
                sendTextToClient(notLoggedIn);
            }
        }

        // CDIR command
        private void cdirCommand(String[] args) throws IOException {
            String success = "+directory ok, send account/password";
            String fileNotExist = "-Can't connect to directory because: file doesn't exist";
            String notAtDir = "-Can't connect to directory because: file is not a directory";
            String sameDirectory = "-Can't connect to directory because: same as current working directory";

            boolean acctGiven = false;
            boolean passGiven = false;

            File curDir = new File(currentWorkingDirectory);

            if (isFullyAuthenticated()) {
                if (args.length == 2) {
                    String arg = args[1];
                    File proposedDir = new File(arg);

                    // Checks if the directory on the remote server exists
                    if (proposedDir.exists()) {
                        // Checks if the argument specified is a directory and not a file
                        if (proposedDir.isDirectory()) {
                            if (!currentWorkingDirectory.equals(arg)) {
                                // Super user doesn't need to authenticate
                                if (isSuperUser) {
                                    currentWorkingDirectory = proposedDir.getAbsolutePath();
                                    passGiven = true;
                                    acctGiven = true;
                                    String changed = "!Changed working dir to " + proposedDir.getAbsolutePath();
                                    sendTextToClient(changed);
                                } else {
                                    // Regular user needs to authenticate
                                    sendTextToClient(success);
                                    String succReturn;
                                    String[] succArgs;
                                    while (!acctGiven || !passGiven) {
                                        succReturn = receiveTextFromClient();
                                        succArgs = parseCommandFromClient(succReturn);

                                        if (succArgs.length > 0) {
                                            String succCommand = succArgs[0].toUpperCase();
                                            switch (succCommand) {
                                                case "ACCT":
                                                    String changeA = "!Changed working dir to " + proposedDir.getAbsolutePath();
                                                    String successA = "+account ok, send password";
                                                    String failA = "-invalid account";
                                                    if (succArgs.length == 2) {
                                                        if (succArgs[1].equals(account)) {
                                                            if (passGiven) {
                                                                acctGiven = true;
                                                                currentWorkingDirectory = proposedDir.getAbsolutePath();
                                                                sendTextToClient(changeA);
                                                            } else {
                                                                acctGiven = true;
                                                                sendTextToClient(successA);
                                                            }
                                                        } else {
                                                            sendTextToClient(failA);
                                                            return;
                                                        }
                                                    } else {
                                                        sendTextToClient(failA);
                                                        return;
                                                    }
                                                    break;
                                                case "PASS":
                                                    String changeP = "!Changed working dir to " + proposedDir.getAbsolutePath();
                                                    String successP = "+password ok, send account";
                                                    String failP = "-invalid password";
                                                    if (succArgs.length == 2) {
                                                        if (succArgs[1].equals(pass)) {
                                                            if (acctGiven) {
                                                                passGiven = true;
                                                                currentWorkingDirectory = proposedDir.getAbsolutePath();
                                                                sendTextToClient(changeP);
                                                            } else {
                                                                passGiven = true;
                                                                sendTextToClient(successP);
                                                            }
                                                        } else {
                                                            sendTextToClient(failP);
                                                            return;
                                                        }
                                                    } else {
                                                        sendTextToClient(failP);
                                                        return;
                                                    }
                                                    break;
                                                default:
                                                    sendTextToClient(success);
                                                    break;
                                            }
                                        } else {
                                            sendTextToClient(success);
                                        }
                                    }
                                }
                            } else {
                                sendTextToClient(sameDirectory);
                            }
                        } else {
                            sendTextToClient(notAtDir);
                        }
                    } else {
                        sendTextToClient(fileNotExist);
                    }
                } else {
                    sendTextToClient(fileNotExist);
                }
            } else {
                sendTextToClient(notLoggedIn);
            }
        }

        // KILL command
        private void killCommand(String[] args) throws IOException {
            String fileNotExist = "-Not deleted because: file doesn't exist";
            String notAtDir = "-Not deleted because: file is a directory";

            if (isFullyAuthenticated()) {
                if (args.length == 2) {
                    String arg = args[1];
                    File proposedFileForDeletion= new File(currentWorkingDirectory + System.lineSeparator() + arg);

                    // Checks if the file exists
                    if (proposedFileForDeletion.exists()) {
                        // Checks if file is NOT a directory
                        if (!proposedFileForDeletion.isDirectory()) {
                            sendTextToClient("+" + arg + " deleted");
                            Files.delete(proposedFileForDeletion.toPath());
                        } else {
                            sendTextToClient(notAtDir);
                        }
                    } else {
                        sendTextToClient(fileNotExist);
                    }
                } else {
                    sendTextToClient(fileNotExist);
                }
            } else {
                sendTextToClient(notLoggedIn);
            }
        }

        // NAME command
        private void nameCommand(String[] args) throws IOException {
            String badArgument = "-Not renamed because: file doesn't exist";
            String oldFileName;
            String newFileName;

            if (isFullyAuthenticated()) {
                if (args.length == 2) {
                    File proposedFileForRename = new File(currentWorkingDirectory + System.lineSeparator() + args[1]);
                    if (proposedFileForRename.exists()) {
                        String fileExists = "+File exists";
                        sendTextToClient(fileExists);
                        oldFileName = proposedFileForRename.getAbsolutePath();
                        String tobeReturn = receiveTextFromClient();
                        String tobeargs[] = parseCommandFromClient(tobeReturn);

                        if (tobeargs.length == 2) {
                            switch(tobeargs[0].toUpperCase()) {
                                case "TOBE":
                                    String newFilePath = new File(tobeargs[1]).getAbsolutePath();
                                    File renamedFile = new File(currentWorkingDirectory + System.lineSeparator() + newFilePath);
                                    if (!renamedFile.exists()) {
                                        newFileName = tobeargs[1];
                                        if (proposedFileForRename.renameTo(renamedFile)) {
                                            String renameSuccess = "+" + oldFileName + " renamed to " + newFileName;
                                            sendTextToClient(renameSuccess);
                                        } else {
                                            String newNameTaken = "-File wasn't renamed because: operation failed";
                                            sendTextToClient(newNameTaken);
                                        }
                                    } else {
                                        String newNameTaken = "-File wasn't renamed because: TOBE name already taken";
                                        sendTextToClient(newNameTaken);
                                    }
                                    break;
                                default:
                                    String wrongCommand = "-File wasn't renamed because: must send TOBE after NAME";
                                    sendTextToClient(wrongCommand);
                                    break;
                            }
                        } else {
                            String badArgs = "-File wasn't renamed because: wrong number of arguments for TOBE";
                            sendTextToClient(badArgs);
                        }
                    } else {
                        String fileNotExist = "-Can't find " + args[1];
                        sendTextToClient(fileNotExist);
                    }
                } else {
                    sendTextToClient(badArgument);
                }
            } else {
                sendTextToClient(notLoggedIn);
            }
        }

        // DONE command
        private void doneCommand(String[] args) throws IOException {
            String success = "+UOA-XX closing connection";
            String tooManyArgs = "-Done requires no arguments";
            if (isFullyAuthenticated()) {
                if (args.length == 1) {
                    sendTextToClient(success);
                    connectionIsOpen = false;
                }
                else {
                    sendTextToClient(tooManyArgs);
                }
            } else {
                sendTextToClient(notLoggedIn);
            }
        }

        // RETR command
        private void retrCommand(String[] args) throws IOException {
            String fileNotExist = "-File doesn't exist";

            if (isFullyAuthenticated()) {
                if (args.length == 2) {
                    String arg = args[1];
                    File fileToBeSent= new File(currentWorkingDirectory + arg);

                    if (fileToBeSent.exists()) {
                        if (!fileToBeSent.isDirectory()) {
                            sendTextToClient(" " + fileToBeSent.length());

                            String succReturn;
                            String[] succArgs;
                            String badCommand = "-please send a SEND or STOP command after RETR";

                            succReturn = receiveTextFromClient();
                            succArgs = parseCommandFromClient(succReturn);

                            if (succArgs.length == 1) {
                                String succCommand = succArgs[0].toUpperCase();
                                switch (succCommand) {
                                    case "SEND":
                                        sendTextToClient("*" + fileToBeSent.getName());
                                        byte[] bytesToSend = Files.readAllBytes(fileToBeSent.toPath());
                                        sendBytes(bytesToSend);
                                        break;
                                    case "STOP":
                                        String abortSuccess = "+ok, RETR aborted";
                                        sendTextToClient(abortSuccess);
                                        break;
                                    default:
                                        sendTextToClient(badCommand);
                                        break;
                                }
                            } else {
                                sendTextToClient(badCommand);
                            }
                        } else {
                            sendTextToClient(fileNotExist);
                        }
                    } else {
                        sendTextToClient(fileNotExist);
                    }
                } else {
                    sendTextToClient(fileNotExist);
                }
            } else {
                sendTextToClient(notLoggedIn);
            }
            System.out.println("end RETR");
        }

        // STOR command
        private void storCommand(String[] args) throws IOException {
            String notEnoughArgs = "-STOR takes 3 arguments";

            if (isFullyAuthenticated()) {
                if (args.length == 3) {
                    String arg = args[1].toUpperCase();
                    String nameOfFiletoSave = currentWorkingDirectory + File.separator + args[2];
                    File potentialFile = new File(nameOfFiletoSave);
                    boolean fileExists = potentialFile.exists();
                    boolean supportsGenerations = false;
                    String succStorReturn;
                    String succStorArgs[];

                    switch(arg) {
                        case "NEW":
                            if(fileExists) {
                                if (supportsGenerations) {
                                    sendTextToClient("+File exists, will create new generation of file");
                                    succStorReturn = receiveTextFromClient();
                                    succStorArgs = parseCommandFromClient(succStorReturn);
                                    if (succStorArgs.length == 2) {
                                        String sizeArg = succStorArgs[0].toUpperCase();
                                        long numberOfBytes;
                                        try {
                                            numberOfBytes = Long.parseLong(succStorArgs[1]);
                                        } catch (NumberFormatException e) {
                                            sendTextToClient("-please enter a valid number of bytes");
                                            break;
                                        }

                                        switch (sizeArg) {
                                            case "SIZE":
                                                if (numberOfBytes <= new File(currentWorkingDirectory).getUsableSpace()) {
                                                    sendTextToClient("+ok, waiting for file");
                                                    byte[] fileAsBytes = receiveBytes(numberOfBytes);
                                                    Files.write(potentialFile.toPath(), fileAsBytes);
                                                    sendTextToClient("+Saved " + potentialFile.getName());
                                                    //sendTextToClient("-Couldn't save because (reason)");
                                                } else {
                                                    sendTextToClient("-Not enough room, don't send it");
                                                }
                                                break;
                                            default:
                                                sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                                break;
                                        }
                                    } else {
                                        sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                    }
                                } else {
                                    sendTextToClient("-File exists, but system doesn't support generations");
                                }
                            } else {
                                sendTextToClient("+File does not exist, will create new file");
                                succStorReturn = receiveTextFromClient();
                                succStorArgs = parseCommandFromClient(succStorReturn);
                                if (succStorArgs.length == 2) {
                                    String sizeArg = succStorArgs[0].toUpperCase();
                                    long numberOfBytes;
                                    try {
                                        numberOfBytes = Long.parseLong(succStorArgs[1]);
                                    } catch (NumberFormatException e) {
                                        sendTextToClient("-please enter a valid number of bytes");
                                        break;
                                    }

                                    switch (sizeArg) {
                                        case "SIZE":
                                            if (numberOfBytes <= new File(currentWorkingDirectory).getUsableSpace()) {
                                                sendTextToClient("+ok, waiting for file");
                                                byte[] fileAsBytes = receiveBytes(numberOfBytes);
                                                Files.write(potentialFile.toPath(), fileAsBytes);
                                                sendTextToClient("+Saved " + potentialFile.getName());
                                                //sendTextToClient("-Couldn't save because (reason)");
                                            } else {
                                                sendTextToClient("-Not enough room, don't send it");
                                            }
                                            break;
                                        default:
                                            sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                            break;
                                    }
                                } else {
                                    sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                }
                            }
                            break;
                        case "OLD":
                            if(fileExists) {
                                sendTextToClient("+Will write over old file");
                                succStorReturn = receiveTextFromClient();
                                succStorArgs = parseCommandFromClient(succStorReturn);
                                if (succStorArgs.length == 2) {
                                    String sizeArg = succStorArgs[0].toUpperCase();
                                    long numberOfBytes;
                                    try {
                                        numberOfBytes = Long.parseLong(succStorArgs[1]);
                                    } catch (NumberFormatException e) {
                                        sendTextToClient("-please enter a valid number of bytes");
                                        break;
                                    }

                                    switch (sizeArg) {
                                        case "SIZE":
                                            if (numberOfBytes <= new File(currentWorkingDirectory).getUsableSpace()) {
                                                sendTextToClient("+ok, waiting for file");
                                                byte[] fileAsBytes = receiveBytes(numberOfBytes);
                                                Files.write(potentialFile.toPath(), fileAsBytes);
                                                sendTextToClient("+Saved " + potentialFile.getName());
                                                //sendTextToClient("-Couldn't save because (reason)");
                                            } else {
                                                sendTextToClient("-Not enough room, don't send it");
                                            }
                                            break;
                                        default:
                                            sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                            break;
                                    }
                                } else {
                                    sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                }
                            } else {
                                sendTextToClient("+Will create new file");
                                succStorReturn = receiveTextFromClient();
                                succStorArgs = parseCommandFromClient(succStorReturn);
                                if (succStorArgs.length == 2) {
                                    String sizeArg = succStorArgs[0].toUpperCase();
                                    long numberOfBytes;
                                    try {
                                        numberOfBytes = Long.parseLong(succStorArgs[1]);
                                    } catch (NumberFormatException e) {
                                        sendTextToClient("-please enter a valid number of bytes");
                                        break;
                                    }

                                    switch (sizeArg) {
                                        case "SIZE":
                                            if (numberOfBytes <= new File(currentWorkingDirectory).getUsableSpace()) {
                                                sendTextToClient("+ok, waiting for file");
                                                byte[] fileAsBytes = receiveBytes(numberOfBytes);
                                                Files.write(potentialFile.toPath(), fileAsBytes);
                                                sendTextToClient("+Saved " + potentialFile.getName());
                                                //sendTextToClient("-Couldn't save because (reason)");
                                            } else {
                                                sendTextToClient("-Not enough room, don't send it");
                                            }
                                            break;
                                        default:
                                            sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                            break;
                                    }
                                } else {
                                    sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                }
                            }
                            break;
                        case "APP":
                            if(fileExists) {
                                sendTextToClient("+Will append to file");
                                succStorReturn = receiveTextFromClient();
                                succStorArgs = parseCommandFromClient(succStorReturn);
                                if (succStorArgs.length == 2) {
                                    String sizeArg = succStorArgs[0].toUpperCase();
                                    long numberOfBytes;
                                    try {
                                        numberOfBytes = Long.parseLong(succStorArgs[1]);
                                    } catch (NumberFormatException e) {
                                        sendTextToClient("-please enter a valid number of bytes");
                                        break;
                                    }

                                    switch (sizeArg) {
                                        case "SIZE":
                                            if (numberOfBytes <= new File(currentWorkingDirectory).getUsableSpace()) {
                                                sendTextToClient("+ok, waiting for file");
                                                byte[] fileAsBytes = receiveBytes(numberOfBytes);
                                                Files.write(potentialFile.toPath(), fileAsBytes, StandardOpenOption.APPEND);
                                                sendTextToClient("+Saved " + potentialFile.getName());
                                                //sendTextToClient("-Couldn't save because (reason)");
                                            } else {
                                                sendTextToClient("-Not enough room, don't send it");
                                            }
                                            break;
                                        default:
                                            sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                            break;
                                    }
                                } else {
                                    sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                }
                            } else {
                                sendTextToClient("+Will create file");
                                succStorReturn = receiveTextFromClient();
                                succStorArgs = parseCommandFromClient(succStorReturn);
                                if (succStorArgs.length == 2) {
                                    String sizeArg = succStorArgs[0].toUpperCase();
                                    long numberOfBytes;
                                    try {
                                        numberOfBytes = Long.parseLong(succStorArgs[1]);
                                    } catch (NumberFormatException e) {
                                        sendTextToClient("-please enter a valid number of bytes");
                                        break;
                                    }

                                    switch (sizeArg) {
                                        case "SIZE":
                                            if (numberOfBytes <= new File(currentWorkingDirectory).getUsableSpace()) {
                                                sendTextToClient("+ok, waiting for file");
                                                byte[] fileAsBytes = receiveBytes(numberOfBytes);
                                                Files.write(potentialFile.toPath(), fileAsBytes);
                                                sendTextToClient("+Saved " + potentialFile.getName());
                                                //sendTextToClient("-Couldn't save because (reason)");
                                            } else {
                                                sendTextToClient("-Not enough room, don't send it");
                                            }
                                            break;
                                        default:
                                            sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                            break;
                                    }
                                } else {
                                    sendTextToClient("-please enter a SEND command followed by the size of the file after a STOR command");
                                }
                            }
                            break;
                        default:
                            String invalidArgument = "-Invalid argument, please send NEW, OLD or APP as the second argument";
                            sendTextToClient(invalidArgument);
                            break;
                    }
                } else {
                    sendTextToClient(notEnoughArgs);
                }
            } else {
                sendTextToClient(notLoggedIn);
            }
        }

        // Below are helper functions. They are called in the specified commands but have been moved into their own
        // functions to avoid duplication
        /****************************************   Helper Functions   ************************************************/
        private String[] parseCommandFromClient(String commandFromClient) {
            String[] tokenizedCommand = commandFromClient.trim().split("\\s+", 3);
            return tokenizedCommand;
        }

        // Removes null values from an array.
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
            sendBytes(byteArrayWithNull);
            System.out.println("Sent [ " + string + " ] return message to client: " + clientSocket);
            System.out.println("");
        }

        private String receiveTextFromClient() throws IOException {
            byte[] messageBuffer = receiveBytes(10000000);

            String messageFromClient = new String(removeNull(messageBuffer));
            return messageFromClient;
        }

        private void sendBytes(byte[] bytesToSend) throws IOException {
            dataToClientFromServer.write(bytesToSend, 0, bytesToSend.length);
            dataToClientFromServer.flush();
        }

        private byte[] receiveBytes(long numberOfbytes) throws IOException {
            byte[] returnedBytes = new byte[(int)numberOfbytes];
            int bytesRead = 0;
            boolean nullDetected = false;
            byte messageByte;

            while(!nullDetected && bytesRead < (int) numberOfbytes){
                messageByte = dataFromClientToServer.readByte();
                if(messageByte != 0) {
                    returnedBytes[bytesRead] = messageByte;
                    bytesRead++;
                } else {
                    nullDetected = true;
                    bytesRead = 0;
                }
            }

            return returnedBytes;
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

        // Forwards the LIST call to cmd.exe
        private String callLsFromCommandLine(String pathToDirectory, boolean isVerbose) throws IOException, InterruptedException {
            String[] windowsArgs = null;
            if(isVerbose) {
                windowsArgs = new String[] {"cmd.exe", "/c", "cd && dir", pathToDirectory, "/q"};
            } else {
                windowsArgs = new String[] {"cmd.exe", "/c", "cd && dir", pathToDirectory, "/b"};
            }
            Process pr = new ProcessBuilder(windowsArgs).start();

            BufferedReader input =
                    new BufferedReader(new InputStreamReader(pr.getInputStream()));

            BufferedReader error =
                    new BufferedReader(new InputStreamReader(pr.getErrorStream()));

            StringBuilder inputBuilder = new StringBuilder();
            String line = null;
            while ( (line = input.readLine()) != null) {
                inputBuilder.append(line);
                inputBuilder.append(System.getProperty("line.separator"));
            }

            StringBuilder errorBuilder = new StringBuilder();
            String errorline = null;
            while ( (errorline = error.readLine()) != null) {
                errorBuilder.append(errorline);
                errorBuilder.append(System.getProperty("line.separator"));
            }

            String result = inputBuilder.toString();
            String errorResult = errorBuilder.toString();
            String returned = result + errorResult;

            int exitCode = pr.waitFor();

            int firstIndexOfPWD = returned.indexOf(":")-1;
            int lastIndexOfPWD = returned.indexOf("\n")+1;
            String errorCodeMessage = "+" + returned.substring(firstIndexOfPWD, lastIndexOfPWD);

            if (exitCode==0) {
                if (isVerbose) {
                    int substringIndexStart = ordinalIndexOf(returned, "\n", 5) + 1;
                    int substringIndexEnd = nthLastIndexOf(3, "\n", returned) + 1;
                    String output = errorCodeMessage + returned.substring(substringIndexStart, substringIndexEnd);
                    return output;
                } else {
                    return "+" + returned;
                }
            } else {
                if (isVerbose) {
                    int index = ordinalIndexOf(returned, "\n", 5) + 1;
                    returned = returned.substring(index);
                    return "-" + returned;
                } else {
                    int index = returned.indexOf("\n") + 1;
                    returned = returned.substring(index);
                    return "-" + returned;
                }
            }
        }

        // Returns the index of the nth occurrence of a char in a string
        private int ordinalIndexOf(String str, String substr, int n) {
            int pos = -1;
            do {
                pos = str.indexOf(substr, pos + 1);
            } while (n-- > 0 && pos != -1);
            return pos;
        }

        // Returns the index of the nth Last occurrence of a char in a string
        private int nthLastIndexOf(int nth, String ch, String string) {
            if (nth <= 0) return string.length();
            return nthLastIndexOf(--nth, ch, string.substring(0, string.lastIndexOf(ch)));
        }
    }

    /******************************************** Server entry code ***************************************************/
    public static void main(String args[]) throws IOException {
        int inputPort = 5000;
        Server server = new Server();
        server.start(5000 + inputPort - inputPort);
    }
}
