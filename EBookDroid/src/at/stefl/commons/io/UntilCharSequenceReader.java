package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;

import at.stefl.commons.util.StateMachine;
import at.stefl.commons.util.collection.CharArrayQueue;

public class UntilCharSequenceReader extends CharwiseFilterReader implements
        StateMachine {
    
    private boolean found;
    
    private CharSequence charSequence;
    private CharArrayQueue queue;
    
    public UntilCharSequenceReader(Reader in, CharSequence charSequence) {
        super(in);
        
        if (charSequence == null) throw new NullPointerException();
        
        if (charSequence.length() > 0) {
            this.charSequence = charSequence;
            this.queue = new CharArrayQueue(charSequence.length());
        } else {
            this.found = true;
        }
    }
    
    @Override
    public int read() throws IOException {
        if (found) return -1;
        if (!queue.isEmpty()) return queue.poll();
        
        while (true) {
            int read = in.read();
            if (read == -1) break;
            
            if (read != charSequence.charAt(queue.size())) return read;
            
            queue.add((char) read);
            if (queue.size() >= charSequence.length()) break;
        }
        
        found = true;
        return -1;
    }
    
    @Override
    public void reset() {
        found = false;
        queue.clear();
    }
    
}