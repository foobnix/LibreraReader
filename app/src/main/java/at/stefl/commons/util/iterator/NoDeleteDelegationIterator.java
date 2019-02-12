package at.stefl.commons.util.iterator;

import java.util.Iterator;

public class NoDeleteDelegationIterator<E> extends SimpleDelegationIterator<E> {
    
    public NoDeleteDelegationIterator(Iterator<? extends E> iterator) {
        super(iterator);
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
}