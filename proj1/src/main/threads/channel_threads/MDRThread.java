package main.threads.channel_threads;

import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

import main.*;
import main.auxiliar.*;

public class MDRThread extends Thread {
    private final Peer peer;
    private final MulticastSocket mdrSocket;

    public MDRThread(Peer peer) {
        this.peer = peer;
        this.mdrSocket = this.peer.getMdrSocket();
    }
    
    /**
     * Thread of MDR multicast channel
     */
    public void run() {
        try {
            while (true) {
                byte[] buf = new byte[64500];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);

                this.mdrSocket.receive(datagramPacket);

                this.processMessage(buf);
            }
        } catch (Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong in MDR thread. Aborting..." + ConsoleColors.RESET);
        }
    }

    /**
     * Processes the message received from the multicast socket
     * @param buf -> received message
     */
    public void processMessage(byte[] buf) {
        String message = new String(buf, 0, Auxiliar.getFirstNullByteIndex(buf), StandardCharsets.UTF_8);

        int splitIndex = message.indexOf("\r\n\r\n"); // gets the index of this in the message
        String header = message.substring(0, splitIndex);
        String body = message.substring(splitIndex + 4);
        String[] headerParts = header.split(" ");

        String fileId = headerParts[3];
        String chunkN = headerParts[4];
        String key = fileId + "$" + chunkN;

//        System.out.println(ConsoleColors.PURPLE + "MDR THREAD (BEGIN)" + ConsoleColors.RESET);

        System.out.println(ConsoleColors.CYAN + "-> Received CHUNK message: " + header + " <body>" + ConsoleColors.RESET);

        if (this.peer.addChunkContentToRestore(key, body))
            System.out.println("-> Storing the content of the chunk: " + key);
        else
            System.out.println("-> The content of the chunk: " + key + " has already been stored...");

//        System.out.println(ConsoleColors.PURPLE + "MDR THREAD (END)" + ConsoleColors.RESET);
    }
}
