package main;

import main.chord.Chord;
import main.exceptions.*;
import main.files.*;
import main.auxiliar.*;

import java.util.*;

import main.threads.service_threads.*;
import main.threads.channel_threads.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;

public class Peer implements RemoteInterface{

    private String protocolVersion;
    private String peerId;
    private String serviceAccessPoint;

    private String mcAddress;
    private String mcPort;
    private String mdbAddress;
    private String mdbPort;
    private String mdrAddress;
    private String mdrPort;

    private ServerSocket mcSocket;
    private ServerSocket mdbSocket;
    private ServerSocket mdrSocket;

    private MCThread mcThread;
    private MDBThread mdbThread;

    private String chordAddress;
    private String chordPort;
    private String askChordAddress;
    private String askChordPort;

    private Chord chord;

    private FileSystemManager fileSystemManager;

    private ExecutorService executorService;

    public Peer(String[] args) throws Exception {
        this.processInput(args);
        System.out.println("Server Available at: " + this.mcSocket.getInetAddress().getHostAddress());

        new DirManager(String.valueOf(peerId)).createDir();

        this.fileSystemManager = new FileSystemManager("../../dirs/" + this.peerId);

        this.initiateThreads();
        this.initiateRMI();
    }

    public static void main(String[] args) {
        try {
            new Peer(args);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ------------------------------------------
    public synchronized String getProtocolVersion() {
        return this.protocolVersion;
    }

    public synchronized int getPeerId() {
        return parseInt(this.peerId);
    }

    public synchronized String getMdbAddress() {
        return this.mdbAddress;
    }

    public synchronized int getMdbPort() {
        return parseInt(this.mdbPort);
    }

    public synchronized String getMcAddress() {
        return this.mcAddress;
    }

    public synchronized int getMcPort() {
        return parseInt(this.mcPort);
    }

    public synchronized String getMdrAddress() {
        return this.mdrAddress;
    }

    public synchronized int getMdrPort() {
        return parseInt(this.mdrPort);
    }

    public synchronized MCThread getMcThread() {
        return this.mcThread;
    }



    public synchronized MDBThread getMdbThread() {
        return this.mdbThread;
    }

    public synchronized ServerSocket getMcSocket() {
        return this.mcSocket;
    }

    public synchronized ServerSocket getMdbSocket() {
        return this.mdbSocket;
    }

    public synchronized ServerSocket getMdrSocket() {
        return this.mdrSocket;
    }

    public synchronized boolean canSaveChunk(int size) {
        return this.fileSystemManager.canSaveChunk(size);
    }

    public synchronized String getAvailablePeers(int size) {
        return this.chord.getAvailablePeers(size);
    }

    public synchronized String lookup(int id) {
        return this.chord.lookup(id);
    }

    public String getPeerInfo(String address, int port) {
        return  this.chord.getPeerInfo(address, port);
    }

    // ------------------------------------------
    //              PROCESS REQUEST
    // ------------------------------------------

    @Override
    public void backup(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived Backup Request.\nMessage: " + message + "\n" + ConsoleColors.RESET);
        this.executorService.execute(new BackupThread(this, message));
    }

    @Override
    public void restore(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived Restore Request. \nMessage: " + message + "\n" + ConsoleColors.RESET);
        try {
            this.executorService.execute(new RestoreThread(this, message));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    @Override
    public void delete(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived Delete Request. \nMessage: " + message + "\n" + ConsoleColors.RESET);
        this.executorService.execute(new DeleteThread(this, message));
    }

    @Override
    public void reclaim(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived Reclaim Request. \nMessage: " + message + "\n" + ConsoleColors.RESET);

        try {
            this.executorService.execute(new ReclaimThread(this, message));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void state(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived State Request. \nMessage: " + message + "\n" + ConsoleColors.RESET);

        try {
            this.executorService.execute(new StateThread(this));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ------------------------------------------
    //              INITIATORS
    // ------------------------------------------

    private void initiateRMI() throws Exception {
        RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject((Remote) this, 0);
        Registry registry;
        try { // tries to create a 1099 registry
            registry = LocateRegistry.createRegistry(1099);
        }
        catch(Exception e) { // there is already one
            registry = LocateRegistry.getRegistry(1099);
        }

        registry.rebind(this.serviceAccessPoint, stub);
    }

    private void initiateThreads() {
        this.executorService = Executors.newFixedThreadPool(6);

        System.out.println("Initiating Threads for peer " + this.peerId + "...");
        new Thread(this.mcThread).start();
        new Thread(this.mdbThread).start();
        System.out.println(ConsoleColors.GREEN + "Done.\n" + ConsoleColors.RESET);
    }

    /* ------------------------------------------------------ */

    public int getChordId() {
        return this.chord.getChordNode().getSelfInfo().getId();
    }

    public synchronized boolean isBackedUp(String fileId) {
        return this.fileSystemManager.isBackedUp(fileId);
    }

    public synchronized String getFileName(String fileId) {
        return this.fileSystemManager.getFileName(fileId);
    }
    public synchronized int getFileRepDegree(String fileId) {
        return  this.fileSystemManager.getFileRepDegree(fileId);
    }

    public synchronized void initiateBackup(String fileId, String fileName, int replicationDegree) {
        this.fileSystemManager.initiateBackup(fileId, fileName, replicationDegree);
    }

    public synchronized void removeBackup(String fileId) {
        this.fileSystemManager.removeBackup(fileId);
    }

    public synchronized void addPeerBackedupChunk(int peerId, String fileId, int chunkN) {
        this.fileSystemManager.addPeerBackedupChunk(peerId, fileId, chunkN);
    }

    public synchronized void removePeerBackedupChunk(int peerId, String fileId, int chunkNumber) {
        this.fileSystemManager.removePeerBackedupChunk(peerId, fileId, chunkNumber);
    }

    public synchronized Set<Integer> getPeersBackedupChunk(String fileId, int chunkNumber) {
        return this.fileSystemManager.getPeersBackedupChunk(fileId, chunkNumber);
    }


    public synchronized Set<Integer> getPeersBackedupFiles(String fileId) {
        return this.fileSystemManager.getPeersBackedupFiles(fileId);
    }

    public synchronized boolean isChunkStored(String fileId, int chunkNo) {
        return this.fileSystemManager.isChunkStored(fileId, chunkNo);
    }

    public synchronized Set<Integer> getChunksStored(String fileId) {
        return this.fileSystemManager.getChunksStored(fileId);
    }

    public List<String> getAllChunksStored() {
        return this.fileSystemManager.getAllChunksStored();
    }

    public synchronized void storeChunk(String fileId, int chunkN, int repDegree, int chunkSize, int initiatorId) {
        this.fileSystemManager.storeChunk(fileId, chunkN, repDegree, chunkSize, initiatorId);
    }

    public synchronized void removeStoredChunks(String fileId) {
        this.fileSystemManager.removeStoredChunks(fileId);
    }

    public synchronized int removeStoredChunk(String fileId, int chunkN) {
        return this.fileSystemManager.removeStoredChunk(fileId, chunkN);
    }

    public synchronized String selectChunkBySize(int size) {
        return this.fileSystemManager.selectChunkBySize(size);
    }

    public synchronized void setMaxSpace(int maxSpace) {
        this.fileSystemManager.setMaxSpace(maxSpace);
    }

    public synchronized int getUsedSpace() {
        return this.fileSystemManager.getUsedSpace();
    }

    // ------------------------------------------
    //              PROCESS INPUT
    // ------------------------------------------

    public void processInput(String[] args) throws Exception {
        if(args.length != 13) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> <chord_addr> <chord_port> <ask_chord_addr> <ask_chord_port>" + ConsoleColors.RESET);

        if(!Utils.isInteger(args[4])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> <chord_addr> <chord_port> <ask_chord_addr> <ask_chord_port>" + ConsoleColors.RESET);
        if(!Utils.isInteger(args[6])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> <chord_addr> <chord_port> <ask_chord_addr> <ask_chord_port>" + ConsoleColors.RESET);
        if(!Utils.isInteger(args[1])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> <chord_addr> <chord_port> <ask_chord_addr> <ask_chord_port>" + ConsoleColors.RESET);
        if(!Utils.isInteger(args[8])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> <chord_addr> <chord_port> <ask_chord_addr> <ask_chord_port>" + ConsoleColors.RESET);
        if(!Utils.isInteger(args[10])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> <chord_addr> <chord_port> <ask_chord_addr> <ask_chord_port>" + ConsoleColors.RESET);
        if(!Utils.isInteger(args[12])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> <chord_addr> <chord_port> <ask_chord_addr> <ask_chord_port>" + ConsoleColors.RESET);

        this.protocolVersion = args[0];
        this.peerId = args[1];
        this.serviceAccessPoint = args[2];

        this.mcAddress = args[3];
        this.mcPort = args[4];
        this.mdbAddress = args[5];
        this.mdbPort = args[6];
        this.mdrAddress = args[7];
        this.mdrPort = args[8];

        this.chordAddress = args[9];
        this.chordPort = args[10];
        this.askChordAddress = args[11];
        this.askChordPort = args[12];

        this.mcSocket = new ServerSocket(parseInt(this.mcPort));
        this.mdbSocket = new ServerSocket(parseInt(this.mdbPort));
        this.mdrSocket = new ServerSocket(parseInt(this.mdrPort));
        // calcular os enderecos

        this.mcThread = new MCThread(this);
        this.mdbThread = new MDBThread(this);

        this.chord = new Chord(chordAddress, parseInt(chordPort), askChordAddress, parseInt(askChordPort), this);
    }
}