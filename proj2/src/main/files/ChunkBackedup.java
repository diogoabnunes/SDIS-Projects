package main.files;

import java.io.Serializable;
import java.util.*;

public class ChunkBackedup implements Serializable {
    int chunkNo;
    Set<Integer> peers; // peers that backed up this chunk
    
    public ChunkBackedup(int chunkNo){
        this.chunkNo = chunkNo;
        peers = new HashSet<>();
    }

    public void addPeer(int id){
        peers.add(id);
    }

    public void removePeer(int id){
        peers.remove(id);
    }

    public int getActualRepDegree(){
        return this.peers.size();
    }

    public Set<Integer> getPeers(){
        return this.peers;
    }

    public String toString() {
        return chunkNo + " # " + peers.toString();
    }

}
