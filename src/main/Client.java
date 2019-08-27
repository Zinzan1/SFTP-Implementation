package main;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.Scanner;

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

    private void sendMessage(String messageToServer) throws IOException {
        boolean atEndOfString = false;
        String[] commandAsTokens = parseCommandFromClient(messageToServer);
        String upperCommand = commandAsTokens[0].toUpperCase();

        switch (upperCommand) {
            case "CDIR":
                sendTextToServer(messageToServer);
                String cdirReturnedMessage = receiveTextFromServer();

                if (String.valueOf(cdirReturnedMessage.charAt(0)).equals("+")) {
                    String inputForCDIR;
                    while (!String.valueOf(cdirReturnedMessage.charAt(0)).equals("!") || !String.valueOf(cdirReturnedMessage.charAt(0)).equals("-")) {
                        inputForCDIR= scan.nextLine();
                        sendTextToServer(inputForCDIR);
                        cdirReturnedMessage = receiveTextFromServer();
                    }
                } else if (String.valueOf(cdirReturnedMessage.charAt(0)).equals("-")) {
                    System.out.println("done CDIR -");
                } else if (String.valueOf(cdirReturnedMessage.charAt(0)).equals("!")) {
                    System.out.println("done CDIR !");
                } else {
                    System.out.println("-An unexpected error has occurred");
                }
                break;
            case "DONE":
                // close connection
                System.out.println("Closing connection");
                stopConnection();
                break;
            case "NAME":
                sendTextToServer(messageToServer);
                String nameReturnedMessage = receiveTextFromServer();

                if (String.valueOf(nameReturnedMessage.charAt(0)).equals("+")) {
                    String inputForTOBE = scan.nextLine();
                    sendTextToServer(inputForTOBE);
                    receiveTextFromServer();

                } else if (String.valueOf(nameReturnedMessage.charAt(0)).equals("-")) {

                } else {
                    System.out.println("-An unexpected error has occurred");
                }
                break;
            case "RETR":
                break;
            case "STOR":
                break;
            default:
                sendTextToServer(messageToServer);
                String returnedMessage = receiveTextFromServer();
                break;
        }
    }

    private void startConnection(String address, int port) throws IOException {
        // establish a connection
        clientSocket = new Socket(address, port);
        dataToServerFromClient = new DataOutputStream(clientSocket.getOutputStream());
        dataFromServerToClient = new DataInputStream(clientSocket.getInputStream());

        String returnedMessage = receiveTextFromServer();
        System.out.println("Received [" + returnedMessage + "] from server");

        System.out.println("Connected");

        String inputFromUser = "";
        scan = new Scanner(System.in);
        System.out.println("All text input will be echoed to the server. Type 'exit' to quit");

        while (!inputFromUser.toLowerCase().equals("done")) {
            System.out.print("Please enter a command: ");
            inputFromUser = scan.nextLine();
            this.sendMessage(inputFromUser);
        }
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

    private String[] parseCommandFromClient(String commandFromClient) {
        String[] tokenizedCommand = commandFromClient.trim().split("\\s+");
//            for (String command : tokenizedCommand) {
////                System.out.println(command);
////            }
        return tokenizedCommand;
    }

    private void sendTextToServer(String string) throws IOException {
        byte[] stringAsByteArray = string.getBytes("ISO646-US");
        byte[] byteArrayWithNull = new byte[stringAsByteArray.length + 1];
        System.arraycopy(stringAsByteArray,0, byteArrayWithNull, 0, stringAsByteArray.length);
        dataToServerFromClient.write(byteArrayWithNull, 0, byteArrayWithNull.length);
        dataToServerFromClient.flush();
        System.out.println("Sent Message: " + string + "\0");
    }

    private String receiveTextFromServer() throws IOException {
        byte[] messageBuffer = new byte[1000];
        int bytesRead = 0;
        boolean nullDetected = false;

        while (!nullDetected) {
            byte messageByte = dataFromServerToClient.readByte();

            if (messageByte != 0) {
                messageBuffer[bytesRead] = messageByte;
                bytesRead++;
            } else {
                nullDetected = true;
                bytesRead = 0;
            }
        }

        String returnedMessage = new String(removeNull(messageBuffer));
        System.out.println("Received message: " + returnedMessage + " from server");
        return returnedMessage;
    }

    public static void main(String args[]) throws IOException {
        System.out.println("Hello World");
        Client client = new Client();
        client.startConnection("127.0.0.1", 5000);

        System.out.println("Disconnected from server");
        client.stopConnection();
    }
}
