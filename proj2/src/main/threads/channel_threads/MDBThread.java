package main.threads.channel_threads;

import main.*;
import main.auxiliar.*;
import main.message.ChannelMessageBuilder;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;


public class MDBThread implements Runnable {
    private final Peer peer;
    private final ServerSocket serverSocket;

    public MDBThread(Peer peer) throws Exception {
        this.peer = peer;
        this.serverSocket = this.peer.getMdbSocket();
    }

    /**
     * Thread of MDB channel
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
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong in MDB Thread..." + ConsoleColors.RESET);
        }
    }

    /**
     * Processes the message received from the multicast socket
     * @param request -> received message
     * @throws Exception: SaveChunk and sendSTOREDmessage
     */
    private byte[] processMessage(byte[] request) throws Exception {
        // Index of \r\n\r\n
        int indexCRLF = ChannelUtils.findIndexOfCRLF(request);

        String header = new String(request, 0, indexCRLF);
        System.out.println("Request Header: " + header);

        String[] splitedHeader = header.split(" ");
        if ("PUTCHUNK".equals(splitedHeader[1])) {
            // Number of chunk
            int chunkN = Integer.parseInt(splitedHeader[4]);

            // Verifies if the chunk was already stored
            if (this.peer.isChunkStored(splitedHeader[3], chunkN)) {
                System.out.println("The chunk " + chunkN + " of file " + splitedHeader[3] + " was already stored by this peer!");
                return "".getBytes();
            }

            // Verifies if it has enough space to save the chunk
            if (!this.peer.canSaveChunk(Integer.parseInt(splitedHeader[6]))) {
                System.out.println("The chunk " + chunkN + " of file " + splitedHeader[3] + " is too big!");
                return "".getBytes();
            }

            // Size of the content
            int chunkContentSize = Integer.parseInt(splitedHeader[6]);

            // Content of the chunk
            byte[] chunkContent = new byte[chunkContentSize];
            System.arraycopy(request, indexCRLF + 4, chunkContent, 0, chunkContentSize);

            // Name of the chunk
            String chunkName = "../../dirs/" + this.peer.getPeerId() + "/chunks/" + splitedHeader[3] + "$" + splitedHeader[4];

            // Write chunk
            new File(chunkName).createNewFile();
            FileUtils.writeFile(chunkName, chunkContent, 0);

            // Tells FileSystemManager that he saved a chunk
            this.peer.storeChunk(splitedHeader[3], Integer.parseInt(splitedHeader[4]), Integer.parseInt(splitedHeader[5]), Integer.parseInt(splitedHeader[6]), Integer.parseInt(splitedHeader[2]));

            // Message to be sent
            return ChannelMessageBuilder.buildSTOREDmessage(splitedHeader[0], this.peer.getChordId(), splitedHeader[3], chunkN);
        }

        return "".getBytes();
    }
}