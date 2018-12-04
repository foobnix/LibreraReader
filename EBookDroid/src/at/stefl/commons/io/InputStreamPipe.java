package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// TODO: improve
public class InputStreamPipe {
    
    private final InputStream in;
    private final OutputStream out;
    
    private Thread pipeThread = new Thread() {
        
        @Override
        public void run() {
            try {
                int read;
                while (((read = in.read()) != -1) && !Thread.interrupted()) {
                    byte[] bytes = new byte[1 + in.available()];
                    bytes[0] = (byte) read;
                    in.read(bytes, 1, bytes.length - 1);
                    
                    out.write(bytes);
                    out.flush();
                }
            } catch (IOException e) {
            }
        }
    };
    
    public InputStreamPipe(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.pipeThread.start();
    }
    
    public void join() throws InterruptedException {
        pipeThread.join();
    }
    
    public void close() {
        pipeThread.interrupt();
        
        try {
            join();
        } catch (InterruptedException e) {
        }
    }
    
}