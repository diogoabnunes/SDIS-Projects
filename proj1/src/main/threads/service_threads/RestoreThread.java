package main.threads.service_threads;

import main.*;
import main.auxiliar.ConsoleColors;
import main.files.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import java.io.File;
import java.io.FileWriter;

public class RestoreThread extends Thread {
    private final Peer peer;
    private String pathName;
    private final FileManager fileManager;
    private final MulticastSocket sendSocket;

    public RestoreThread(Peer peer, String message) throws Exception {
        this.peer = peer;

        this.processMessage(message);

        this.fileManager = new FileManager(this.pathName);
        this.sendSocket = new MulticastSocket();
    }

    private void processMessage(String message) {
        this.pathName = message;
    }

    /**
     * Verifies if it is possible to restore the specified file
     */
    public void run() {
        try {
            System.out.println(ConsoleColors.CYAN + "Initiating Restore of: " + this.pathName + ConsoleColors.RESET);
            if(!fileManager.fileExists()) { // the specified file does not exist
                System.out.println(ConsoleColors.RED + "ERROR: The specified file: " + this.pathName + " does not exist...\n" + ConsoleColors.RESET);
                return;
            }
            this.initiateRestore();
            System.out.println(ConsoleColors.GREEN + "Restore done.\n" + ConsoleColors.RESET);
        }
        catch (Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong while restoring the file. Aborting..." + ConsoleColors.RESET);
        }
    }

    /**
     * Initiates the restore of the file
     * @throws Exception: Getting file ID and File Writer
     */
    public void initiateRestore() throws Exception {
        String fileId = this.fileManager.getFileId();
        int chunkN = 0, length;

        String filePathToRestore = "../../dirs/" + this.peer.getPeerId() + "/restores/" + this.fileManager.getFileNameFromPathName(this.pathName);
        File file = new File(filePathToRestore);
        
        if(file.exists()) {
            System.out.println(ConsoleColors.YELLOW + "-> Restore of the specified file already done!" + ConsoleColors.RESET);
            return;
        }

        FileWriter fileWriter = new FileWriter(file);

        System.out.println("-> Restore will be made into: " + filePathToRestore);

        do {
            System.out.println("Chunk: " + chunkN);

            this.sendGETCHUNKmessage(fileId, chunkN);
            length = this.receiveCHUNKmessage(fileId, chunkN, fileWriter);
            chunkN++;
        } while (length == 64000); // it ends only when the size of the chunk is different from 64KB

        fileWriter.close();

        if (length == -1) {
            file.delete(); // deletes the restored file because didn't obtained an answer for some chunk
            System.out.println(ConsoleColors.YELLOW + "-> Couldn't restore the chunk: " + (chunkN - 1) + ". Deleting restored file..." + ConsoleColors.RESET);
        }
    }

    /**
     * Sends GETCHUNK message to MC multicast data chanel
     * @param fileId -> the id generated for the file
     * @param chunkN -> chunk number
     * @throws IOException: Send packet
     */
    private void sendGETCHUNKmessage(String fileId, int chunkN) throws IOException {
        System.out.println(ConsoleColors.CYAN + "-> Sending GETCHUNK message." + ConsoleColors.RESET);

        byte[] message = this.buildGETCHUNKmessage(fileId, chunkN);
        DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(this.peer.getMcAddress()), this.peer.getMcPort());

        this.sendSocket.send(datagramPacket);
    }

    /**
     * Verifies if some chunk message was received
     * @param fileId -> the id generated for the file
     * @param chunkN -> chunk number
     * @return the size of the read chunk or -1 if no chunk was received
     * @throws Exception: Sleep and write
     */
    public int receiveCHUNKmessage(String fileId, int chunkN, FileWriter fileWriter) throws Exception {
        int waitTime = 1000;
        do {
            System.out.println("-> Waiting Time for CHUNK message: " + (waitTime / 1000) + "s");
            Thread.sleep(waitTime);

            String received = this.peer.getChunkContent(fileId, chunkN);
            if (received != null) {
                System.out.println(ConsoleColors.CYAN + "-> Received CHUNK message. Writing into the file...\n" + ConsoleColors.RESET);
                fileWriter.write(received);
                return received.length();
            }
            else{
                System.out.println("-> Didn't received CHUNK message. Doubling Waiting Time...");
                waitTime *= 2;
            }
        } while(waitTime <= 16000);

        return -1;
    }

    /**
     * Builds the message to be sent to the other peers
      * @param fileId -> the id generated for the file
     * @param chunkN -> chunk number
     * @return the message to be sent in bytes
     */
    private byte[] buildGETCHUNKmessage(String fileId, int chunkN) {
        String message = this.peer.getProtocolVersion() + " GETCHUNK " + this.peer.getPeerId() + " " +
                fileId + " " + chunkN + "\r\n\r\n";
        return message.getBytes();
    }
}
