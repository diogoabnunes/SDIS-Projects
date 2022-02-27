package main;

import main.exceptions.*;
import main.files.*;
import main.auxiliar.*;

import java.util.*;

import main.threads.service_threads.*;
import main.threads.channel_threads.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.net.*;

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

    private MulticastSocket mcSocket;
    private MulticastSocket mdbSocket;
    private MulticastSocket mdrSocket;

    private MCThread mcThread;
    private MDBThread mdbThread;
    private MDRThread mdrThread;

    private final HashMap<String, ArrayList<String>> filesBackedUp; // fileId -> [fileName, repDegree]
    private final HashMap<String, String> chunksRepDegree; // chunkId- > repDegree
    private final HashMap<String, FileStorage> peersBackedUpChunks; // fileId -> FileStorage
    private final HashMap<String, String> chunksRestored; // chunkId -> chunkContent

    public Peer(String[] args) throws Exception {
        this.filesBackedUp = new HashMap<>();
        this.chunksRepDegree = new HashMap<>();
        this.peersBackedUpChunks = new HashMap<>();
        this.chunksRestored = new HashMap<>();

        this.processInput(args);

        new DirManager(String.valueOf(peerId)).createDir();

        this.loadInfo();

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

    public synchronized MDRThread getMdrThread() {
        return this.mdrThread;
    }

    public synchronized MDBThread getMdbThread() {
        return this.mdbThread;
    }

    public synchronized MulticastSocket getMcSocket() {
        return this.mcSocket;
    }

    public synchronized MulticastSocket getMdbSocket() {
        return this.mdbSocket;
    }

    public synchronized MulticastSocket getMdrSocket() {
        return this.mdrSocket;
    }

    public synchronized HashMap<String, ArrayList<String>> getFilesBackedUp() {
        return this.filesBackedUp;
    }

    public synchronized HashMap<String, FileStorage> getPeersBackedUpChunks() {
        return this.peersBackedUpChunks;
    }

    public synchronized HashMap<String, String> getChunksStored() {
        return this.chunksRepDegree;
    }

    public synchronized List<String> getChunksIdsStored() {
        return new ArrayList<>(this.chunksRepDegree.keySet());
    }

    // ------------------------------------------
    //              PROCESS REQUEST
    // ------------------------------------------

    @Override
    public void backup(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived Backup Request.\nMessage: " + message + "\n" + ConsoleColors.RESET);
        try {
            new BackupThread(this, message).start();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void restore(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived Restore Request. \nMessage: " + message + "\n" + ConsoleColors.RESET);
        try {
            new RestoreThread(this, message).start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    @Override
    public void delete(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived Delete Request. \nMessage: " + message + "\n" + ConsoleColors.RESET);
        try {
            new DeleteThread(this, message).start();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void reclaim(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived Reclaim Request. \nMessage: " + message + "\n" + ConsoleColors.RESET);

        try {
            new ReclaimThread(this, message).start();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void state(String message) throws RemoteException {
        System.out.println(ConsoleColors.GREEN + "\nReceived State Request. \nMessage: " + message + "\n" + ConsoleColors.RESET);

        try {
            new StateThread(this).start();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ------------------------------------------
    //              LOADERS
    // ------------------------------------------

    private void loadInfo() {
        try {
            this.loadFilesBackedUpInfo();
            this.loadChunksRepDegreeInfo();
            this.loadPeersBackedUpChunksInfo();
        }
        catch(Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: An error has occurred while loading peer info..." + ConsoleColors.RESET);
        }
    }

    private void loadFilesBackedUpInfo() throws Exception {
        String dir = "../../dirs/" + this.peerId + "/";
        File file = new File(dir + "filesBackedUpInfo.txt");
        BufferedReader reader = new BufferedReader( new FileReader(file));
    
        String line;
        while((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            String fileId = parts[0];
            String fileName = parts[1];
            String repDegree = parts[2];

            this.addFileBackedUp(fileId, fileName, parseInt(repDegree));
        }

        reader.close();
    }

    private void loadChunksRepDegreeInfo() throws Exception {
        String dir = "../../dirs/" + this.peerId + "/";
        File file = new File(dir + "chunksRepDegreeInfo.txt");
        BufferedReader reader = new BufferedReader( new FileReader(file));
    
        String line;
        while((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            String chunkId = parts[0];
            String repDegree = parts[1];

            this.addChunkStored(chunkId, repDegree);
        }

        reader.close();
    }

    private void loadPeersBackedUpChunksInfo() throws Exception {
        String dir = "../../dirs/" + this.peerId + "/";
        File file = new File(dir + "peersBackedUpChunksInfo.txt");
        BufferedReader reader = new BufferedReader( new FileReader(file));
    
        String line;
        while((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            String fileId = parts[0];
            String chunkN = parts[1];
            String peerId = parts[2];

            this.addPeerBackedUpchunk(fileId, chunkN, parseInt(peerId));
        }

        reader.close();
    }

    // ------------------------------------------
    //              SAVERS
    // ------------------------------------------

    private void saveFilesBackedUpInfo() {
        try{
            String dir = "../../dirs/" + this.peerId + "/";
            File file = new File(dir + "filesBackedUpInfo.txt");
            FileWriter writer = new FileWriter(file);

            for(String key: this.filesBackedUp.keySet()) {
                ArrayList<String> params = this.filesBackedUp.get(key);
                String info = key + " " + params.get(0) + " " + params.get(1) + "\n";
                writer.write(info);
            }

            writer.close();
        }
        catch(Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: An error has occurred while saving peer info..." + ConsoleColors.RESET);
        }
    }

    private void saveChunksRepDegreeInfo() {
        try{
            String dir = "../../dirs/" + this.peerId + "/";
            File file = new File(dir + "chunksRepDegreeInfo.txt");
            FileWriter writer = new FileWriter(file);

            for(String key: this.chunksRepDegree.keySet()) {
                String info = key + " " + this.chunksRepDegree.get(key) + "\n";
                writer.write(info);
            }

            writer.close();
        }
        catch(Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: An error has occurred while saving peer info..." + ConsoleColors.RESET);
        }
    }

    private void savePeersBackedUpChunksInfo() {
        try{
            String dir = "../../dirs/" + this.peerId + "/";
            File file = new File(dir + "peersBackedUpChunksInfo.txt");
            FileWriter writer = new FileWriter(file);

            for(String key: this.peersBackedUpChunks.keySet()) {
                Set<String> chunks = this.peersBackedUpChunks.get(key).getChunks();
                for(String chunk: chunks) {
                    ArrayList<Integer> peers = this.peersBackedUpChunks.get(key).getPeers(chunk);
                    for (Integer peer : peers) {
                        String text = key + " " + chunk + " " + peer + "\n";
                        writer.write(text);
                    }
                }
            }

            writer.close();
        }
        catch(Exception e) {
            System.out.println(ConsoleColors.RED + "ERROR: An error has occurred while saving peer info..." + ConsoleColors.RESET);
        }
    }

    // ------------------------------------------
    //              INITIATORS
    // ------------------------------------------

    private void initiateRMI() throws Exception {
        RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(this, 0);
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
        System.out.println("Initiating Threads for peer " + this.peerId + "...");
        this.mcThread.start();
        this.mdbThread.start();
        this.mdrThread.start();
        System.out.println(ConsoleColors.GREEN + "Done.\n" + ConsoleColors.RESET);
    }

    // ------------------------------------------

    public synchronized long usedDiskSpaceForChunks() throws Exception {
        long counterSize = 0;
        for(String key: this.chunksRepDegree.keySet()){
            long chunkSize = Files.size(Paths.get("../../dirs/" + this.peerId + "/chunks/" + key));
            counterSize += chunkSize;
        }

        return counterSize;
    }

    public synchronized int maxDiskSpaceAvailableForChunks() throws Exception {
        File file = new File("../../dirs/" + this.peerId + "/info.txt");
        FileReader reader = new FileReader(file);
        char[] aux = new char[50];
        int numRead = reader.read(aux);
        String capacity = new String(aux, 0, numRead);
        reader.close();
        
        if(capacity.equals("none")) return -1;
        else return parseInt(capacity);
    }

    // ------------------------------------------

    public synchronized void addFileBackedUp(String fileId, String fileName, int replicationDegree) {
        ArrayList<String> info = new ArrayList<>();
        info.add(fileName); info.add(String.valueOf(replicationDegree));
        this.filesBackedUp.put(fileId, info);

        this.saveFilesBackedUpInfo();
    }

    public synchronized void removeFileBackedUp(String fileId) {
        this.filesBackedUp.remove(fileId);
        this.saveFilesBackedUpInfo();
        this.peersBackedUpChunks.remove(fileId);
        this.savePeersBackedUpChunksInfo();
    }

    public synchronized boolean fileBackedUpByThisPeer(String fileId) {
        return this.filesBackedUp.containsKey(fileId);
    }

    public synchronized void addPeerBackedUpchunk(String fileId, String chunkN, int peerId) {
        if (this.peersBackedUpChunks.containsKey(fileId)) {
            this.peersBackedUpChunks.get(fileId).addChunk(chunkN, peerId);
        }
        else {
            FileStorage fs = new FileStorage();
            fs.addChunk(chunkN, peerId);
            this.peersBackedUpChunks.put(fileId, fs);
        }
        this.savePeersBackedUpChunksInfo();
    }

    public synchronized void removeChunk(String fileId, String chunkN) {
        this.removeChunkStored(fileId + "$" + chunkN);
        this.peersBackedUpChunks.get(fileId).removeChunk(String.valueOf(chunkN));
        if (this.peersBackedUpChunks.get(fileId).checkEmptyHistory()) {
            this.peersBackedUpChunks.remove(fileId);
        }
        this.savePeersBackedUpChunksInfo();
    }

    public synchronized void removePeerBackedUpChunk(String fileId, String chunkN, int peer) {
        if(peer == parseInt(this.peerId)) this.removeChunkStored(fileId + "$" + chunkN);

        this.peersBackedUpChunks.get(fileId).removePeerIdFromChunk(String.valueOf(chunkN), peer);

        if(this.peersBackedUpChunks.get(fileId).checkEmptyHistory()) this.peersBackedUpChunks.remove(fileId);

        this.savePeersBackedUpChunksInfo();
    }

    public synchronized boolean isChunkBackedUp(String fileId, String chunkN) {
        if(this.peersBackedUpChunks.containsKey(fileId)) return this.peersBackedUpChunks.get(fileId).peerSavedChunk(chunkN, this.peerId);
        else return false;
    }

    /**
     * Returns the actual replication degree to a specified chunk
     * @param fileId -> if of the file that the chunk belongs to
     * @param chunk -> number of the chunk
     * @return number of peers that backed up that chunk
     */
    public synchronized int getChunkActualReplicationDegree(String fileId, String chunk) {
        if (!this.peersBackedUpChunks.containsKey(fileId)) return 0;
        else return this.peersBackedUpChunks.get(fileId).getActualReplicationDegree(chunk);
    }

    public synchronized int getChunkDesiredReplicationDegree(String fileId, String chunk) {
        if (!this.chunksRepDegree.containsKey(fileId + "$" + chunk)) return -1;
        else return parseInt(this.chunksRepDegree.get(fileId + "$" + chunk));
    }

    public synchronized boolean addChunkContentToRestore(String key, String content) {
        if (!this.chunksRestored.containsKey(key)) {
            this.chunksRestored.put(key, content);
            return true;
        }
        else return false;
    }

    /**
     * Returns the content of a specified chunk!
     * @param fileId -> id of the file
     * @param chunkNo -> number of the chunk
     * @return -> content of the chunk, null if it does not exist in here
     */
    public synchronized String getChunkContent(String fileId, int chunkNo) {
        return this.chunksRestored.getOrDefault(fileId + "$" + chunkNo, null);
    }

    /**
     * Verifies if there was already an answer to referring to the same chunk
     * @param fileId -> id of the file
     * @param chunkNo -> number of the chunk
     * @return -> returns true if it has been already send
     */
    public synchronized boolean isChunkContentSavedToRestore(String fileId, String chunkNo) {
        return this.chunksRestored.containsKey(fileId + "$" + chunkNo);
    }

    public synchronized void addChunkStored(String chunk, String repDegree) {
        if(!this.chunksRepDegree.containsKey(chunk)) {
            this.chunksRepDegree.put(chunk, repDegree);
            this.saveChunksRepDegreeInfo();
        }
    }

    public synchronized void removeChunkStored(String chunk) {
        if(this.chunksRepDegree.remove(chunk) != null)
            this.saveChunksRepDegreeInfo();
    }

    // ------------------------------------------
    //              PROCESS INPUT
    // ------------------------------------------

    public void processInput(String[] args) throws Exception {
        if(args.length != 9) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>" + ConsoleColors.RESET);

        if(!Auxiliar.isInteger(args[4])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>" + ConsoleColors.RESET);
        if(!Auxiliar.isInteger(args[6])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>" + ConsoleColors.RESET);
        if(!Auxiliar.isInteger(args[1])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>" + ConsoleColors.RESET);
        if(!Auxiliar.isInteger(args[8])) throw new InvalidArgumentsException(ConsoleColors.YELLOW + "Usage: java Peer <protocol_version> <peer_id> <srv_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>" + ConsoleColors.RESET);

        this.protocolVersion = args[0];
        this.peerId = args[1];
        this.serviceAccessPoint = args[2];

        this.mcAddress = args[3];
        this.mcPort = args[4];
        this.mdbAddress = args[5];
        this.mdbPort = args[6];
        this.mdrAddress = args[7];
        this.mdrPort = args[8];

        this.mcSocket = new MulticastSocket(parseInt(this.mcPort));
        this.mcSocket.joinGroup(InetAddress.getByName(this.mcAddress));
        this.mdbSocket = new MulticastSocket(parseInt(this.mdbPort));
        this.mdbSocket.joinGroup(InetAddress.getByName(this.mdbAddress));
        this.mdrSocket = new MulticastSocket(parseInt(this.mdrPort));
        this.mdrSocket.joinGroup(InetAddress.getByName(this.mdrAddress));

        this.mdrThread = new MDRThread(this);
        this.mcThread = new MCThread(this);
        this.mdbThread = new MDBThread(this);
    }
}