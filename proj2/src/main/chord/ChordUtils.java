package main.chord;

import main.chord.node.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChordUtils {
    public static boolean isInteger(String val) {
        try {
            Integer.parseInt(val);
        }
        catch (NumberFormatException e){
            return false;
        }
        return true;
    }

    public static int generateId(String address, int port, int nTry, int limit) {
        String aux = address + ":" + port + ":" + nTry;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashbytes = digest.digest(aux.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte hashbyte : hashbytes) {
                String hex = Integer.toString((0xff & hashbyte) + 0x100, 16).substring(1);
                hexString.append(hex);
            }

            return  (ByteBuffer.wrap(hexString.toString().getBytes(StandardCharsets.UTF_8)).getInt() % limit);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static boolean isSuccessor(int id, ChordNode node) {
        return (node.isFirst() && (node.getPredecessor().getId() <= id || id <= node.getSelfInfo().getId()))
                || (node.getSelfInfo().getId() >= id && node.getPredecessor().getId() <= id)
                || node.isUnique();
    }

    public static boolean isPredecessor(int id, ChordNode node) {
        return (node.isLast() && (node.getSuccessor().getId() >= id || id >= node.getSelfInfo().getId()))
                || (node.getSelfInfo().getId() <= id && id <= node.getSuccessor().getId())
                || node.isUnique();
    }

    public static ChordNodeInfo getSuccessorFromFINDSPOTResponse(String response) {
        String[] split_response = response.split(" ");

        int successorId = Integer.parseInt(split_response[1]);
        String successorAddress = split_response[2];
        int successorPort = Integer.parseInt(split_response[3]);

        return new ChordNodeInfo(successorId, successorAddress, successorPort);
    }

    public static ChordNodeInfo getPredecessorFromFINDSPOTResponse(String response) {
        String[] split_response = response.split(" ");

        int predecessorId = Integer.parseInt(split_response[4]);
        String predecessorAddress = split_response[5];
        int predecessorPort = Integer.parseInt(split_response[6]);

        return new ChordNodeInfo(predecessorId, predecessorAddress, predecessorPort);
    }
    public static  ChordNodeInfo getNodeFromStabilizerResponse(String request) {
        String[] splitMessage = request.split(" ");

        int id = Integer.parseInt(splitMessage[1]);
        String address = splitMessage[2];
        int port = Integer.parseInt(splitMessage[3]);

        return new ChordNodeInfo(id, address, port);
    }

    public static  ChordNodeInfo getNodeFromUpdateSuccessorOrPredecessor(String request) {
        String[] splitMessage = request.split(" ");

        int id = Integer.parseInt(splitMessage[1]);
        String address = splitMessage[2];
        int port = Integer.parseInt(splitMessage[3]);

        return new ChordNodeInfo(id, address, port);
    }
}
