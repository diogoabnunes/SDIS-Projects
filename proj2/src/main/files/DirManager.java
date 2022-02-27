package main.files;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class DirManager {
    private final String peerId;

    public DirManager(String peerId) {
        this.peerId = peerId;
    }

    /**
     * Creates the directory where we will save the original copy and possible chunks in future, if it does not exist
     * @throws Exception: Creating directories
     */
    public void createDir() throws Exception {
        String dir = "../../dirs/" + this.peerId + "/";
        Path path = Paths.get(dir);
        if(!Files.exists(path)) {
            Files.createDirectories(path);
            String origsDir = dir + "restores/";
            String chunksDir = dir + "chunks/";
            String databaseDir = dir + "database/";
            Path origsPath = Paths.get(origsDir);
            Path chunksPath = Paths.get(chunksDir);
            Path databasePath = Paths.get(databaseDir);
            Files.createDirectories(origsPath);
            Files.createDirectories(chunksPath);
            Files.createDirectories(databasePath);
        }
    }
}
