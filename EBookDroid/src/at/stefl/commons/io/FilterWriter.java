package at.stefl.commons.io;

import java.io.Writer;

public abstract class FilterWriter extends DelegationWriter {
    
    public FilterWriter(Writer out) {
        super(out);
    }
    
}