package main.threads.service_threads;

import main.*;
import main.auxiliar.ChannelUtils;
import main.auxiliar.ConsoleColors;
import main.files.*;
import main.message.ChannelMessageBuilder;

import java.util.HashMap;
import java.util.Set;

public class DeleteThread implements Runnable {
    private final Peer peer;
    private String pathName;
    private final FileManager fileManager;

    public DeleteThread(Peer peer, String message) {
        this.peer = peer;

        this.processMessage(message);

        this.fileManager = new FileManager(this.pathName);
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
            e.printStackTrace();
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong while deleting the file. Aborting..." + ConsoleColors.RESET);
        }
    }

    private void initiateDelete() throws Exception {

        // Verifies if the file was already backedup
        if (!this.peer.isBackedUp(this.fileManager.getFileId())) {
            System.out.println("The file: " + this.pathName + " was not backedup by this peer");
            return;
        }

        Set<Integer> peersBackedUp = this.peer.getPeersBackedupFiles(this.fileManager.getFileId());
        System.out.println("Peers Backed Up: " + peersBackedUp);

        if (peersBackedUp.isEmpty()) {
            System.out.println("No peers have chunks of this file");
            return;
        }

        this.peer.removeBackup(this.fileManager.getFileId());

        // Get peer info
        HashMap<Integer, String> peerInfo = new HashMap<>();
        for (int peer: peersBackedUp) {
            String lookupResponse = this.peer.lookup(peer);
            String parts[] = lookupResponse.split(" ");
            peerInfo.put(peer, this.peer.getPeerInfo(parts[2], Integer.parseInt(parts[3])));
        }

        byte[] message = ChannelMessageBuilder.buildDELETEmessage(this.peer.getProtocolVersion(), this.peer.getChordId(), this.fileManager.getFileId());

        for (int peer: peerInfo.keySet()) {
            String[] split = peerInfo.get(peer).split(" ");
            String address = split[1];
            int port = Integer.parseInt(split[2]);

            System.out.println("Sending DELETE message to peer: " + peer);
            ChannelUtils.sendMessage(message, address, port);
        }
    }
}
