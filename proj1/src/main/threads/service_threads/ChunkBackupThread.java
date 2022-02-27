package main.threads.service_threads;

import main.*;
import main.auxiliar.ConsoleColors;

import java.net.*;

import java.io.File;
import java.io.FileReader;

public class ChunkBackupThread extends Thread {
    private String  chunkId;
    private final Peer peer;
    private final MulticastSocket sendSocket;

    public ChunkBackupThread(Peer peer, String message) throws Exception {
        this.peer = peer;

        this.processMessage(message);

        this.sendSocket = new MulticastSocket();

    }

    private void processMessage(String message) {
        this.chunkId = message;
    }

    /**
     * Verifies if it is possible to backup the specified file
     */
    public void run() {
        try {
            System.out.println(ConsoleColors.BLUE + "Initiating Backup of Chunk: " + this.chunkId + "\n" + ConsoleColors.RESET);
            this.initiateBackup();
            System.out.println(ConsoleColors.GREEN + "Backup of Chunk done.\n" + ConsoleColors.RESET);
        }
        catch(Exception e) {
            e.printStackTrace();
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong while backing up the chunk. Aborting...\n" + ConsoleColors.RESET);
        }
    }

    /**
     * Initiates the backup of the file
     * @throws Exception: File Reader and sendPUTCHUNKmessage
     */
    private void initiateBackup() throws Exception {
        File file = new File("../../dirs/" + this.peer.getPeerId() + "/chunks/" + this.chunkId);
        FileReader reader = new FileReader(file);

        // each chunk should have a maximum size of 64K bytes
        int CHUNK_SIZE = 64000;
        char[] a = new char[CHUNK_SIZE];
        int num = chunkId.indexOf("$");
        String fileId = chunkId.substring(0, num);
        String chunkN = chunkId.substring(num + 1);

        int numRead = reader.read(a);
        sendPUTCHUNKmessage(fileId, new String(a, 0, numRead), Integer.parseInt(chunkN));

        reader.close();
    }

    /**
     * Sends PUTCHUNK message to MDB multicast data chanel
     * @param fileId -> the id generated for the file
     * @param chunk -> chunk content
     * @param chunkN -> chunk number
     */
    private void sendPUTCHUNKmessage(String fileId, String chunk, int chunkN) throws Exception {
        boolean replicationDegreeRespected;
        int waitTime = 1000; // time that will wait for the stored messages

        byte[] message = this.buildPUTCHUNKmessage(fileId, chunk, chunkN);
        DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(this.peer.getMdbAddress()), this.peer.getMdbPort());

        System.out.println("\tChunk: " + chunkN);

        do {
            if(waitTime == 1000)
                System.out.println(ConsoleColors.BLUE + "-> Sending PUTCHUNK message. Waiting " + (waitTime/1000) + "s for STORED messages: " + (waitTime/1000) + "s" + ConsoleColors.RESET);
            else
                System.out.println(ConsoleColors.BLUE + "-> Resending PUTCHUNK message. Waiting " + (waitTime/1000) + "s for STORED messages: " + (waitTime/1000) + "s" + ConsoleColors.RESET);
            this.sendSocket.send(datagramPacket);

            replicationDegreeRespected = this.receivedCorrectNumberOfSTOREDmessage(fileId, chunkN, waitTime);

            waitTime *= 2; // doubles the waiting time

        } while(!replicationDegreeRespected && waitTime <= 16000); // tries sending 5 times the message

        if(!replicationDegreeRespected)
            System.out.println(ConsoleColors.YELLOW + "-> Couldn't backup the specified chunk with the desired replication degree: " + this.peer.getChunkDesiredReplicationDegree(fileId, String.valueOf(chunkN)) + ConsoleColors.RESET);
    }

    /**
     * Verifies if it has received at least the number corresponding
     * to the replication degree of stored messages by the other peers
     * @param waitTime -> the time tht will wait for stored messages
     * @return true if it has received at least that number, false otherwise
     */
    public boolean receivedCorrectNumberOfSTOREDmessage(String fileId, int chunkN, int waitTime) throws Exception {
        Thread.sleep(waitTime);

        int actualReplicationDegree = this.peer.getChunkActualReplicationDegree(fileId, String.valueOf(chunkN));
        System.out.println("-> Number of Stored Messages: " + actualReplicationDegree + "\n");

        return actualReplicationDegree >= this.peer.getChunkDesiredReplicationDegree(fileId, String.valueOf(chunkN));
    }

    /**
     * Builds the message to be sent to the other peers
     * @param fileId -> the id generated for the file
     * @param chunk -> chunk content
     * @param chunkN -> chunk number
     * @return the message to be sent in bytes
     */
    private byte[] buildPUTCHUNKmessage(String fileId, String chunk, int chunkN) {
        String message = this.peer.getProtocolVersion() + " PUTCHUNK " + this.peer.getPeerId() + " " +
                fileId + " " + chunkN + " " + this.peer.getChunkDesiredReplicationDegree(fileId, String.valueOf(chunkN)) + "\r\n\r\n" +
                chunk;

        return message.getBytes();
    }
}