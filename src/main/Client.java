package main;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;

// Below is the code for the client.
public class Client {

    // initialize socket and input output streams
    private Socket clientSocket;
    private Scanner scan;

    private DataOutputStream dataToServerFromClient;
    private DataInputStream dataFromServerToClient;

    // constructor to put ip address and port
    private Client() {
    }

    private void stopConnection() throws IOException {
        clientSocket.close();
    }

    // This is the bulk of the code on the client end
    // I have tried to defer most of the actual processing to the server whenever possible
    // However, some commands require the client to be in a certain state.
    // Those are the commands that are in the switch statement.
    private void sendMessage(String messageToServer) throws IOException {
        boolean atEndOfString = false;
        String[] commandAsTokens = parseCommandFromClient(messageToServer);
        String upperCommand = commandAsTokens[0].toUpperCase();

        switch (upperCommand) {
            case "CDIR":
                sendTextToServer(messageToServer);
                String cdirReturnedMessage = receiveTextFromServer(true);

                if (String.valueOf(cdirReturnedMessage.charAt(0)).equals("+")) {
                    String inputForCDIR;
                    while (!String.valueOf(cdirReturnedMessage.charAt(0)).equals("!") || !String.valueOf(cdirReturnedMessage.charAt(0)).equals("-")) {
                        inputForCDIR = scan.nextLine();
                        sendTextToServer(inputForCDIR);
                        cdirReturnedMessage = receiveTextFromServer(true);
                    }
                } else if (String.valueOf(cdirReturnedMessage.charAt(0)).equals("-")) {

                } else if (String.valueOf(cdirReturnedMessage.charAt(0)).equals("!")) {

                } else {
                    System.out.println("-An unexpected error has occurred");
                }
                break;
            case "DONE":
                // close connection
                stopConnection();
                break;
            case "NAME":
                sendTextToServer(messageToServer);
                String nameReturnedMessage = receiveTextFromServer(true);

                if (String.valueOf(nameReturnedMessage.charAt(0)).equals("+")) {
                    String inputForTOBE = scan.nextLine();
                    sendTextToServer(inputForTOBE);
                    String tobeReturnMessage = receiveTextFromServer(true);

                } else if (String.valueOf(nameReturnedMessage.charAt(0)).equals("-")) {

                } else {
                    System.out.println("-An unexpected error has occurred");
                }
                break;
            case "RETR":
                sendTextToServer(messageToServer);
                String retrReturnedMessage = receiveTextFromServer(true);

                if (String.valueOf(retrReturnedMessage.charAt(0)).equals(" ")) {
                    int numberOfBytes = (int) Long.parseLong(retrReturnedMessage.substring(1));
                    String inputForCDIR;
                    inputForCDIR = scan.nextLine();
                    sendTextToServer(inputForCDIR);
                    retrReturnedMessage = receiveTextFromServer(false);

                    if (!String.valueOf(retrReturnedMessage.charAt(0)).equals("*")) {
                        System.out.println(retrReturnedMessage);
                    } else {
                        String nameOfRemoteFile = retrReturnedMessage.substring(1);
                        String pathOfFile = System.getProperty("user.dir") + File.separator + nameOfRemoteFile;
                        System.out.println(System.getProperty("user.dir"));
                        byte[] fileAsBytes = receiveBytes(numberOfBytes);
                        Files.write(new File(pathOfFile).toPath(), fileAsBytes);
                        break;
                    }
                } else if (String.valueOf(retrReturnedMessage.charAt(0)).equals("-")) {

                } else {
                    System.out.println("-An unexpected error has occurred");
                }
                break;
            case "STOR":
                if (commandAsTokens.length == 3) {
                    String nameOfFile = commandAsTokens[2];
                    String pathOfFile = System.getProperty("user.dir") + File.separator + nameOfFile;
                    File fileToBeSent = new File(pathOfFile);
                    if (fileToBeSent.exists()) {
                        // Send STOR command
                        sendTextToServer(messageToServer);

                        // Response from server
                        String storReturnedMessage = receiveTextFromServer(true);

                        // If response code is success
                        if (String.valueOf(storReturnedMessage.charAt(0)).equals("+")) {
                            String inputForCDIR = scan.nextLine();
                            sendTextToServer(inputForCDIR);
                            storReturnedMessage = receiveTextFromServer(true);

                            // Server response for SIZE command
                            if (String.valueOf(storReturnedMessage.charAt(0)).equals("+")) {
                                byte[] bytesToSend = Files.readAllBytes(fileToBeSent.toPath());
                                sendBytes(bytesToSend);
                                receiveTextFromServer(true);
                            } else if (String.valueOf(storReturnedMessage.charAt(0)).equals("-")) {

                            } else {
                            }

                            // If response code is a failure
                        } else if (String.valueOf(storReturnedMessage.charAt(0)).equals("-")) {

                            // Safety else statement (shouldn't be executed)
                        } else {
                            System.out.println("-An unexpected error has occurred");
                        }
                    } else {
                        System.out.println("-Please enter a valid file as the 3rd argument");
                    }
                } else {
                    System.out.println("-Please provide STOR in the form: STOR { NEW | OLD | APP } file-spec");
                }
                break;
            default:
                sendTextToServer(messageToServer);
                String returnedMessage = receiveTextFromServer(true);
                break;
        }
    }

