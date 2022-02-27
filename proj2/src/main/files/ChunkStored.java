package main.files;

import java.io.Serializable;
import java.util.Objects;

public class ChunkStored implements Serializable {
    private int chunkN; // Number of the chunk
    private int chunkSize; // Size of the chunk in bytes

    private int repDegree; // Desired replication degree

    private int initiatorId; // Id of the peer that initiated it's backup

    public ChunkStored(int chunkN, int chunkSize, int repDegree, int initiatorId) {
        this.chunkN = chunkN;
        this.chunkSize = chunkSize;
        this.repDegree = repDegree;
        this.initiatorId = initiatorId;
    }

    public int getChunkN() {
        return chunkN;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getInitiatorId() {
        return initiatorId;
    }

    @Override
    public String toString() {
        return "ChunkStored{" +
                "chunkN=" + chunkN +
                ", chunkSize=" + chunkSize +
                ", repDegree=" + repDegree +
                ", initiatorId=" + initiatorId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkStored that = (ChunkStored) o;
        return chunkN == that.chunkN;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkN, chunkSize, repDegree, initiatorId);
    }
}
