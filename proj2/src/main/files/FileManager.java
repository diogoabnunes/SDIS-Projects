package main.files;

import java.io.File;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class FileManager {
    private final File file;
    private String fileId;

    public FileManager(String fileName) {
        this.file = new File(fileName);
        this.fileId = null;
    }

    public boolean fileExists() {
        return this.file.exists();
    }

    public File getFile() {
        return this.file;
    }

    public int getFileSize() {
        return (int) this.file.length();
    }

    public String getFileId() throws Exception {
        if(this.fileId == null) this.buildFileId();

        return this.fileId;
    }

    /**
     * Given the pathName to the file returns the fileName
     * @param pathName -> absolute or relative path
     * @return fileName
     */
    public String getFileNameFromPathName(String pathName) {
        int ind = pathName.lastIndexOf("\\");
        if (ind == -1) ind = pathName.lastIndexOf("/");
        if(ind == -1) return pathName;
        else return pathName.substring(ind + 1);
    }

    /**
     * Generates the file id with sha256
     * @throws Exception: Getting instance
     */
    private void buildFileId() throws Exception {
        String fileId = this.file.getName() + this.file.lastModified() + this.file.getAbsolutePath();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashbytes = digest.digest(fileId.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder(2 * hashbytes.length);
        for (byte hashbyte : hashbytes) {
            String hex = Integer.toHexString(0xff & hashbyte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        this.fileId = hexString.toString();
    }
}