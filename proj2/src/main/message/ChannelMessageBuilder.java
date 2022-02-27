package main.message;

import main.auxiliar.ChannelUtils;

public class ChannelMessageBuilder {
    public static byte[] buildPUTCHUNKmessage(String protocolVersion, int peerId, String fileId, byte[] content, int replicationDegree, int chunkN, int chunkSize) {
        return ChannelUtils.merge_byte_array((protocolVersion + " PUTCHUNK " + peerId + " " +
                fileId + " " + chunkN + " " + replicationDegree + " " + chunkSize + "\r\n\r\n").getBytes(), content);
    }

    public static byte[] buildSTOREDmessage(String protocolVersion, int peerId, String fileId, int chunkN) {
        return (protocolVersion + " STORED " + peerId + " " + fileId + " " + chunkN + "\r\n\r\n").getBytes();
    }

    public static byte[] buildDELETEmessage(String protocolVersion, int peerId, String fileId) {
        return (protocolVersion + " DELETE " + peerId + " " + fileId + " " + "\r\n\r\n").getBytes();
    }

    public static byte[] buildREMOVEDmessage(String protocolVersion, int peerId, String fileId, int chunkN) {
        return  (protocolVersion + " REMOVED " + peerId + " " + fileId + " " +
                chunkN + " " + "\r\n\r\n").getBytes();
    }

    public static byte[] buildGETCHUNKmessage(String protocolVersion, int peerId, String fileId, int chunkN) {
        return (protocolVersion + " GETCHUNK " + peerId + " " + fileId + " " + chunkN + "\r\n\r\n").getBytes();
    }

    public static byte[] buildCHUNKmessage(String protocolVersion, int peerId, String fileId, int chunkN, byte[] content, int chunkSize) {
        return ChannelUtils.merge_byte_array((protocolVersion + " CHUNK " +
                peerId + " " + fileId + " " + chunkN + " " + chunkSize + "\r\n\r\n").getBytes(), content);
    }

}
