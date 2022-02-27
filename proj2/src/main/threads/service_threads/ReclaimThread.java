package main.threads.service_threads;

import main.*;
import main.auxiliar.ChannelUtils;
import main.auxiliar.ConsoleColors;
import main.auxiliar.FileUtils;

import java.net.*;



import java.util.*;

import main.message.ChannelMessageBuilder;

public class ReclaimThread implements Runnable {
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
            e.printStackTrace();
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong while reclaiming peer space. Aborting..." + ConsoleColors.RESET);
        }
    }

    private void initiateReclaim() throws Exception {
        this.peer.setMaxSpace(Integer.parseInt(this.spaceDisk));

        // Reclaims all space so it needs to delete all chunks
        if (Integer.parseInt(this.spaceDisk) == 0) this.deleteAll();
        else {
            int usedSpace = this.peer.getUsedSpace();
            int space = usedSpace - Integer.parseInt(this.spaceDisk);

            System.out.println("UsedSpace: " + usedSpace);
            System.out.println("MaxSpace: " + this.spaceDisk);

            // It only needs to free some space
            if (space > 0) this.deleteSome(space);
            else System.out.println("No need to delete anything.");
        }
    }

    private void deleteAll() throws Exception {
        List<String> chunks = this.peer.getAllChunksStored();

        for (String chunkId: chunks) {
            if (!this.deleteChunk(chunkId)) return;
        }
    }

    private void deleteSome(int space) throws Exception {
        int sizeMinChunk = space % CHUNK_SIZE;
        int numMaxChunks = space / CHUNK_SIZE;

        System.out.println("Space: " + space);
        System.out.println("Rest: " + sizeMinChunk);
        System.out.println("DivInt: " + numMaxChunks);

        for (int i = 0; i < numMaxChunks; i++) {
            if (!this.deleteChunk(this.peer.selectChunkBySize(CHUNK_SIZE))) return;
        }

        if (sizeMinChunk > 0)
            this.deleteChunk(this.peer.selectChunkBySize(sizeMinChunk));
    }

    private boolean deleteChunk(String chunkId) throws Exception {
        // separates fileId and chunkN
        System.out.println("CHUNK ID: " + chunkId);
        if (chunkId != null) {
            int num = chunkId.indexOf("$");
            String fileId = chunkId.substring(0, num);
            int chunkNo = Integer.parseInt(chunkId.substring(num + 1));

            // Deletes the chunk
            FileUtils.deleteFile("../../dirs/" + this.peer.getPeerId() + "/chunks/" + chunkId);

            // Tells fileSystemManager that he deleted chunk
            int initiatorId = this.peer.removeStoredChunk(fileId, chunkNo);

            this.tellInitiator(fileId, chunkNo, initiatorId);

            return true;
        }
        else {
            System.out.println("Could not conclude RECLAIM");
            return false;
        }
    }

    private void tellInitiator(String fileId, int chunkN, int initiatorId) throws Exception {
        String lookupResponse = this.peer.lookup(initiatorId);
        String[] parts = lookupResponse.split(" ");
        String peerInfo = this.peer.getPeerInfo(parts[2], Integer.parseInt(parts[3]));

        System.out.println(ConsoleColors.CYAN + "-> Sending REMOVED message..." + ConsoleColors.RESET);

        byte[] message = ChannelMessageBuilder.buildREMOVEDmessage(this.peer.getProtocolVersion(), this.peer.getChordId(), fileId, chunkN);

        String[] split = peerInfo.split(" ");
        String address = split[1];
        int port = Integer.parseInt(split[2]);

        ChannelUtils.sendMessage(message, address, port);
    }
}
