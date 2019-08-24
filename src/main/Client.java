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

    private ObjectOutputStream dataFromClientToServer;
    private ObjectInputStream dataFromServerToClient;

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

        try {
            // create an object output stream from the output stream so we can send an object through it
            dataFromClientToServer = new ObjectOutputStream(clientSocket.getOutputStream());
            dataFromClientToServer.writeObject(messageToServer);
            System.out.println("Sent Message: " + messageToServer);

            dataFromServerToClient = new ObjectInputStream(clientSocket.getInputStream());
            String returnedMessage = (String) dataFromServerToClient.readObject();
            System.out.println("Received [" + returnedMessage + "] from server");

        } catch (SocketException e) {
            System.out.println("Server socket closed");
            stopConnection();
        } catch (IOException | ClassNotFoundException e) {
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

    public static void main(String args[])
    {
        System.out.println("Hello World");
        Client client = new Client();
        client.startConnection("127.0.0.1", 5000);

        System.out.println("Disconnected from server");
        client.stopConnection();
    }
}
