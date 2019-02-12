package at.stefl.commons.io;

import java.io.OutputStream;

public abstract class FilterOutputStream extends DelegationOutputStream {
    
    public FilterOutputStream(OutputStream out) {
        super(out);
    }
    
}