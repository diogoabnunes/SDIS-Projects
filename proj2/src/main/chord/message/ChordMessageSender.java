package main.chord.message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChordMessageSender {
    public static String sendMessageAndWait(String message, String address, int port) {
        try {
            Socket socket = new Socket(address, port);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(message);
            String response = in.readLine();

            out.close();
            in.close();
            socket.close();

            return response;
        } catch (IOException e) {
            System.out.println("Chord - JOIN REQUEST - Error in JOIN request");
            e.printStackTrace();

            return null;
        }
    }

    public static void sendMessageAndReturn(String message, String address, int port) {
        try {
            Socket socket = new Socket(address, port);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(message);

            out.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Chord - JOIN REQUEST - Error in JOIN request");
            e.printStackTrace();
        }
    }
}
