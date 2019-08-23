package main;

import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String args[])
    {
        String inputFromUser = "";
        Scanner scan = new Scanner(System.in);
        System.out.println("All text input will be echoed to the server. Type 'exit' to quit");

        while (!inputFromUser.toLowerCase().equals("exit")) {
            inputFromUser = scan.nextLine();
            System.out.println(inputFromUser);
        }

        System.out.println("Disconnected from server");

//        System.out.println("Hello World");
//        Client client = new Client();
//        client.startConnection("127.0.0.1", 5000);
//        client.sendMessage("Hello World No. " + new Random().nextInt(50));
//        client.stopConnection();
    }
}
