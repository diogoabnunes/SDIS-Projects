package main.threads.service_threads;

import main.*;
import main.auxiliar.ConsoleColors;
import main.exceptions.*;

import java.net.*;

import java.io.File;
import java.io.FileWriter;

import java.util.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import main.files.*;

public class ReclaimThread extends Thread {
    private final int CHUNK_SIZE = 64000; // each chunk should have a maximum size of 64K bytes
    private final Peer peer;
    private String spaceDisk;
    private final MulticastSocket sendSocket;

    public ReclaimThread(Peer peer, String message) throws Exception {
        this.peer = peer;

        this.processMessage(message);

        this.sendSocket = new MulticastSocket();
    }

    private void processMessage(String message) {
        this.spaceDisk = message;
    }

    public void run() {
        try {
            System.out.println(ConsoleColors.CYAN + "Initiating Reclaiming of " + this.spaceDisk + " B" + ConsoleColors.RESET);
            this.initiateReclaim();
            System.out.println(ConsoleColors.GREEN + "\nReclaiming done.\n" + ConsoleColors.RESET);
        }
        catch (Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong while reclaiming peer space. Aborting..." + ConsoleColors.RESET);
        }
    }

    private void initiateReclaim() throws Exception {
        HashMap<String, String> chunksStored = this.peer.getChunksStored();

        this.saveNewDiskSpace();
        long usedDiskSpace = this.computeUsedDiskSpace(chunksStored);
        int space = (int)(usedDiskSpace - Long.parseLong(this.spaceDisk));
        
        if (space > 0) {
            int sizeMinChunk = space % CHUNK_SIZE;
            int numMaxChunks = space / CHUNK_SIZE;
            System.out.println("Space: " + space);
            System.out.println("Rest: " + sizeMinChunk);
            System.out.println("DivInt: " + numMaxChunks);

            for (int i = 0; i < numMaxChunks; i++) {
                String chunkId = this.selectChunkBySize(CHUNK_SIZE);
                this.deleteChunk(chunkId);
            }
            if (sizeMinChunk > 0)
                this.deleteChunk(this.selectChunkBySize(sizeMinChunk));
        }
        else System.out.println("No need to delete anything.");
    }

    private void deleteChunk(String chunkId) throws Exception {
        // separates fileId and chunkN
        int num = chunkId.indexOf("$");
        String fileId = chunkId.substring(0, num);
        String chunkNo = chunkId.substring(num + 1);

        this.deleteChunkFile(chunkId);
        this.peer.removePeerBackedUpChunk(fileId, chunkNo, this.peer.getPeerId());

        this.sendREMOVEDmessage(fileId,  chunkNo);
    }

    private void deleteChunkFile(String chunkId) throws Exception {
        String dir = "../../dirs/" + this.peer.getPeerId() + "/chunks/";
        File file = new File(dir + chunkId);
        
        if(!file.exists()) 
            throw new NoFileFoundException("Didn't found file: " + chunkId + ", in: " + dir);

        file.delete();
    }

    private void sendREMOVEDmessage(String fileId, String chunkN) throws Exception {
        System.out.println(ConsoleColors.CYAN + "-> Sending REMOVED message..." + ConsoleColors.RESET);

        byte[] message = this.buildREMOVEDmessage(fileId, chunkN);
        DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(this.peer.getMcAddress()), this.peer.getMcPort());

        this.sendSocket.send(datagramPacket);
    }

    private byte[] buildREMOVEDmessage(String fileId, String chunkN) {
        String message = this.peer.getProtocolVersion() + " REMOVED " + this.peer.getPeerId() + " " + fileId + " " +
                         chunkN + " " + "\r\n\r\n";
        return message.getBytes();
    }

    private String selectChunkBySize(int size) throws Exception {
        if (size == CHUNK_SIZE) return this.selectBiggerChunk();
        else return this.selectSmallerChunk(size);
    }

    private String selectBiggerChunk() throws Exception {
        HashMap<String, String> chunksStored = this.peer.getChunksStored();
        HashMap<String, FileStorage> aux = this.peer.getPeersBackedUpChunks();

        String chunkId = "";
        int max = -1000;

        for(String key: chunksStored.keySet()){
            long chunkSize = Files.size(Paths.get("../../dirs/" + this.peer.getPeerId() + "/chunks/" + key));
            int num = key.indexOf("$");
            String fileId = key.substring(0, num);
            String chunkNo = key.substring(num + 1);

            int diff = aux.get(fileId).getActualReplicationDegree(chunkNo) - Integer.parseInt(chunksStored.get(key));
            if (chunkSize == CHUNK_SIZE && diff >= max) {
                max = diff;
                chunkId = key;
            }
        }
        System.out.println("Bigger ChunkId: " + chunkId);
        return chunkId;
    }

    private String selectSmallerChunk(int size) throws Exception {
        HashMap<String, String> chunksStored = this.peer.getChunksStored();
        HashMap<String, FileStorage> aux = this.peer.getPeersBackedUpChunks();
        
        String chunkId = "";
        int max = -1000;
        int min = CHUNK_SIZE + 1000;

        for(String key: chunksStored.keySet()){
            long chunkSize = Files.size(Paths.get("../../dirs/" + this.peer.getPeerId() + "/chunks/" + key));
            int num = key.indexOf("$");
            String fileId = key.substring(0, num);
            String chunkNo = key.substring(num + 1);

            int diff = aux.get(fileId).getActualReplicationDegree(chunkNo) - Integer.parseInt(chunksStored.get(key));
            if (chunkSize >= size && diff >= max && chunkSize < min) {
                max = diff;
                chunkId = key;
                min = (int)chunkSize;
            }
        }
        System.out.println("Smaller ChunkId: " + chunkId);
        return chunkId;
    }

    private long computeUsedDiskSpace(HashMap<String, String> chunksStored) throws Exception {
        long counterSize = 0;

        for(String key: chunksStored.keySet()){
            long chunkSize = Files.size(Paths.get("../../dirs/" + this.peer.getPeerId() + "/chunks/" + key));
            counterSize += chunkSize;
        }

        return counterSize;
    }

    private void saveNewDiskSpace() throws Exception{
        String dir = "../../dirs/" + this.peer.getPeerId() + "/";
        File file = new File(dir + "info.txt");
        FileWriter writer = new FileWriter(file);
        writer.write(this.spaceDisk);
        writer.close();
    }
}