    // Called in the beginning to connect to the server.
    private void startConnection(String address, int port) throws IOException {
        // establish a connection
        clientSocket = new Socket(address, port);
        dataToServerFromClient = new DataOutputStream(clientSocket.getOutputStream());
        dataFromServerToClient = new DataInputStream(clientSocket.getInputStream());

        String returnedMessage = receiveTextFromServer(true);

        String inputFromUser = "";
        scan = new Scanner(System.in);

        while (!inputFromUser.toLowerCase().equals("done")) {
            inputFromUser = scan.nextLine();
            this.sendMessage(inputFromUser);
        }
    }

    // Removes null values from a byte array
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

    private String[] parseCommandFromClient(String commandFromClient) {
        String[] tokenizedCommand = commandFromClient.trim().split("\\s+");
        return tokenizedCommand;
    }

    private void sendTextToServer(String string) throws IOException {
        byte[] stringAsByteArray = string.getBytes("ISO646-US");
        byte[] byteArrayWithNull = new byte[stringAsByteArray.length + 1];
        System.arraycopy(stringAsByteArray,0, byteArrayWithNull, 0, stringAsByteArray.length);
        sendBytes(byteArrayWithNull);
        System.out.println(string + "\0");
    }

    private String receiveTextFromServer(boolean printOutput) throws IOException {

        byte[] messageBuffer = receiveBytes(10000000);
        String returnedMessage = new String(removeNull(messageBuffer));
        if (printOutput) {
            System.out.println(returnedMessage);
        }
        return returnedMessage;
    }

    private void sendBytes(byte[] bytesToSend) throws IOException {
        dataToServerFromClient.write(bytesToSend, 0, bytesToSend.length);
        dataToServerFromClient.flush();
    }

    private byte[] receiveBytes(long numberOfbytes) throws IOException {
        byte[] returnedBytes = new byte[(int)numberOfbytes];
        int bytesRead = 0;
        boolean nullDetected = false;
        byte messageByte;

        while (!nullDetected && bytesRead < (int) numberOfbytes) {
            messageByte = dataFromServerToClient.readByte();

            if (messageByte != 0) {
                returnedBytes[bytesRead] = messageByte;
                bytesRead++;
            } else {
                nullDetected = true;
                bytesRead = 0;
            }
        }

        return returnedBytes;
    }

    public static void main(String args[]) throws IOException {
        Client client = new Client();
        client.startConnection("127.0.0.1", 5000);

        System.out.println("Disconnected from server");
        client.stopConnection();
    }
}
