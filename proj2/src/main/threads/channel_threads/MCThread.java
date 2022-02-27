package main.threads.channel_threads;

import main.*;
import main.auxiliar.*;
import main.message.ChannelMessageBuilder;
import main.threads.service_threads.BackupChunk;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class MCThread implements Runnable {
    private final Peer peer;
    private final ServerSocket serverSocket;

    private int CHUNK_SIZE = 64000;

    public MCThread(Peer peer) {
        this.peer = peer;
        this.serverSocket = this.peer.getMcSocket();
    }

    /**
     * Thread of MC multicast channel
     */
    public void run() {
        try {
            while (true) {
                byte[] request = new byte[64500];

                Socket socket = this.serverSocket.accept();

                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                // Receive Request and Processes it
                in.read(request);
                byte[] response = this.processMessage(request);

                // Send response
                out.write(response);

                socket.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong in MC Thread..." + ConsoleColors.RESET);
        }
    }

    /**
     * Processes the message received from the multicast socket
     * @param buf -> received message
     * @throws Exception: Process messages
     */
    private byte[] processMessage(byte[] buf) throws Exception {
        String message = new String(buf, 0, Utils.getFirstNullByteIndex(buf), StandardCharsets.UTF_8);
        String cuttedMessage = message.substring(0, message.indexOf("\r\n\r\n"));
        String[] parts = cuttedMessage.split(" ");


        if ("GETCHUNK".equals(parts[1])){
            System.out.println(ConsoleColors.CYAN + "-> Received GETCHUNK message: " + cuttedMessage + ConsoleColors.RESET);
            return this.processGETCHUNKmessage(buf);
        }
        else if ("DELETE".equals(parts[1])){
            System.out.println(ConsoleColors.CYAN + "-> Received DELETE message: " + cuttedMessage + ConsoleColors.RESET);
            return this.processDELETEmessage(buf);
        }
        else if ("REMOVED".equals(parts[1])){
            System.out.println(ConsoleColors.CYAN + "-> Received REMOVED message: " + cuttedMessage + ConsoleColors.RESET);
            this.processREMOVEDmessage(buf);
        }

        return "".getBytes();
    }


    /**
     * Processes GETCHUNK message
     * @param parts -> array with the strings of the STORED message
     * @throws Exception: File Reader
     */
    private byte[] processGETCHUNKmessage(byte[] request) throws Exception {
        int indexCRLF = ChannelUtils.findIndexOfCRLF(request);

        String header = new String(request, 0, indexCRLF);
        System.out.println("Request Header: " + header);

        String[] splitedHeader = header.split(" ");
        if ("GETCHUNK".equals(splitedHeader[1])) {
            // Number of chunk
            int chunkN = Integer.parseInt(splitedHeader[4]);
            String fileId = splitedHeader[3];

            // Verifies if the chunk was is stored
            if (!this.peer.isChunkStored(fileId, chunkN)) {
                System.out.println("The chunk " + chunkN + " of file " + fileId + " wasn't stored by this peer!");
                return "".getBytes();
            }
            
            // Path of the chunk
            String chunkPath = "../../dirs/" + this.peer.getPeerId() + "/chunks/" + fileId + "$" + chunkN;

            File file = new File(chunkPath);

            // Size of the content
            int chunkContentSize = (int) file.length();

            byte[] chunkContent = null;
            // Content of the chunk
            try{
                chunkContent = FileUtils.readFile(chunkPath,chunkContentSize,0);
            } catch(Exception e){
                System.out.println("Error reading file on MDR Thread");
                e.printStackTrace();
            }

            // Build message
            byte[] message = ChannelMessageBuilder.buildCHUNKmessage(this.peer.getProtocolVersion(), this.peer.getPeerId(), fileId, chunkN, chunkContent, chunkContentSize);

            return message; 
        }

        return "".getBytes();
    }

    private byte[] processDELETEmessage(byte[] request) {
        // Index of \r\n\r\n
        int indexCRLF = ChannelUtils.findIndexOfCRLF(request);

        String header = new String(request, 0, indexCRLF);
        System.out.println("Request Header: " + header);

        String fileId = header.split(" ")[3];

        Set<Integer> chunksStored = this.peer.getChunksStored(fileId);
        System.out.println("Chunks Stored: " + chunksStored);

        for (int chunkN: chunksStored)
            FileUtils.deleteFile("../../dirs/" + this.peer.getPeerId() + "/chunks/" + fileId + "$" + chunkN);

        this.peer.removeStoredChunks(fileId);

        System.out.println("Deletion completed!");

        return "".getBytes();
    }


    private byte[] processREMOVEDmessage(byte[] request) throws Exception {
        // Index of \r\n\r\n
        int indexCRLF = ChannelUtils.findIndexOfCRLF(request);

        String header = new String(request, 0, indexCRLF);
        System.out.println("Request Header: " + header);

        String split[] = header.split(" ");
        int peerId = Integer.parseInt(split[2]);
        String fileId = split[3];
        int chunkN = Integer.parseInt(split[4]);

        // Tells file system manager that the peer has no longer the backup of the chunk
        this.peer.removePeerBackedupChunk(peerId, fileId, chunkN);

        // gets file name and file replication degree
        String fileName = this.peer.getFileName(fileId);
        int repDegree = this.peer.getFileRepDegree(fileId);
        if (fileName == null) {
            System.out.println("Couldn't get the file name from that fileId, so the backup was not initiated");
            return "".getBytes();
        }
        if (repDegree == -1) {
            System.out.println("Couldn't get the file repDegree from that fileId, so the backup was not initiated");
            return "".getBytes();
        }

        int fileSize = FileUtils.sizeOfFile(fileName);
        int numChunks = fileSize / CHUNK_SIZE;
        int chunkSize;

        // last chunk
        if (numChunks == chunkN) chunkSize = fileSize % CHUNK_SIZE;
        else chunkSize = CHUNK_SIZE;

        // gets chunk content and initiates backup
        byte[] chunkContent = FileUtils.readFile(fileName, chunkSize, chunkN * CHUNK_SIZE);
        new BackupChunk(this.peer, fileId, chunkContent, chunkN, repDegree).start();

        return "".getBytes();
    }

}