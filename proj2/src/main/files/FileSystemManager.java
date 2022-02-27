package main.files;

import main.auxiliar.FileUtils;
import main.auxiliar.Utils;

import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileSystemManager {
    String dir;

    // Structure to save fileIds of files that backup was initiated in this peer. Ex: List<String> em que as string sao o fileId
    // Structure to save the peers that backedup a specific chunk.
    // Ex: HashMap<String, List<Integer>> em que string sao os chunkIds e List<Integer> lista dos peersId

    ConcurrentHashMap<FileInfo,List<ChunkBackedup>> backedUp = new ConcurrentHashMap<>();

    // Structure to save chunkIds of chunks that this peer backedup. 
    // Ex: List<String> em que as string sao os chunkIds
    // String - FileID , List<Integer> - Numbers of the stored chunks
    ConcurrentHashMap<String,List<ChunkStored>> stored = new ConcurrentHashMap<>();

    // Maximum disk space available for peer to save chunks, if -1 the it's unlimited
    private int maxSpace;
    // Used disk space for saving chunks
    private int usedSpace;

    public FileSystemManager(String dir){
        this.dir = dir;

        this.maxSpace = -1; // unlimited
        this.usedSpace = 0; // all free

        this.loadFiles();
    }


    /**
     *  Files whose backup was initiated in this peer.
     */

    public boolean isBackedUp(String fileId) {
        FileInfo fi = new FileInfo(fileId);
        return this.backedUp.containsKey(fi);
    }

    public String getFileName(String fileId) {
        for (FileInfo fileInfo: this.backedUp.keySet()) {
            if (fileInfo.getFileID().equals(fileId)) return fileInfo.getFileName();
        }

        return null;
    }

    public int getFileRepDegree(String fileId) {
        for (FileInfo fileInfo: this.backedUp.keySet()) {
            if (fileInfo.getFileID().equals(fileId)) return fileInfo.getDesiredRepDegree();
        }

        return -1;
    }

    public void initiateBackup(String fileId, String fileName, int rdegree) {
        FileInfo fi = new FileInfo(fileId,fileName,rdegree);
        backedUp.computeIfAbsent(fi, val->new ArrayList<ChunkBackedup>());

        this.saveFiles();
    }

    public void removeBackup(String fileId) {
        FileInfo fi = new FileInfo(fileId);
        backedUp.remove(fi);

        this.saveFiles();
    }

    /**
     * Peers that backedup chunks.
     */

    public Set<Integer> getPeersBackedupChunk(String fileId, int chunkNumber) {
        FileInfo fi = new FileInfo(fileId);

        if (backedUp.containsKey(fi)) {
            for (ChunkBackedup c : backedUp.get(fi)) {
                if (c.chunkNo == chunkNumber)
                    return c.getPeers();
            }
        }

        return null;
    }

    public Set<Integer> getPeersBackedupFiles(String fileId) {
        FileInfo fi = new FileInfo(fileId);
        Set<Integer> ret = new HashSet<>();

        if (backedUp.containsKey(fi)) {
            for (ChunkBackedup c : backedUp.get(fi)) {
                ret.addAll(c.getPeers());
            }
        }

        return ret;
    }

    public void addPeerBackedupChunk(int peerId, String fileId, int chunkNumber) {
        FileInfo fi = new FileInfo(fileId);
        ChunkBackedup chunk = new ChunkBackedup(chunkNumber);

        backedUp.computeIfPresent(fi, (key,val) -> {
            for(ChunkBackedup c : val){
                if(c.chunkNo == chunkNumber){
                    c.addPeer(peerId);
                    return val;
                }
            }
            chunk.addPeer(peerId);
            val.add(chunk);
            return val;
        });

        this.saveFiles();
    }

    public void removePeerBackedupChunk(int peerId, String fileId, int chunkNumber) {
        FileInfo fi = new FileInfo(fileId);

        backedUp.computeIfPresent(fi, (key,val) -> {
            for(ChunkBackedup c : val){
                if(c.chunkNo == chunkNumber){
                    c.removePeer(peerId);
                    return val;
                }
            }

            return val;
        });

        this.saveFiles();
    }

    /**
     * Chunks backedup by this peer.
     */
  

    public boolean isChunkStored(String fileId, int chunkNo) {
        List<ChunkStored> chunks = stored.get(fileId);
        if (chunks == null) return false;

        for (ChunkStored chunk: this.stored.get(fileId)) {
            if (chunk.getChunkN() == chunkNo) return true;
        }

        return false;
    }

    public Set<Integer> getChunksStored(String fileId) {
        Set<Integer> chunks = new HashSet<>();
        if (this.stored.containsKey(fileId)) {
            for (ChunkStored chunk: this.stored.get(fileId))
                chunks.add(chunk.getChunkN());
        }

        return chunks;
    }

    public List<String> getAllChunksStored() {
        List<String> chunks = new ArrayList<>();

        for (String fileId: this.stored.keySet()) {
            for (ChunkStored chunkStored: this.stored.get(fileId))
                chunks.add(fileId + "$" + chunkStored.getChunkN());
        }

        return chunks;
    }

    public void storeChunk(String fileId, int chunkNo, int repDegree, int chunkSize, int initiatorId) {
        // it was already saved
        if (this.isChunkStored(fileId, chunkNo)) return;
        // there is no more available space
        if ((this.usedSpace + chunkSize) > this.maxSpace && this.maxSpace != -1) return;

        ChunkStored chunkStored = new ChunkStored(chunkNo, chunkSize, repDegree, initiatorId);

        List<ChunkStored> chunks = new ArrayList<>();
        if (this.stored.containsKey(fileId)) chunks = this.stored.get(fileId);
        chunks.add(chunkStored);

        // saves new chunk
        this.stored.put(fileId, chunks);

        // updates used disk space
        this.usedSpace += chunkSize;
        System.out.println("Updated Used Space: " + this.usedSpace);

        this.saveFiles();
    }

    public void removeStoredChunks(String fileId) {
        if (stored.containsKey(fileId)) {
            // updates used disk space
            List<ChunkStored> chunks = this.stored.get(fileId);
            for (ChunkStored chunk: chunks)
                this.usedSpace -= chunk.getChunkSize();

            // removes chunks of a file
            stored.remove(fileId);

            this.saveFiles();
        }
    }

    public int removeStoredChunk(String fileId, int chunkN) {
        if (this.stored.containsKey(fileId)) {
            List<ChunkStored> chunks = this.stored.get(fileId);
            if (chunks == null) return -1;
            int ind = -1;
            for (int i = 0; i < chunks.size(); i++) {
                if (chunks.get(i).getChunkN() == chunkN) {
                    ind = i;
                }
            }

            if (ind != -1) {
                ChunkStored chunkStored = chunks.remove(ind);

                this.usedSpace -= chunkStored.getChunkSize();

                if (chunks.isEmpty()) this.stored.remove(fileId);
                else this.stored.put(fileId, chunks);

                this.saveFiles();

                return chunkStored.getInitiatorId();
            }
        }

        return -1;
    }

    /**
     * Tries to select the smaller chunk that is greater or equal to that size
     * @param size
     * @return
     */
    public String selectChunkBySize(int size) {
        String chunkId = null;
        int chunkSize = -1;

        for (String fileId: this.stored.keySet()) {
            for (ChunkStored chunkStored: this.stored.get(fileId)) {
                boolean firstSelectableChunk = chunkId == null && chunkStored.getChunkSize() >= size;
                boolean selectableSmallerChunk = chunkStored.getChunkSize() >= size && chunkStored.getChunkSize() < chunkSize;

                if (chunkId == null || firstSelectableChunk || selectableSmallerChunk) {
                    chunkId = fileId + "$" + chunkStored.getChunkN();
                    chunkSize = chunkStored.getChunkSize();
                }
            }
        }

        return chunkId;
    }

    /* --------------------------------------------- */
    /* ---------------- SPACE ---------------------- */
    /* --------------------------------------------- */

    public void setMaxSpace(int maxSpace) {
        this.maxSpace = maxSpace;
        this.saveFiles();
    }

    public int getUsedSpace() {
        return this.usedSpace;
    }

    private void computeUsedSpace() {
        this.usedSpace = 0;
        for (String fileId: this.stored.keySet()) {
            for (ChunkStored chunkStored: this.stored.get(fileId))
                this.usedSpace += chunkStored.getChunkSize();
        }
    }

    /* --------------------------------------------- */
    /* ---------------- AUXILIAR ------------------- */
    /* --------------------------------------------- */

    private void saveFiles(){
        try {
            if(!backedUp.isEmpty()){
                File backedUpFile= new File(this.dir+"/database/"+"backedUp.ser");
                if(!backedUpFile.exists())
                    backedUpFile.createNewFile();
                FileOutputStream backedUp_fo = new FileOutputStream(backedUpFile);
                ObjectOutputStream backedUp_oo = new ObjectOutputStream(backedUp_fo);
                backedUp_oo.writeObject(this.backedUp);
                backedUp_oo.close(); backedUp_fo.close();
            }
            else FileUtils.deleteFile(this.dir+"/database/"+"backedUp.ser");

            if(!stored.isEmpty()){
                File storedFile= new File(this.dir+"/database/"+"stored.ser");
                if(!storedFile.exists())
                    storedFile.createNewFile();
                FileOutputStream stored_fo = new FileOutputStream(storedFile);
                ObjectOutputStream stored_oo = new ObjectOutputStream(stored_fo);
                stored_oo.writeObject(this.stored);
                stored_oo.close(); stored_fo.close();
            }
            else FileUtils.deleteFile(this.dir+"/database/"+"stored.ser");

            //save maxsize
            String path = this.dir + "/database/" + "maxspace.txt";
            FileUtils.deleteFile(path);

            if(this.maxSpace != -1){
                File maxSpaceFile = new File(path);

                maxSpaceFile.createNewFile();

                byte[] space=String.valueOf(maxSpace).getBytes();
                try {
                    FileUtils.writeFile(this.dir+"/database/"+"maxspace.txt",space,0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("FileSystemManager - Saving - File not found");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("FileSystemManager - Saving - Error initializing stream");
        }

    }

    private void loadFiles(){
        try{
            File backedUpFile =new File(this.dir+"/database/"+"backedUp.ser");
            if(backedUpFile.exists()){
                FileInputStream backedUp_fi = new FileInputStream(backedUpFile);

                ObjectInputStream backedUp_oi = new ObjectInputStream(backedUp_fi);
                this.backedUp = (ConcurrentHashMap<FileInfo, List<ChunkBackedup>>) backedUp_oi.readObject();
                backedUp_oi.close(); backedUp_fi.close();
            }

            File storedFile =new File(this.dir+"/database/"+"stored.ser");
            if(storedFile.exists()){
                FileInputStream stored_fi = new FileInputStream(storedFile);
                ObjectInputStream stored_oi = new ObjectInputStream(stored_fi);
                this.stored = (ConcurrentHashMap<String, List<ChunkStored>>) stored_oi.readObject();
                stored_oi.close(); stored_fi.close();
            }


            File maxSpaceFile =new File(this.dir+"/database/"+"maxspace.txt");
            if(maxSpaceFile.exists()){
                try {
                    byte[] s = FileUtils.readFile(maxSpaceFile.getPath(),1000,0);
                    int index= Utils.getFirstNullByteIndex(s);
                    String str=new String(s,0,index);
                    this.maxSpace=Integer.parseInt(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("FileSystemManager - Loading -File not found");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("FileSystemManager - Loading -Error initializing stream");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Calculates used disk space at the moment
        this.computeUsedSpace();

        System.out.println(" ##################### ");

        for (FileInfo file: this.backedUp.keySet()) {
            System.out.println(file);
            for (ChunkBackedup chunk: this.backedUp.get(file)) {
                System.out.print(chunk);
                System.out.print("\n");
            }
            System.out.println("-----");
        }

        System.out.println(" ------------------ ");

        for (String stored: this.stored.keySet()) {
            System.out.print(stored + " -> ");
            for (ChunkStored chunkStored: this.stored.get(stored))
                System.out.println(chunkStored);
        }

        System.out.println(" ------------------ ");
        System.out.println("MaxSpace: "+ this.maxSpace);
        System.out.println(" ##################### ");
    }

    /**
     * ------------
     */

    public boolean canSaveChunk(int size) {
        return this.maxSpace == -1 || (this.usedSpace + size <= this.maxSpace && this.maxSpace != 0);
    }

}
