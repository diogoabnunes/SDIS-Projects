package main.threads.service_threads;

import main.*;
import main.auxiliar.ConsoleColors;

public class StateThread implements Runnable {
    private final Peer peer;

    public StateThread(Peer peer) {
        this.peer = peer;
    }

    public void run() {
        try {
            System.out.println(ConsoleColors.CYAN + "Initiating State..."+ ConsoleColors.RESET);

            this.initiateState();
            System.out.println(ConsoleColors.GREEN + "State done.\n" + ConsoleColors.RESET);
        }
        catch(Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong while presenting the state of the peer. Aborting..." + ConsoleColors.RESET);
        }
    }

    private void initiateState() throws Exception {
        System.out.println("Unavailable Functionality (state)");
        this.printInfoFilesBackedUp();
        this.printInfoChunkStored();
        this.printPeerStorageCapacity();
    }

    private void printInfoFilesBackedUp() {
        System.out.println("\nFiles backed up:");

        /*HashMap<String, ArrayList<String>> aux = this.peer.getFilesBackedUp();
        HashMap<String, FileStorage> aux1 = this.peer.getPeersBackedUpChunks();

        if (aux.isEmpty()) {
            System.out.println("\tNone");
            return;
        }
        for(String key: aux.keySet()){
            System.out.println("\tFile Name: " + aux.get(key).get(0));
            System.out.println("\tFile Id: " + key);
            System.out.println("\tDesired Replication Degree: " + aux.get(key).get(1));

            Set<String> chunks = aux1.get(key).getChunks();
            for(String key1: chunks){
                System.out.println("\t\tChunk Id: " + key1);
                System.out.println("\t\t->Perceived Replication Degree: " + aux1.get(key).getActualReplicationDegree(key1));
            }
        }*/
    }

    private void printInfoChunkStored() throws Exception {
        System.out.println("\nChunks stored:");

        /*HashMap<String, FileStorage> aux = this.peer.getPeersBackedUpChunks();
        HashMap<String, String> chunksStored = this.peer.getChunksStored();

        if (chunksStored.isEmpty()) {
            System.out.println("\tNone");
            return;
        }
        for(String key: chunksStored.keySet()){
            long chunkSize = Files.size(Paths.get("../../dirs/" + this.peer.getPeerId() + "/chunks/" + key));

            int num = key.indexOf("$");
            String fileId = key.substring(0, num);
            String chunkNo = key.substring(num + 1);
            
            System.out.println("\tChunk Id: " + key);
            System.out.println("\tChunk Size: " + chunkSize + " B");
            System.out.println("\tDesired Replication Degree: " + chunksStored.get(key));
            System.out.println("\tPerceived Replication Degree: " + aux.get(fileId).getActualReplicationDegree(chunkNo) + "\n");
        }*/
    }

    private void printPeerStorageCapacity() throws Exception {
        /*System.out.println("\nPeer's capacity:");

        int capacity = this.peer.maxDiskSpaceAvailableForChunks();
        if(capacity == -1) System.out.println("\tMaximum Capacity: Not Specified");
        else System.out.println("\tMaximum Capacity: " + capacity + " B");
        
        System.out.println("\tCapacity used to backup chunks: " + this.peer.usedDiskSpaceForChunks() + " B\n");
    */}
}
