package main.files;

import java.util.*;

public class FileStorage {
    HashMap<String, ArrayList<Integer>> history;

    public FileStorage() {
        this.history = new HashMap<>();
    }
    
    public void addChunk(String chunk, Integer id) {
        if (!checkingChunk(chunk)) {
            ArrayList<Integer> a = new ArrayList<>();
            a.add(id);
            history.put(chunk, a);
        }
        else if (!history.get(chunk).contains(id)) {
            ArrayList<Integer> a = history.get(chunk);
            a.add(id);
            history.put(chunk, a);
        }
    }

    public boolean checkingChunk(String chunk) {
        return history.containsKey(chunk);
    }

    public boolean peerSavedChunk(String chunk, String peer) {
        if(!history.containsKey(chunk)) return false;
        else return history.get(chunk).contains(Integer.parseInt(peer));
    }

    public Set<String> getChunks() {
        return this.history.keySet();
    }

    public ArrayList<Integer> getPeers(String chunk) {
        return this.history.get(chunk);
    }

    public int getActualReplicationDegree(String chunk) {
        if (!history.containsKey(chunk)) {
            return 0;
        }
        else {
            return history.get(chunk).size();
        }
    }

    public boolean checkEmptyHistory() {
        return history.isEmpty();
    }

    public void removeChunk(String chunk) {
        this.history.remove(chunk);
    }

    public void removePeerIdFromChunk(String chunk, int peerId) {
        if (history.containsKey(chunk)) {
            ArrayList<Integer> new_array = this.removeElem(history.get(chunk), peerId);
            if(new_array.size() != 0)
                this.history.put(chunk, new_array);
            else
                this.history.remove(chunk);
        }
    }

    private ArrayList<Integer> removeElem(ArrayList<Integer> a, int elem) {
        ArrayList<Integer> ret = new ArrayList<>();

        for (Integer integer : a) if (integer != elem) ret.add(integer);

        return ret;
    }
}
