package junrar.unpack.ppm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * For debugging purposes only.
 *
 * @author alban
 */
public class AnalyzeHeapDump {
    
    /** Creates a new instance of AnalyzeHeapDump */
    public AnalyzeHeapDump() {
    }

    public static void main(String[] argv) {
        File cfile = new File("P:\\test\\heapdumpc");
        File jfile = new File("P:\\test\\heapdumpj");
        if (!cfile.exists()) {
            System.err.println("File not found: " + cfile.getAbsolutePath());
            return;
        }
        if (!jfile.exists()) {
            System.err.println("File not found: " + jfile.getAbsolutePath());
            return;
        }
        long clen = cfile.length();
        long jlen = jfile.length();
        if (clen != jlen) {
            System.out.println("File size mismatch");
            System.out.println("clen = " + clen);
            System.out.println("jlen = " + jlen);
        }
        // Do byte comparison
        long len = Math.min(clen, jlen);
        InputStream cin = null;
        InputStream jin = null;
        int bufferLen = 256*1024;
        try {
            cin = new BufferedInputStream(
                    new FileInputStream(cfile), bufferLen);
            jin = new BufferedInputStream(
                    new FileInputStream(jfile), bufferLen);
            boolean matching = true;
            boolean mismatchFound = false;
            long startOff = 0L;
            long off = 0L;
            while (off < len) {
                if (cin.read() != jin.read()) {
                    if (matching) {
                        startOff = off;
                        matching = false;
                        mismatchFound = true;
                    }
                }
                else { // match
                    if (!matching) {
                        printMismatch(startOff, off);
                        matching = true;
                    }
                }
                off++;
            }
            if (!matching) {
                printMismatch(startOff, off);
            }
            if (!mismatchFound) {
                System.out.println("Files are identical");
            }
            System.out.println("Done");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
				cin.close();
				jin.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }

    private static void printMismatch(long startOff, long bytesRead) {
        System.out.println("Mismatch: off=" + startOff +
                "(0x" + Long.toHexString(startOff) +
                "), len=" + (bytesRead - startOff));
    }
}
