package at.stefl.commons.util.iterator;

import java.util.Iterator;

public class SimpleDelegationIterator<E> extends DelegationIterator<E, E> {
    
    public SimpleDelegationIterator(Iterator<? extends E> iterator) {
        super(iterator);
    }
    
    @Override
    public E next() {
        return iterator.next();
    }
    
}