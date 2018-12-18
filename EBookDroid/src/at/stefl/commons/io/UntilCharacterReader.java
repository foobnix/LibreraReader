package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import at.stefl.commons.util.StateMachine;
import at.stefl.commons.util.array.ArrayUtil;

// TODO: improve (boxing crap)
public class UntilCharacterReader extends CharwiseFilterReader implements
        StateMachine {
    
    private boolean found;
    
    private Set<Character> characterSet;
    
    public UntilCharacterReader(Reader in, char... characters) {
        super(in);
        
        this.characterSet = ArrayUtil.toHashSet(characters);
    }
    
    public UntilCharacterReader(Reader in, Set<Character> characterSet) {
        super(in);
        
        this.characterSet = new HashSet<Character>(characterSet);
    }
    
    @Override
    public int read() throws IOException {
        if (found) return -1;
        
        int read = in.read();
        
        if ((read == -1) || characterSet.contains((char) read)) {
            found = true;
            return -1;
        }
        
        return read;
    }
    
    @Override
    public void reset() {
        found = false;
    }
    
}