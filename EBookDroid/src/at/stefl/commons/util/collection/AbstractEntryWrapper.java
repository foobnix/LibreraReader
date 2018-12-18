package at.stefl.commons.util.collection;

import java.util.Map.Entry;

public abstract class AbstractEntryWrapper<K1, V1, K2, V2> extends
        AbstractEntry<K2, V2> {
    
    protected final Entry<K1, V1> entry;
    
    public AbstractEntryWrapper(Entry<K1, V1> entry) {
        this.entry = entry;
    }
    
}