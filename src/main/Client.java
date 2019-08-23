package main;

import java.io.*;
import java.net.Socket;
import java.util.Random;

public class Client {

    // initialize socket and input output streams
    private Socket clientSocket;

    // constructor to put ip address and port
    public Client() {
    }

    public void stopConnection() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String messageToServer) {

        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            // create an object output stream from the output stream so we can send an object through it
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(messageToServer);
            System.out.println("Sent Message: " + messageToServer);

            InputStream inputStream = clientSocket.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            String returnedMessage = (String) objectInputStream.readObject();
            System.out.println("Received [" + returnedMessage+ "] from server");

            // block until return message is received.
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void startConnection(String address, int port) {
        // establish a connection
        try {
            clientSocket = new Socket(address, port);
            System.out.println("Connected");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
