package at.stefl.commons.io;

import java.io.InputStream;

public abstract class FilterInputStream extends DelegationInputStream {
    
    public FilterInputStream(InputStream in) {
        super(in);
    }
    
}