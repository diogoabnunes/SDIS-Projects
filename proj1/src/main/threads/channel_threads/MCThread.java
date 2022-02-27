package main.threads.channel_threads;

import main.*;
import main.auxiliar.*;
import main.threads.service_threads.*;

import java.io.File;
import java.io.FileReader;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.lang.Integer.parseInt;

public class MCThread extends Thread{
    private final Peer peer;
    private final MulticastSocket mcSocket;
    private final MulticastSocket sendSocket;
    private final Random random;

    public MCThread(Peer peer)  throws Exception {
        this.peer = peer;
        this.mcSocket = this.peer.getMcSocket();
        this.sendSocket = new MulticastSocket();
        this.random = new Random();
    }

    /**
     * Thread of MC multicast channel
     */
    public void run() {
        try {
            while (true) {
                byte[] buf = new byte[256];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);

                this.mcSocket.receive(datagramPacket);

                this.processMessage(buf);
            }
        }
        catch (Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong in MC Thread..." + ConsoleColors.RESET);
        }
    }

    /**
     * Processes the message received from the multicast socket
     * @param buf -> received message
     * @throws Exception: Process messages
     */
    private void processMessage(byte[] buf) throws Exception {
        String message = new String(buf, 0, Auxiliar.getFirstNullByteIndex(buf), StandardCharsets.UTF_8);
        String cuttedMessage = message.substring(0, message.indexOf("\r\n\r\n"));
        String[] parts = cuttedMessage.split(" ");

//        System.out.println(ConsoleColors.PURPLE + "MC THREAD (BEGIN)" + ConsoleColors.RESET);

        if("STORED".equals(parts[1])){
            System.out.println(ConsoleColors.CYAN + "-> Received STORED message: " + cuttedMessage + ConsoleColors.RESET);
            this.processSTOREDMessage(parts);
        }
        else if ("GETCHUNK".equals(parts[1])){
            System.out.println(ConsoleColors.CYAN + "-> Received GETCHUNK message: " + cuttedMessage + ConsoleColors.RESET);
            this.processGETCHUNKmessage(parts);
        }
        else if ("DELETE".equals(parts[1])){
            System.out.println(ConsoleColors.CYAN + "-> Received DELETE message: " + cuttedMessage + ConsoleColors.RESET);
            this.processDELETEmessage(parts);
        }
        else if ("REMOVED".equals(parts[1])){
            System.out.println(ConsoleColors.CYAN + "-> Received REMOVED message: " + cuttedMessage + ConsoleColors.RESET);
            this.processREMOVEDmessage(parts);
        }

//        System.out.println(ConsoleColors.PURPLE + "MC THREAD (END)" + ConsoleColors.RESET);
    }

    /**
     * Processes STORED message
     * @param parts -> array with the strings of the STORED message
     */
    private void processSTOREDMessage(String[] parts) {
        System.out.println("-> Saving STORED info...");
        this.peer.addPeerBackedUpchunk(parts[3], parts[4], parseInt(parts[2]));
    }

    /**
     * Processes GETCHUNK message
     * @param parts -> array with the strings of the STORED message
     * @throws Exception: File Reader
     */
    private void processGETCHUNKmessage(String[] parts) throws Exception {
        if (this.peer.isChunkBackedUp(parts[3], parts[4])) {
            String path = "../../dirs/" + this.peer.getPeerId() + "/chunks/" + parts[3] + "$" + parts[4];
            FileReader fileReader = new FileReader(path);

            // each chunk should have a maximum size of 64K bytes
            int CHUNK_SIZE = 64000;
            char[] chunkContent = new char[CHUNK_SIZE];

            fileReader.read(chunkContent);
            this.sendCHUNKmessage(chunkContent, parts);

            fileReader.close();
        }
    }

    private void processDELETEmessage(String[] parts) {
        List<String> chunksIds = this.peer.getChunksIdsStored();

        for(String chunkId: chunksIds) {
            if(parts[3].equals(chunkId.substring(0, chunkId.indexOf("$")))) {
                File file = new File("../../dirs/" + this.peer.getPeerId() + "/chunks/" + chunkId);
                file.delete();

                this.peer.removeChunk(parts[3], chunkId.substring(chunkId.indexOf("$") + 1));

                System.out.println("-> Delete chunk: " + chunkId);
            }
        }
        System.out.println(ConsoleColors.GREEN + "-> Deletion of file: " + parts[3] + " completed.\n" + ConsoleColors.RESET);
    }


        private void processREMOVEDmessage(String[] parts) throws Exception {
        this.peer.removePeerBackedUpChunk(parts[3], parts[4], parseInt(parts[2]));
        int desiredRepDeg = this.peer.getChunkDesiredReplicationDegree(parts[3], parts[4]);
        int actualRepDeg = this.peer.getChunkActualReplicationDegree(parts[3], parts[4]);
        if (desiredRepDeg != -1 && actualRepDeg < desiredRepDeg) {
            MDBThread mdbThread = this.peer.getMdbThread();
            mdbThread.startCapturePUTCHUNKmessages();
            
            Thread.sleep(this.random.nextInt(401));
            
            mdbThread.stopCapturePUTCHUNKmessages();
            if(!mdbThread.receivePUTCHUNKmessageForChunk(parts[3] + "$" + parts[4]))
                new ChunkBackupThread(this.peer, parts[3] + "$" + parts[4]).start();
        }
    }

    /**
     * Sends CHUNK message to MDR multicast data channel
     * @param chunkContent -> content of the chunk
     * @param parts -> array with the strings of the message header
     * @throws Exception: Sleep and send packet
     */
    private void sendCHUNKmessage(char[] chunkContent, String[] parts) throws Exception {
        Thread.sleep(this.random.nextInt(401)); // waits a random time between 0 and 400 ms

        if (this.peer.isChunkContentSavedToRestore(parts[3], parts[4])) {
            System.out.println(ConsoleColors.YELLOW + "-> Someone already sent CHUNK message..." + ConsoleColors.RESET);
            return;
        }

        System.out.println(ConsoleColors.CYAN + "-> Sending CHUNK message!" + ConsoleColors.RESET);

        byte[] message = this.buildCHUNKmessage(chunkContent, parts);
        DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(this.peer.getMdrAddress()), this.peer.getMdrPort());

        this.sendSocket.send(datagramPacket);
    }

    /**
     * Builds the CHUNK message to be sent ot MDR multicast data channel
     * @param chunkContent -> content of the chunk 
     * @param parts -> array with the strings of the message header
     * @return message to be sent in bytes
     */
    private byte[] buildCHUNKmessage(char[] chunkContent, String[] parts) {
        String message = parts[0] + " CHUNK " + this.peer.getPeerId() + " " + parts[3] + " " +
                parts[4] + " " + "\r\n\r\n" + new String(chunkContent);
        return message.getBytes();
    }
}