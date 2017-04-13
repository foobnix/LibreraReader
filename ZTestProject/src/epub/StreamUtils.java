package epub;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {

    private static final int BUFFER_SIZE = 16 * 1024;

    public static void copy(InputStream zipInputStream, OutputStream out) throws IOException {

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipInputStream.read(bytesIn)) != -1) {
            out.write(bytesIn, 0, read);
        }
    }

    public static void copy(String fromFile, String toFile) throws IOException {
        FileReader reader = new FileReader(new File(fromFile));
        FileWriter writer = new FileWriter(toFile);

        char[] bytesIn = new char[4 * 1024];
        int read = 0;
        while ((read = reader.read(bytesIn)) != -1) {
            writer.write(bytesIn, 0, read);
        }
        writer.flush();
        writer.close();
        reader.close();
    }

}
