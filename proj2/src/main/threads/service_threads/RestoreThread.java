package main.threads.service_threads;

import main.*;
import main.auxiliar.ChannelUtils;
import main.auxiliar.ConsoleColors;
import main.auxiliar.FileUtils;
import main.files.*;
import main.message.ChannelMessageBuilder;

import java.net.MulticastSocket;
import java.util.Set;
import java.io.File;
import java.io.FileWriter;

public class RestoreThread implements Runnable {
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

        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);

        System.out.println("-> Restore will be made into: " + filePathToRestore);

        do {
            System.out.println("Chunk: " + chunkN);

            byte[] body=this.sendGETCHUNKmessage(fileId, chunkN);
            
            if(body==null) 
                length = -1;
            else{
                // appends onto file
                FileUtils.writeFile(filePathToRestore, body,chunkN*64000);
                length = body.length;
            }

    

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
     * @throws Exception
     */
    private byte[] sendGETCHUNKmessage(String fileId, int chunkN) throws Exception {
        System.out.println(ConsoleColors.CYAN + "-> Sending GETCHUNK message." + ConsoleColors.RESET);

        if(!this.peer.isBackedUp(fileId)){
            System.out.println(ConsoleColors.RED + "ERROR: This file hasn't been backed up yet\n" + ConsoleColors.RESET);
            return null;
        }

        byte[] message =  ChannelMessageBuilder.buildGETCHUNKmessage(this.peer.getProtocolVersion(),this.peer.getPeerId(),fileId, chunkN);

        Set<Integer> peers = this.peer.getPeersBackedupChunk(fileId,chunkN);

        for (Integer peer: peers) {
            String lookupResponse = this.peer.lookup(peer);
            String[] parts = lookupResponse.split(" ");

            String peerInfo = this.peer.getPeerInfo(parts[2], Integer.parseInt(parts[3]));
            String[] info = peerInfo.split(" ");

            String address = info[1];
            int port = Integer.parseInt(info[2]);

            byte[] restoreResponseBytes = ChannelUtils.sendMessage(message, address, port, 65000);
            
            int crlfIndex = ChannelUtils.findIndexOfCRLF(restoreResponseBytes);
            String header = new String(restoreResponseBytes, 0, crlfIndex);
            String[] headerArgs = header.split(" ");

            // See if received CHUNK message if not go to next peer
            if(!headerArgs[1].equals("CHUNK")){
                System.out.println("-> Didn't received CHUNK message. Trying with another peer...");
                continue;
            }

            System.out.println(ConsoleColors.CYAN + "-> Received CHUNK message. Writing into the file...\n" + ConsoleColors.RESET);

            for(String s : headerArgs)
              System.out.println(s);

            int chunkSize = Integer.parseInt(headerArgs[5]);
            // 2 crlfs = 4 bytes

            byte[] body =  new byte[chunkSize];
            
            System.arraycopy(restoreResponseBytes,crlfIndex+ 4, body, 0, chunkSize);

            return body;
        }

        return null;

    }

}
