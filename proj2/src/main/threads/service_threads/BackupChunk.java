package main.threads.service_threads;

import main.Peer;
import main.auxiliar.ChannelUtils;
import main.auxiliar.Utils;
import main.message.ChannelMessageBuilder;

import java.util.*;

public class BackupChunk {
    /**
     * Peer that started backup
     */
    private Peer peer;

    /**
     * Chunk Info
     */
    private String fileId;
    private byte[] content;
    private int chunkN;
    private int repDegree;

    private Random random;

    public BackupChunk(Peer peer, String fileId, byte[] content, int chunkN, int repDegree) {
        this.peer = peer;

        this.fileId = fileId;
        this.content = content;
        this.chunkN = chunkN;
        this.repDegree = repDegree;

        this.random = new Random();
    }

    public boolean start() throws Exception {
        // Get peers available for storing the chunk
        String availPeers = this.peer.getAvailablePeers(this.content.length);
        System.out.println("Peers Available for Storing: " + (availPeers.trim().equals("") ? "none" : availPeers));

        if (availPeers == null || availPeers.trim().equals("")) return false;

        // Get peer info
        String[] peersResponse = availPeers.split(" ");
        HashMap<Integer, String> peerInfo = new HashMap<>();
        for (String peer: peersResponse) {
            String lookupResponse = this.peer.lookup(Integer.parseInt(peer));
            String[] parts = lookupResponse.split(" ");
            peerInfo.put(Integer.parseInt(peer), this.peer.getPeerInfo(parts[2], Integer.parseInt(parts[3])));
        }

        // Build message
        byte[] message = ChannelMessageBuilder.buildPUTCHUNKmessage(this.peer.getProtocolVersion(), this.peer.getChordId(), this.fileId, this.content, this.repDegree, chunkN, this.content.length);

        Set<Integer> peersBackedup = this.peer.getPeersBackedupChunk(this.fileId, this.chunkN);
        int numBacks = (peersBackedup != null) ? peersBackedup.size() : 0; // Number of peers that backedup
        List<Integer> forbidden = (peersBackedup != null) ? new ArrayList<>(peersBackedup) : new ArrayList<>(); // List of peers that were already selected for backup
        System.out.println("Curr Backs: " + numBacks);
        System.out.println("Peers: " + peersBackedup);
        while (numBacks < this.repDegree) {
            // Select peers
            List<Integer> peers = this.selectPeers(availPeers.split(" "), forbidden, numBacks);
            if (peers.isEmpty()) break;

            numBacks += this.sentToPeers(peers, peerInfo, message);

            // adds the already selected peers to not be selected next
            for(int peer: peers) forbidden.add(peer);
        }

        if (numBacks < this.repDegree)
            System.out.println("Couldn't backup with the specified replication degree!\nDesired Rep Degree: " + this.repDegree + "\nActual Rep Degree: " + numBacks);
        else
            System.out.println("Chunk backedup with the following replication degree: " + numBacks);
        System.out.print("Peers received backup of chunk: ");
        for(int peer: forbidden) System.out.print(peer + " ");
        System.out.print("\n");

        return numBacks != 0;

    }

    private int sentToPeers(List<Integer> peers, HashMap<Integer, String> peerInfo, byte[] message) throws Exception {
        // Number of peers that backedup
        int numBacks = 0;

        for (int peer: peers) {
            String[] split = peerInfo.get(peer).split(" ");
            String address = split[3];
            int port = Integer.parseInt(split[4]);

            System.out.println("Sending PUTCHUNK message to peer: " + peer);

            byte[] backupResponseBytes = ChannelUtils.sendMessage(message, address, port);
            String backupResponse = new String(backupResponseBytes, 0, Utils.getFirstNullByteIndex(backupResponseBytes));

            // Tells FilesystemManager who contains a copy of a chunk
            this.peer.addPeerBackedupChunk(peer, this.fileId, chunkN);

            System.out.println("Received: " + backupResponse);
            if (backupResponse.trim().equals("")) continue;

            if ("STORED".equals(backupResponse.split(" ")[1])) numBacks++;
        }

        return numBacks;
    }

    /**
     * Selects randomly the peers that will be used to backup
     * @param peers -> peers available to backup
     * @return list of peers that will receive chunk
     */
    public List<Integer> selectPeers(String[] peers, List<Integer> forbidden, int numBacks) {
        List<String> peers1 = new ArrayList(Arrays.asList(peers));
        List<Integer> ret = new ArrayList<>();

        do {
            int ind = this.random.nextInt(peers1.size());
            String peer = peers1.remove(ind);
            if (!forbidden.contains(Integer.parseInt(peer)))
                ret.add(Integer.parseInt(peer));
        } while((peers1.size() != 0) && ((ret.size() + numBacks) != this.repDegree));

        return ret;
    }
}
