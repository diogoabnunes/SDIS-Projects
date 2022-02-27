package main.chord;

import main.Peer;
import main.chord.manage.*;
import main.chord.message.*;
import main.chord.node.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Chord {
    /**
     * Leads with all incoming messages to chord
     */
    private ChordMessageHandlerThread chordMessageHandlerThread;

    /**
     * Responsable for stabilizing chord
     */
    private ChordStabilizerThread chordStabilizerThread;

    /**
     * Node that contains the current Peer
     */
    private ChordNode chordNode;

    /**
     * Number of entries in finger table
     * (2^m) elem that chord can have
     */
    private int m = 5;

    private ExecutorService executorService;

    private boolean created = false;

    public Chord(String chordAddress, int chordPort, String askAddress, int askPort, Peer peer) {
        this.executorService = Executors.newFixedThreadPool(2);

        if (askAddress.equals("null") || askPort < 0) this.createFirstNode(chordAddress, chordPort, peer);
        else this.createNode(chordAddress, chordPort, askAddress, askPort, peer);
    }

    private void createFirstNode(String chordAddress, int chordPort, Peer peer) {
        int id = ChordUtils.generateId(chordAddress, chordPort, 0, (int) Math.pow(2, m));
        System.out.println("Generated Id: " + id);

        this.chordNode = new ChordNode(this.m, new ChordNodeInfo(id, chordAddress, chordPort), peer);
        this.initiateThreads();

        this.chordNode.setState(ChordNodeState.READY); // node is ready to receive requests

        this.created = true;
    }

    private void createNode(String chordAddress, int chordPort, String askAddress, int askPort, Peer peer) {
        int id = this.generateId(chordAddress, chordPort, askAddress, askPort);
        System.out.println("Generated Id: " + id);

        if (id == -1) {
            System.out.println("The chord is already fulled, it can not receive more nodes!");
            return;
        }

        // finds spot where we will insert new node
        String response = ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildFINDSPOTrequest(id), askAddress, askPort);

        ChordNodeInfo successor = ChordUtils.getSuccessorFromFINDSPOTResponse(response);
        ChordNodeInfo predecessor = ChordUtils.getPredecessorFromFINDSPOTResponse(response);
        ChordNodeInfo chordNodeInfo = new ChordNodeInfo(id, chordAddress, chordPort);

        this.chordNode = new ChordNode(this.m, chordNodeInfo, successor, predecessor, peer);
        this.initiateThreads();

        // updates successor and predecessor
        ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildUPDATE_SUCCESSORrequest(chordNodeInfo), predecessor.getAddress(), predecessor.getPort());
        ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildUPDATE_PREDECESSORrequest(chordNodeInfo), successor.getAddress(), successor.getPort());

        // changes all nodes states
        ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildUPDATESTATErequest(ChordNodeState.UPDATING, id), successor.getAddress(), successor.getPort());

        // updates finger tables
        this.chordNode.updateFingerTable();
        ChordMessageSender.sendMessageAndReturn(ChordMessageBuilder.buildUPDATEFINGERTABLErequest(id), successor.getAddress(), successor.getPort());

        this.created = true;
    }

    public int generateId(String address, int port, String askAddress, int askPort) {
        List<Integer> ids = new ArrayList<>();
        int id;
        int nTry = 0;

        do {
            // Generated all possible ids
            if (ids.size() == (int) Math.pow(2, m)) return -1;

            id = ChordUtils.generateId(address, port, nTry, (int) Math.pow(2, m));
            // this id was already tried
            if (ids.contains(id)) {
                nTry++;
                continue;
            }

            String response = ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildFREEIDrequest(id), askAddress, askPort);

            // this id is not in use in chord
            if (response.equals("TRUE")) return id;
            // there is already a node with this id
            else {
                nTry++;
                ids.add(id);
            }

        } while(true);
    }

    private void initiateThreads() {
        this.chordMessageHandlerThread = new ChordMessageHandlerThread(this);
        this.chordStabilizerThread = new ChordStabilizerThread(this);

        this.executorService.execute(this.chordMessageHandlerThread);
        this.executorService.execute(this.chordStabilizerThread);
    }

    public ChordNode getChordNode() {
        return this.chordNode;
    }

    /**
     * Finds both the sucessor and predecessor of a future node
     * @param request -> contains info about new node: id, address, port
     * @return response with info about sucessor and predecessor
     */
    public synchronized String findSpot(String request) {
        int id = Integer.parseInt(request.split(" ")[1]);

        if (ChordUtils.isSuccessor(id, this.chordNode)) return ChordMessageBuilder.buildFINDSPOTresponse(this.chordNode.getSelfInfo(), this.chordNode.getPredecessor());
        else if (ChordUtils.isPredecessor(id, this.chordNode)) return ChordMessageBuilder.buildFINDSPOTresponse(this.chordNode.getSuccessor(), this.chordNode.getSelfInfo());
        else return ChordMessageSender.sendMessageAndWait(request, this.chordNode.getSuccessor().getAddress(), this.chordNode.getSuccessor().getPort());

    }

    /**
     * Updates sucessor of this node
     * @param message
     */
    public synchronized void updateSuccessor(String message) {
        this.chordNode.setSuccessor(ChordUtils.getNodeFromUpdateSuccessorOrPredecessor(message));
    }

    /**
     * Updates predecessor of this node
     * @param message
     */
    public synchronized void updatePredecessor(String message) {
        this.chordNode.setPredecessor(ChordUtils.getNodeFromUpdateSuccessorOrPredecessor(message));
    }

    /**
     * Function invoked for stabilize node
     * @param message
     */
    public synchronized void updateFingerTable(String message) {
        int id = Integer.parseInt(message.split(" ")[1]);
        if (id != this.chordNode.getSelfInfo().getId()) {
            this.chordNode.updateFingerTable();
            ChordMessageSender.sendMessageAndReturn(ChordMessageBuilder.buildUPDATEFINGERTABLErequest(id), this.chordNode.getSuccessor().getAddress(), this.chordNode.getSuccessor().getPort());
        }
        else { // after all tables are updated it changes the state
            this.chordNode.setState(ChordNodeState.READY);
            ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildUPDATESTATErequest(ChordNodeState.READY, this.chordNode.getSelfInfo().getId()), this.chordNode.getSuccessor().getAddress(), this.chordNode.getSuccessor().getPort());
            System.out.println("ALL READY");
        }
    }

    /**
     * Updates chord state
     * @param message
     */
    public synchronized void updateState(String message) {
        String state = message.split(" ")[1];
        int id = Integer.parseInt(message.split(" ")[2]);
        if (id != this.chordNode.getSelfInfo().getId()) {
            if (state.equals("READY")) this.chordNode.setState(ChordNodeState.READY);
            else if (state.equals("UPDATING")) this.chordNode.setState(ChordNodeState.UPDATING);

            if (this.chordNode.getSuccessor().getId() != id) ChordMessageSender.sendMessageAndWait(message, this.chordNode.getSuccessor().getAddress(), this.chordNode.getSuccessor().getPort());
        }
    }

    /**
     * Executes lookup for a specific peer
     * @param message
     * @return string with the sucessor id, address and port
     */
    public synchronized String lookup(String message) {
        return this.chordNode.lookup(message);
    }

    public synchronized String lookup(int id) {
        return this.chordNode.lookup(ChordMessageBuilder.buildLOOKUPrequest(id));
    }

    /**
     * Finds the predecessor of the successor node.
     * Required for stabilization.
     * @return
     */
    public synchronized String getPredecessorOfSuccessor() {
        return ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildGETSUCCESSORrequest(), this.chordNode.getSuccessor().getAddress(), this.chordNode.getSuccessor().getPort());
    }

    /**
     * Returns the predecessor of node.
     * Required for stabilization.
     * @return
     */
    public synchronized String getPredecessor() {
        return ChordMessageBuilder.buildPREDECESSORresponse(this.chordNode.getPredecessor());
    }

    public synchronized String getAvailablePeers(String message) {
        String[] aux = message.split(" ");
        int id = Integer.parseInt(aux[1]);
        int size = Integer.parseInt(aux[2]);

        String answer = "";
        if (id != this.chordNode.getSuccessor().getId())
            answer = ChordMessageSender.sendMessageAndWait(message, this.chordNode.getSuccessor().getAddress(), this.chordNode.getSuccessor().getPort());

        if (this.chordNode.canSave(size)) answer = this.chordNode.getSelfInfo().getId() + " " + answer;

        return  answer;
    }

    public String getAvailablePeers(int size) {
        if (this.chordNode.isUnique()) return null;
        else return ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildGETAVAILABLEPEERSrequest(this.chordNode.getSelfInfo().getId(), size), this.chordNode.getSuccessor().getAddress(), this.chordNode.getSuccessor().getPort());
    }

    public synchronized String getPeerInfo() {
        return this.chordNode.getPeerInfo();
    }

    public String getPeerInfo(String address, int port) {
        return ChordMessageSender.sendMessageAndWait(ChordMessageBuilder.buildGETPEERINFOrequest(), address, port);
    }

    public synchronized String freeId(String message) {
        int id = Integer.parseInt(message.split(" ")[1]);

        boolean isOccupied = this.chordNode.getSelfInfo().getId() == id || this.chordNode.getSuccessor().getId() == id;
        boolean isFree = (this.chordNode.getSelfInfo().getId() < id && id < this.chordNode.getSuccessor().getId())
                            || (this.chordNode.isLast() && id > this.chordNode.getSelfInfo().getId());

        if (isOccupied) return ChordMessageBuilder.buildFREEIDresponse("FALSE");
        else if (isFree) return ChordMessageBuilder.buildFREEIDresponse("TRUE");
        else return ChordMessageSender.sendMessageAndWait(message, this.chordNode.getSuccessor().getAddress(), this.chordNode.getSuccessor().getPort());
    }

    public void print() {
        System.out.println(this.chordNode);
    }

    public boolean canReceiveRequests() {
        return this.chordNode.getState().equals(ChordNodeState.READY);
    }
}
