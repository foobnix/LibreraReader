package at.stefl.commons.lwxml.reader;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.writer.LWXMLWriter;

public class LWXMLTeeReader extends LWXMLFilterReader {
    
    private final LWXMLWriter tee;
    
    public LWXMLTeeReader(LWXMLReader in, LWXMLWriter tee) {
        super(in);
        
        this.tee = tee;
    }
    
    @Override
    public LWXMLEvent readEvent() throws IOException {
        LWXMLEvent result = in.readEvent();
        
        if (result != LWXMLEvent.END_DOCUMENT) {
            tee.writeEvent(result);
            tee.flush();
        }
        
        return result;
    }
    
    @Override
    public String readValue() throws IOException {
        String result = in.readValue();
        
        if (result != null) {
            tee.write(result);
            tee.flush();
        }
        
        return result;
    }
    
    @Override
    public int read() throws IOException {
        int result = in.read();
        tee.write(result);
        tee.flush();
        return result;
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        int result = in.read(cbuf);
        tee.write(cbuf, 0, result);
        tee.flush();
        return result;
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int result = in.read(cbuf, off, len);
        tee.write(cbuf, off, result);
        tee.flush();
        return result;
    }
    
    // TODO: implement
    // @Override
    // public int read(CharBuffer target) throws IOException {}
    
}