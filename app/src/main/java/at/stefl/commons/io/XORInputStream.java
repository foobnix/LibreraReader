package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

// TODO: improve
public class XORInputStream extends BytewiseFilterInputStream {
    
    private byte[] key;
    private int index;
    
    public XORInputStream(InputStream in, byte[] key) {
        super(in);
        
        this.key = key;
    }
    
    public byte[] getKey() {
        return key;
    }
    
    public void setKey(byte[] key) {
        this.key = key;
    }
    
    @Override
    public int read() throws IOException {
        int read = in.read();
        if (read == -1) return -1;
        
        int result = read ^ key[index];
        index = (index + 1) % key.length;
        
        return result;
    }
    
}