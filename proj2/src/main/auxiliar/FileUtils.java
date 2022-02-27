package main.auxiliar;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Future;

public class FileUtils {
    public static byte[] readFile(String fileName, int bufferSize, int offset) throws Exception {
        AsynchronousFileChannel reader = AsynchronousFileChannel.open(Paths.get(fileName), StandardOpenOption.READ);

        ByteBuffer bufferRead = ByteBuffer.allocate(bufferSize);

        Future<Integer> operation = reader.read(bufferRead, offset);
        while (!operation.isDone());

        reader.close();

        return bufferRead.array();
    }

    public static void writeFile(String fileName, byte[] content, int offset) throws Exception {
        AsynchronousFileChannel writer = AsynchronousFileChannel.open(Paths.get(fileName), StandardOpenOption.WRITE);

        ByteBuffer buffferWriter = ByteBuffer.wrap(content);

        Future<Integer> operation1 = writer.write(buffferWriter, offset);
        while (!operation1.isDone());

        writer.close();
    }

    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists())
            file.delete();
    }

    public static int sizeOfFile(String fileName) throws Exception {
        return (int) Files.size(Paths.get(fileName));
    }
}
