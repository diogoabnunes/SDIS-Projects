package main.chord.message;

import main.chord.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ChordMessageHandlerThread implements Runnable {
    /**
     * Destiny of received messages
     */
    private Chord chord;

    /**
     * Socket through which we will receive messages
     */
    private ServerSocket socket;

    public ChordMessageHandlerThread(Chord chord) {
        this.chord = chord;

        try {
            this.socket = new ServerSocket(chord.getChordNode().getSelfInfo().getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Leads with requests
     */
    public void run() {
        try {
            while (true) {
                Socket comSocket = this.socket.accept();

                PrintWriter out = new PrintWriter(comSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(comSocket.getInputStream()));

                this.handleRequest(in, out);

                comSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes wich type of request it is
     * @param in -> inputStream
     * @param out -> outputStream
     * @throws IOException
     */
    private void handleRequest(BufferedReader in, PrintWriter out) throws IOException {
        String request = in.readLine();
        String[] split_request = request.split(" ");

        if (split_request[0].equals("FINDSPOT_REQUEST")) out.println(this.chord.findSpot(request));
        else if (split_request[0].equals("UPDATE_SUCCESSOR")) this.chord.updateSuccessor(request);
        else if (split_request[0].equals("UPDATE_PREDECESSOR")) this.chord.updatePredecessor(request);
        else if (split_request[0].equals("UPDATE_FINGERTABLE")) {
            this.chord.updateFingerTable(request);
            this.chord.print();
        }
        else if (split_request[0].equals("LOOKUP_REQUEST")) out.println(this.chord.lookup(request));
        else if (split_request[0].equals("UPDATESTATE")) this.chord.updateState(request);
        else if (split_request[0].equals("GETSUCCESSOR")) out.println(this.chord.getPredecessor());
        else if (split_request[0].equals("GETAVAILABLEPEERS")) out.println(this.chord.getAvailablePeers(request));
        else if (split_request[0].equals("GETPEERINFO")) out.println(this.chord.getPeerInfo());
        else if (split_request[0].equals("FREEID")) out.println(this.chord.freeId(request));
    }
}
