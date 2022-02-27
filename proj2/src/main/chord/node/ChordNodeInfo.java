package main.chord.node;

public class ChordNodeInfo {
    /**
     * Id of node
     */
    private int id;

    /**
     * Address and Port to Communicate with this node
     */
    private String address;
    private int port;

    public ChordNodeInfo(int id, String address, int port) {
        this.id = id;

        this.address = address;
        this.port = port;
    }

    public int getId() {
        return this.id;
    }

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public String toString() {
        return this.id + " # " + this.address + ":" + this.port;
    }
}
