package main.threads.service_threads;

import main.*;
import main.auxiliar.ConsoleColors;
import main.files.*;

import java.net.*;

import java.io.FileReader;

public class BackupThread extends Thread {
    private String  pathName;
    private int replicationDegree;
    private final Peer peer;
    private final FileManager fileManager;
    private final MulticastSocket sendSocket;

    public BackupThread(Peer peer, String message) throws Exception {
        this.peer = peer;

        this.processMessage(message);

        this.sendSocket = new MulticastSocket();
        this.fileManager = new FileManager(this.pathName);

    }

    private void processMessage(String message) {
        String[] parts = message.split(" ");
        this.pathName = parts[0];
        this.replicationDegree = Integer.parseInt(parts[1]);
    }

    /**
     * Verifies if it is possible to backup the specified file
     */
    public void run() {
        try {
            System.out.println(ConsoleColors.CYAN + "Initiating Backup of: " + this.pathName + "\n" + ConsoleColors.RESET);
            if(!fileManager.fileExists()) { // the specified file does not exist
                System.out.println(ConsoleColors.RED + "ERROR: The specified file: " + this.pathName + " does not exist...\n" + ConsoleColors.RESET);
                return;
            }
            this.initiateBackup();
            System.out.println(ConsoleColors.GREEN + "Backup done.\n" + ConsoleColors.RESET);
        } catch(Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong while backing up the file. Aborting...\n" + ConsoleColors.RESET);
        }
    }

    /**
     * Initiates the backup of the file
     * @throws Exception: Getting file ID.
     */
    private void initiateBackup() throws Exception {
        int size = this.fileManager.getFileSize();
        String fileId = this.fileManager.getFileId();
        FileReader reader = new FileReader(this.fileManager.getFile());

        // each chunk should have a maximum size of 64K bytes
        int CHUNK_SIZE = 64000;
        char[] a = new char[CHUNK_SIZE];
        int numRead, numChunk = 0;

        while((numRead = reader.read(a)) != -1)
            sendPUTCHUNKmessage(fileId, new String(a, 0, numRead), numChunk++);

        reader.close();

        // because the file as a size multiple of 64000 the last chunk should have a size of 0
        if(size % CHUNK_SIZE == 0)
            sendPUTCHUNKmessage(fileId, "", numChunk);

        this.peer.addFileBackedUp(fileId, this.pathName, this.replicationDegree);
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

        System.out.println("Chunk: " + chunkN);

        do {
            if (waitTime == 1000)
                System.out.println(ConsoleColors.CYAN + "-> Sending PUTCHUNK message. Waiting " + (waitTime/1000) + "s for STORED messages: " + (waitTime/1000) + "s" + ConsoleColors.RESET);
            else
                System.out.println(ConsoleColors.CYAN + "-> Resending PUTCHUNK message. Waiting " + (waitTime/1000) + "s for STORED messages: " + (waitTime/1000) + "s" + ConsoleColors.RESET);
            this.sendSocket.send(datagramPacket);

            replicationDegreeRespected = this.receivedCorrectNumberOfSTOREDmessage(fileId, chunkN, waitTime);

            waitTime *= 2; // doubles the waiting time

        } while(!replicationDegreeRespected && waitTime <= 16000); // tries sending 5 times the message

        if (!replicationDegreeRespected)
            System.out.println(ConsoleColors.YELLOW + "-> Couldn't backup the specified chunk with the desired replication degree: " + this.replicationDegree + ConsoleColors.RESET);
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

        return actualReplicationDegree >= this.replicationDegree;
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
                fileId + " " + chunkN + " " + this.replicationDegree + "\r\n\r\n" +
                chunk;
        return message.getBytes();
    }
}