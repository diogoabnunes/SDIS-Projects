package main.threads.service_threads;

import main.*;
import main.auxiliar.FileUtils;
import main.auxiliar.ConsoleColors;
import main.files.*;

public class BackupThread implements Runnable {
    private String  pathName;
    private int replicationDegree;
    private final Peer peer;
    private final FileManager fileManager;

    public BackupThread(Peer peer, String message) {
        this.peer = peer;

        this.processMessage(message);

        this.fileManager = new FileManager(this.pathName);

    }

    private void processMessage(String message) {
        String[] parts = message.split(" ");
        this.pathName = parts[0];
        this.replicationDegree = Integer.parseInt(parts[1]);
    }

    /**
     * Verifies if it is possible to backup the specified file
     */
    public void run() {
        try {
            System.out.println(ConsoleColors.CYAN + "Initiating Backup of: " + this.pathName + "\n" + ConsoleColors.RESET);

            if(!fileManager.fileExists()) { // the specified file does not exist
                System.out.println(ConsoleColors.RED + "ERROR: The specified file: " + this.pathName + " does not exist...\n" + ConsoleColors.RESET);
                return;
            }

            this.initiateBackup();

            System.out.println(ConsoleColors.GREEN + "Backup done.\n" + ConsoleColors.RESET);
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println(ConsoleColors.RED + "ERROR: Something went wrong while backing up the file. Aborting...\n" + ConsoleColors.RESET);
        }
    }

    /**
     * Initiates the backup of the file
     * @throws Exception: Getting file ID.
     */
    private void initiateBackup() throws Exception {
        int size = this.fileManager.getFileSize();
        String fileId = this.fileManager.getFileId();

        // Verifies if the file was already backedup
        if (this.peer.isBackedUp(fileId)) {
            System.out.println("The file: " + this.pathName + " was already backedup by this peer");
            return;
        }

        // Tells FilesystemManager that he started a backup of a file, if some chunk was saved
        this.peer.initiateBackup(fileId, this.pathName, this.replicationDegree);

        // each chunk should have a maximum size of 64K bytes
        int CHUNK_SIZE = 64000;
        int numChunk = 0;
        int totalChunks = size / CHUNK_SIZE;
        int finalChunkSize = size % CHUNK_SIZE;
        boolean somePeerBackup = false;

        while (numChunk != totalChunks) {
            // Reads the content of a chunk
            byte[] content = FileUtils.readFile(this.pathName, CHUNK_SIZE, CHUNK_SIZE * numChunk);
            if (new BackupChunk(this.peer, fileId, content, numChunk++, this.replicationDegree).start())
                somePeerBackup = true;

            System.out.println("\n--------------------------\n");
        }

        // Reads the content of last chunk
        byte[] content = FileUtils.readFile(this.pathName, finalChunkSize, CHUNK_SIZE * numChunk);
        if (new BackupChunk(this.peer, fileId, content, numChunk, this.replicationDegree).start())
            somePeerBackup = true;

        // Tells FilesystemManager that he started a backup of a file, if some chunk was saved
        if (!somePeerBackup) {
            this.peer.removeBackup(fileId);
            System.out.println("None of the chunks of the file was saved!");
        }
    }
}