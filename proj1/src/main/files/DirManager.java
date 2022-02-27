package main.files;

import java.io.File;
import java.io.FileWriter;
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
            Path origsPath = Paths.get(origsDir);
            Path chunksPath = Paths.get(chunksDir);
            Files.createDirectories(origsPath);
            Files.createDirectories(chunksPath);

            File file = new File(dir + "info.txt");
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write("none");
            writer.close();

            File file1 = new File(dir + "filesBackedUpInfo.txt");
            file1.createNewFile();

            File file2 = new File(dir + "chunksRepDegreeInfo.txt");
            file2.createNewFile();

            File file3 = new File(dir + "peersBackedUpChunksInfo.txt");
            file3.createNewFile();
        }
    }
}
