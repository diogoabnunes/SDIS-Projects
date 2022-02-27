package main.threads.channel_threads;

import main.*;
import main.auxiliar.*;

import java.net.*;
import java.nio.charset.StandardCharsets;

import java.io.File;
import java.io.FileWriter;

import java.util.Random;

import java.util.*;

public class MDBThread extends Thread {
    private final Peer peer;
    private final MulticastSocket mdbSocket;
    private final MulticastSocket socket;
    private final Random random;

    private boolean captureMessages;
    private final ArrayList<String> chunksRequested;

    public MDBThread(Peer peer) throws Exception {
        this.peer = peer;
        this.mdbSocket = this.peer.getMdbSocket();
        this.socket = new MulticastSocket();
        this.random = new Random();

        this.captureMessages = false;
        this.chunksRequested = new ArrayList<>();
    }

    public synchronized void startCapturePUTCHUNKmessages() {
        this.captureMessages = true;
    }

    public synchronized void stopCapturePUTCHUNKmessages() {
        this.captureMessages = false;
    }

    public synchronized boolean receivePUTCHUNKmessageForChunk(String chunkId) {
        boolean in = false;

        for (String s : this.chunksRequested) {
            if (s.equals(chunkId)) {
                in = true;
                break;
            }
        }

        this.chunksRequested.clear();

        return in;
    }

    /**
     * Thread of MDB multicast channel
     */
    public void run() {
        try {
            while (true) {
                byte[] buf = new byte[64500];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);

                this.mdbSocket.receive(datagramPacket);

                this.processMessage(buf);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong in MDB Thread..." + ConsoleColors.RESET);
        }
    }

    /**
     * Processes the message received from the multicast socket
     * @param buf -> received message
     * @throws Exception: SaveChunk and sendSTOREDmessage
     */
    private void processMessage(byte[] buf) throws Exception {
        String message = new String(buf, 0, Auxiliar.getFirstNullByteIndex(buf), StandardCharsets.UTF_8);

        int splitIndex = message.indexOf("\r\n\r\n"); // gets the index of this in the message
        String header = message.substring(0, splitIndex);
        String body = message.substring(splitIndex + 4);

        String[] headerParts = header.split(" ");

        int senderId = Integer.parseInt(headerParts[2]);
        if (senderId == this.peer.getPeerId())
            return; // it was this peer that send the message so there is no need to process it

        if (this.captureMessages) this.chunksRequested.add(headerParts[3] + "$" + headerParts[4]);

//        System.out.println(ConsoleColors.PURPLE + "MDB THREAD (BEGIN)" + ConsoleColors.RESET);

        System.out.println(ConsoleColors.CYAN + "\n-> Received PUTCHUNK message: " + header + " <body>" + ConsoleColors.RESET);

        String chunkName = headerParts[3] + "$" + headerParts[4];
        if (this.saveChunk(chunkName, body, Integer.parseInt(headerParts[4]), headerParts[5]))
            this.sendSTOREDmessage(headerParts[0], headerParts[3], headerParts[4]);

//        System.out.println(ConsoleColors.PURPLE + "MDB THREAD (END)" + ConsoleColors.RESET);
    }

    /**
     * Saves the chunk if it has not been saved yet
     * @param chunkName -> name of the chunk
     * @param body -> content of the chunk
     * @param chunkNo -> number of this chunk
     * @return -> true if it saves , false otherwise
     * @throws Exception: maxDiskSpaceAvailableForChunks and usedDiskSpaceForChunks
     */
    private boolean saveChunk(String chunkName, String body, int chunkNo, String repDegree) throws Exception {
        String path = "../../dirs/" + this.peer.getPeerId() + "/chunks/" + chunkName;
        File file = new File(path);

        int maxSize = this.peer.maxDiskSpaceAvailableForChunks();
        int usedSize = (int)this.peer.usedDiskSpaceForChunks();

        String fileId = chunkName.substring(0, chunkName.indexOf("$"));

        if (file.exists()) {
            System.out.println(ConsoleColors.YELLOW + "-> The chunk: " + chunkNo + " has already been backed up..." + ConsoleColors.RESET);
            return false;
        }
        else if (this.peer.fileBackedUpByThisPeer(fileId)) {
            System.out.println(ConsoleColors.YELLOW + "-> This peer initiated the backup for the file that contains that chunk." + ConsoleColors.RESET);
            return false;
        }
        else if (maxSize != -1 && (usedSize + body.length()) > maxSize) {
            System.out.println(ConsoleColors.YELLOW + "-> The chunk: " + chunkNo + " is too big..." + ConsoleColors.RESET);
            return false;
        }

        System.out.println("-> Saving chunk: " + chunkNo + ";\n->Chunk name: " + chunkName);

        FileWriter writer = new FileWriter(file);
        writer.write(body, 0, body.length());
        writer.close();

        this.peer.addChunkStored(chunkName, repDegree);

        System.out.println(ConsoleColors.GREEN + "-> Chunk saved." + ConsoleColors.RESET);

        return true;
    }

    /**
     * Sends STORED to MC multicast data channel
     * @param protocol -> version of the protocol
     * @param fileId -> id do file
     * @param chunkNo -> number of this chunk
     * @throws Exception: Sleep and send packet
     */
    private void sendSTOREDmessage(String protocol, String fileId, String chunkNo) throws Exception {
        Thread.sleep(this.random.nextInt(401)); // waits a random time between 0 and 400 ms

        System.out.println(ConsoleColors.CYAN + "-> Sending STORED message." + ConsoleColors.RESET);

        byte[] message = this.buildSTOREDmessage(protocol, fileId, chunkNo);
        DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(this.peer.getMcAddress()), this.peer.getMcPort());
        this.socket.send(datagramPacket);
    }

    /**
     * Builds the STORED message to be sent to MC multicast chanel
     * @param protocol -> version of the protocol
     * @param fileId -> id of the file
     * @param chunkNo -> number of the chunk
     * @return message to be sent in bytes
     */
    private byte[] buildSTOREDmessage(String protocol, String fileId, String chunkNo) {
        String message = protocol + " STORED " + this.peer.getPeerId() + " " + fileId + " " + chunkNo + "\r\n\r\n";
        return message.getBytes();
    }

}