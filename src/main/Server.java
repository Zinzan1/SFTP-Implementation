package main;

import com.sun.deploy.util.StringUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

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

        private ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                // create in/output streams to send byte data to/from client
                dataFromClientToServer = new DataInputStream(clientSocket.getInputStream());
                dataToClientFromServer = new DataOutputStream(clientSocket.getOutputStream());
                sendTextToClient(Server.POS_GREETING);
                System.out.println(currentWorkingDirectory);

                while (connectionIsOpen) {
                    String messageFromClient = receiveTextFromClient();
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

            System.out.println("Thread Dead");
        }

        private void userCommand(String[] args) throws IOException {
            String success = "+User-id valid, send account and password";
            String error = "-Invalid user-id, try again";

            if (isFullyAuthenticated() || userHasBeenGiven()) {
                String alreadyLoggedIn = "! " + user + " logged in";
                sendTextToClient(alreadyLoggedIn);
            } else {
                if (args.length == 2) {
                    if (args[1].equals(superUserID)) {
                        user = superUserID;
                        account = superUserID;
                        pass = superUserID;
                        isSuperUser = true;
                        String alreadyLoggedIn = "! " + user + " logged in";
                        sendTextToClient(alreadyLoggedIn);
                    }
                    else if(userIdExists(args[1])) {
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

            if (isFullyAuthenticated()) {
                sendTextToClient(loggedIn);
            } else {
                if (userHasBeenGiven()) {
                    if(args.length == 2) {
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

        //TODO use helper function (uses cmd or terminal)
        private void listCommand(String[] args) throws IOException, InterruptedException {
            String wrongArg = "-Please provide 'F' or 'V' as the second argument";
            String error = "-cannot access '" + "your arg" + "': No such file or directory";

            File curDir = new File(currentWorkingDirectory);
            String[] filesInThisDir = curDir.list();

            if (isFullyAuthenticated()) {
                if (args.length == 2) {
                    String arg = args[1].toUpperCase();
                    String lsReturn = callLsFromCommandLine(currentWorkingDirectory);
                    switch(arg) {
                        case "F":
                            System.out.println("Client arg was F");
                            sendTextToClient(lsReturn);
                            break;
                        case "V":
                            System.out.println("Client arg was V");
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
                    String lsReturn = callLsFromCommandLine(pathToDir);
                    switch(arg3) {
                        case "F":
                            System.out.println("Client arg was F");
                            sendTextToClient(lsReturn);
                            break;
                        case "V":
                            System.out.println("Client arg was V");
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

                    if (proposedDir.exists()) {
                        if (proposedDir.isDirectory()) {
                            if (!currentWorkingDirectory.equals(arg)) {
                                if (isSuperUser) {
                                    currentWorkingDirectory = proposedDir.getAbsolutePath();
                                    passGiven = true;
                                    acctGiven = true;
                                    String changed = "!Changed working dir to " + proposedDir.getAbsolutePath();
                                    sendTextToClient(changed);
                                } else {
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

        private void killCommand(String[] args) throws IOException {
            String fileNotExist = "-Not deleted because: file doesn't exist";
            String notAtDir = "-Not deleted because: file is a directory";

            if (isFullyAuthenticated()) {
                if (args.length == 2) {
                    String arg = args[1];
                    File proposedFileForDeletion= new File(currentWorkingDirectory + System.lineSeparator() + arg);

                    if (proposedFileForDeletion.exists()) {
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

        private void doneCommand(String[] args) throws IOException {
            String success = "+Your account has been invoiced";
            String tooManyArgs = "-Done requires no arguments";
            if (isFullyAuthenticated()) {
                if (args.length == 1) {
                    sendTextToClient(success);
                    clientSocket.close();
                    connectionIsOpen = false;
                }
                else {
                    sendTextToClient(tooManyArgs);
                }
            } else {
                sendTextToClient(notLoggedIn);
            }
        }

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


        /****************************************   Helper Functions   ************************************************/
        private String[] parseCommandFromClient(String commandFromClient) {
            String[] tokenizedCommand = commandFromClient.trim().split("\\s+", 3);
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

        private String callLsFromCommandLine(String pathToDirectory) throws IOException, InterruptedException {
            String[] linuxArgs = new String[] {"/bin/bash", "-c", "ls", "-l"};
            String[] windowsArgs = new String[] {"cmd.exe", "/c", "dir", pathToDirectory, "/q"};
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
            int substringIndex = ordinalIndexOf(returned, "\n", 4);
            String output = returned.substring(substringIndex);
            System.out.println(output);

            int exitCode = pr.waitFor();
            System.out.println("\nExited with error code : " + exitCode);
            return result;
        }

        private int ordinalIndexOf(String str, String substr, int n) {
            int pos = -1;
            do {
                pos = str.indexOf(substr, pos + 1);
            } while (n-- > 0 && pos != -1);
            return pos;
        }
    }

    /******************************************** Server entry code ***************************************************/
    public static void main(String args[]) throws IOException {
        int inputPort = 5000;
        Server server = new Server();
        server.start(5000 + inputPort - inputPort);
    }
}
