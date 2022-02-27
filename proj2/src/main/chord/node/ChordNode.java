package main.chord.node;

import main.Peer;
import main.chord.*;
import main.chord.message.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Element of a chord ring
 */
public class ChordNode {
    /**
     * State of the nod eit can be ready to be asked or updating
     */
    private ChordNodeState chordNodeState;

    /**
     * Number of max elements in the finger table
     */
    private int m;
    private List<ChordNodeInfo> fingerTable;

    /**
     * Info of both predecessor, successor and itself
     */
    private ChordNodeInfo predecessor;
    private ChordNodeInfo successor;
    private ChordNodeInfo selfInfo;

    /**
     * Peer that it is representing
     */
    private Peer peer;

    public ChordNode(int m, ChordNodeInfo selfInfo, Peer peer) {
        this.chordNodeState = ChordNodeState.CREATING;

        this.m = m;
        this.fingerTable = new ArrayList<>();

        this.predecessor = selfInfo;
        this.successor = selfInfo;
        this.selfInfo = selfInfo;

        // Because it's the first node in the ring all elements are the id of this node
        for (int i = 0; i < this.m; i++) this.fingerTable.add(this.selfInfo);

        this.peer = peer;
    }

    public ChordNode(int m, ChordNodeInfo selfInfo, ChordNodeInfo successor, ChordNodeInfo predecessor, Peer peer) {
        this.chordNodeState = ChordNodeState.CREATING;

        this.m = m;
        this.fingerTable = new ArrayList<>();

        this.predecessor = predecessor;
        this.successor = successor;
        this.selfInfo = selfInfo;

        this.peer = peer;
    }

    public ChordNodeInfo getSuccessor() {
        return this.successor;
    }

    public ChordNodeInfo getPredecessor() {
        return this.predecessor;
    }

    public ChordNodeInfo getSelfInfo() {
        return this.selfInfo;
    }

    public ChordNodeState getState() {
        return  this.chordNodeState;
    }

    public void setSuccessor(ChordNodeInfo successor) {
        this.successor = successor;
    }

    public void setPredecessor(ChordNodeInfo predecessor) {
        this.predecessor = predecessor;
    }

    public void setState(ChordNodeState chordNodeState) {
        this.chordNodeState = chordNodeState;
    }

    public boolean isUnique() {
        return this.successor.getId() == this.selfInfo.getId();
    }

    public boolean isFirst() {
        return this.selfInfo.getId() <= this.predecessor.getId() && this.selfInfo.getId() <= this.successor.getId();
    }

    public boolean isLast() {
        return this.selfInfo.getId() >= this.predecessor.getId() && this.selfInfo.getId() >= this.successor.getId();
    }

    public boolean canSave(int size) {
        return this.peer.canSaveChunk(size);
    }

    /**
     * Updates the finger table
     */
    public void updateFingerTable() {
        this.fingerTable.clear();

        this.fingerTable.add(this.successor);
        for (int i = 1; i < this.m; i++) {
            int nodeToFindSuccessor = Math.floorMod(this.selfInfo.getId() + (int) Math.pow(2, i), (int) Math.pow(2, this.m));

            String response = ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildFINDSPOTrequest(nodeToFindSuccessor), this.successor.getAddress(), this.successor.getPort());
            this.fingerTable.add(ChordUtils.getSuccessorFromFINDSPOTResponse(response));
        }
    }

    /**
     * Tries to find some node
     * @param message -> contains the id of the node to find
     * @return -> id, address and port of the node
     */
    public String lookup(String message) {
        int id = Integer.parseInt(message.split(" ")[1]);

        if ((this.isLast() && id > this.selfInfo.getId()) || (this.isLast() && id <= this.successor.getId()) || (this.selfInfo.getId() < id && id <= this.fingerTable.get(0).getId()))
            return ChordMessageBuilder.buildLOOKUPresponse(this.successor.getId(), this.successor.getAddress(), this.successor.getPort());

        for (int i = this.fingerTable.size(); i > 0; i--) {
            if (this.selfInfo.getId() < this.fingerTable.get(i-1).getId() && this.fingerTable.get(i-1).getId() < id)
                return ChordMessageSender.sendMessageAndWait(message, this.fingerTable.get(i-1).getAddress(), this.fingerTable.get(i-1).getPort());
        }

        return ChordMessageSender.sendMessageAndWait(message, this.fingerTable.get(0).getAddress(), this.fingerTable.get(0).getPort());
    }

    public String getPeerInfo() {
        return ChordMessageBuilder.buildPEERINFOresponse(this.peer);
    }

    /**
     * Prints the node and it's finger table and sends print to his sucessor
     */
    public String toString() {
        String ret = "Predecessor: " + this.predecessor + "\nCurrent: " + this.selfInfo + " # [\n";

        for(ChordNodeInfo info : this.fingerTable) ret += "-> " + info + ", \n";
        ret += "]";

        return ret;
    }
}
