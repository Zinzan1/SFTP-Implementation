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
    private String MessageToServer;

    private DataOutputStream dataToServerFromClient;
    private DataInputStream dataFromServerToClient;

    // constructor to put ip address and port
    private Client() {
    }

    private void stopConnection() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String messageToServer) {
        boolean atEndOfString = false;
        try {
            // create an object output stream from the output stream so we can send an object through it
            dataToServerFromClient = new DataOutputStream(clientSocket.getOutputStream());
            dataFromServerToClient = new DataInputStream(clientSocket.getInputStream());

            byte[] stringAsByteArray = messageToServer.getBytes("ISO646-US");
            byte[] byteArrayWithNull = new byte[stringAsByteArray.length + 1];
            System.arraycopy(stringAsByteArray,0, byteArrayWithNull, 0, stringAsByteArray.length);
            dataToServerFromClient.write(byteArrayWithNull, 0, byteArrayWithNull.length);
            dataToServerFromClient.flush();

            System.out.println("Sent Message: " + messageToServer+"\0");

            byte[] messageBuffer = new byte[1000];
            int bytesRead  = 0;
            boolean nullDetected = false;

            while(!nullDetected){
                byte messageByte = dataFromServerToClient.readByte();

                if(messageByte != 0) {
                    messageBuffer[bytesRead] = messageByte;
                    bytesRead++;
                } else {
                    nullDetected = true;
                    bytesRead = 0;
                }
            }

            String returnedMessage = new String(removeNull(messageBuffer));
            System.out.println("Received [" + returnedMessage + "] from server");

        } catch (SocketException e) {
            System.out.println("Server socket closed");
            stopConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startConnection(String address, int port) {
        // establish a connection
        try {
            clientSocket = new Socket(address, port);
            System.out.println("Connected");

            String inputFromUser = "";
            Scanner scan = new Scanner(System.in);
            System.out.println("All text input will be echoed to the server. Type 'exit' to quit");

            while (!inputFromUser.toLowerCase().equals("done")) {
                System.out.print("Please enter a command: ");
                inputFromUser = scan.nextLine();
                this.sendMessage(inputFromUser);
            }

        } catch (ConnectException e){
            System.out.println("Server refused the connection");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return;
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

    public static void main(String args[])
    {
        System.out.println("Hello World");
        Client client = new Client();
        client.startConnection("127.0.0.1", 5000);

        System.out.println("Disconnected from server");
        client.stopConnection();
    }
}
