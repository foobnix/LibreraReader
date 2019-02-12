package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

// TODO: improve
public class XOROutputStream extends BytewiseFilterOutputStream {
    
    private byte[] key;
    private int index;
    
    public XOROutputStream(OutputStream out, byte[] key) {
        super(out);
        
        this.key = key;
    }
    
    public byte[] getKey() {
        return key;
    }
    
    public void setKey(byte[] key) {
        this.key = key;
    }
    
    @Override
    public void write(int b) throws IOException {
        out.write(b ^ key[index]);
        
        index = (index + 1) % key.length;
    }
    
}