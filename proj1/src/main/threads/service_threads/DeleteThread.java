package main.threads.service_threads;

import main.*;
import main.auxiliar.ConsoleColors;
import main.files.*;

import java.net.*;

public class DeleteThread extends Thread {
    private final Peer peer;
    private String pathName;
    private final FileManager fileManager;
    private final MulticastSocket sendSocket;

    public DeleteThread(Peer peer, String message) throws Exception {
        this.peer = peer;

        this.processMessage(message);

        this.fileManager = new FileManager(this.pathName);
        this.sendSocket = new MulticastSocket();
    }

    private void processMessage(String message) {
        this.pathName = message;
    }

    public void run() {
        try {
            System.out.println(ConsoleColors.CYAN + "Initiating Deletion of: " + this.pathName + "\n" + ConsoleColors.RESET);
            if(!fileManager.fileExists()) { // the specified file does not exist
                System.out.println(ConsoleColors.RED + "ERROR: The specified file: " + this.pathName + " does not exist...\n" + ConsoleColors.RESET);
                return;
            }
            this.initiateDelete();
            System.out.println(ConsoleColors.GREEN + "Deletion done.\n" + ConsoleColors.RESET);
        }
        catch (Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong while deleting the file. Aborting..." + ConsoleColors.RESET);
        }
    }

    private void initiateDelete() throws Exception {
        this.sendDELETEmessage(this.fileManager.getFileId());
    }

    private void sendDELETEmessage(String fileId) throws Exception {
        byte[] message = this.buildDELETEmessage(fileId);
        DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(this.peer.getMcAddress()), this.peer.getMcPort());

        int waitTime = 1000;
        do {
            if(waitTime == 1000)
                System.out.println(ConsoleColors.CYAN + "-> Sending DELETE message: Waiting Time: " + (waitTime/1000) + "s" + ConsoleColors.RESET);
            else
                System.out.println(ConsoleColors.CYAN + "\n-> Resending DELETE message: Waiting Time: " + (waitTime/1000) + "s" + ConsoleColors.RESET);

            this.sendSocket.send(datagramPacket);

            Thread.sleep(waitTime);
            waitTime *= 2;

        } while(waitTime <= 16000);

        this.peer.removeFileBackedUp(fileId);
    }

    private byte[] buildDELETEmessage(String fileId) {
        String message = this.peer.getProtocolVersion() + " DELETE " + this.peer.getPeerId() + " " +
                fileId + " " + "\r\n\r\n";
        return message.getBytes();
    }
}
