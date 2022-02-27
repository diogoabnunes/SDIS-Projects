package main.auxiliar;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class ChannelUtils {
    public static byte[] merge_byte_array(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];

        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);

        return result;
    }

    public static int findIndexOfCRLF(byte[] a) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == 0xD && a[i+1] == 0xA && a[i+2] == 0xD && a[i+3] == 0xA)
                return i;
        }
        return -1;
    }

    public static byte[] sendMessage(byte[] message, String addres, int port) throws Exception {
        byte[] response = new byte[200];

        Socket socket = new Socket(addres, port);

        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        out.write(message);
        in.read(response);

        socket.close();

        return response;
    }

    public static byte[] sendMessage(byte[] message, String address, int port, int size) throws Exception {
        byte[] response = new byte[size];

        Socket socket = new Socket(address, port);

        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        out.write(message);
        in.read(response);

        socket.close();

        return response;
    }

}
