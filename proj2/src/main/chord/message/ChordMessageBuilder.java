package main.chord.message;

import main.Peer;
import main.chord.node.*;

public class ChordMessageBuilder {
    public static String buildFINDSPOTrequest(int id) {
        return "FINDSPOT_REQUEST " + id;
    }

    public static String buildFINDSPOTresponse(ChordNodeInfo successor, ChordNodeInfo predecessor) {
        return "FINDSPOT_RESPONSE " + successor.getId() + " " + successor.getAddress() + " " + successor.getPort()
                + " " + predecessor.getId() + " " + predecessor.getAddress() + " " + predecessor.getPort();
    }

    public static String buildGETSUCCESSORrequest() {
        return "GETSUCCESSOR";
    }

    public static String buildPREDECESSORresponse(ChordNodeInfo predecessor) {
        return "PREDECESSOR " + predecessor.getId() + " " + predecessor.getAddress() + " " + predecessor.getPort();
    }

    public static String buildUPDATE_SUCCESSORrequest(ChordNodeInfo node) {
        return "UPDATE_SUCCESSOR " + node.getId() + " " + node.getAddress() + " " + node.getPort();
    }

    public static String buildUPDATE_PREDECESSORrequest(ChordNodeInfo node) {
        return "UPDATE_PREDECESSOR " + node.getId() + " " + node.getAddress() + " " + node.getPort();
    }

    public static String buildUPDATEFINGERTABLErequest(int id) {
        return "UPDATE_FINGERTABLE " + id;
    }

    public static String buildLOOKUPrequest(int id) {
        return "LOOKUP_REQUEST " + id;
    }

    public static String buildLOOKUPresponse(int id, String address, int port) {
        return "LOOKUP_RESPONSE " + id + " " + address + " " + port;
    }

    public static String buildUPDATESTATErequest(ChordNodeState chordNodeState, int id) {
        return "UPDATESTATE " + chordNodeState + " " + id;
    }

    public static String buildGETAVAILABLEPEERSrequest(int id, int size) {
        return "GETAVAILABLEPEERS " + id + " " + size;
    }

    public static String buildGETPEERINFOrequest() {
        return "GETPEERINFO";
    }

    public static String buildPEERINFOresponse(Peer peer) {
        return peer.getChordId()
                + " " + peer.getMcAddress() + " " + peer.getMcPort()
                + " " + peer.getMdbAddress() + " " + peer.getMdbPort()
                + " " + peer.getMdrAddress() + " " + peer.getMdrPort();
    }

    public static String buildFREEIDrequest(int id) {
        return "FREEID " + id;
    }
    public static String buildFREEIDresponse(String message) {
        return message;
    }

}
