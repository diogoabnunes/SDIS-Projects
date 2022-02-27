package main.auxiliar;

public class Auxiliar {
    public static boolean isInteger(String val) {
        try {
            Integer.parseInt(val);
        }
        catch (NumberFormatException e){
            return false;
        }
        return true;
    }

    public static int getFirstNullByteIndex(byte[] buf) {
        for(int i = 0; i < buf.length; i++)
            if (buf[i] == 0) return i;

        return buf.length;
    }
}
