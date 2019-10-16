package at.stefl.commons.util.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class LimitedIterator<E> extends SimpleDelegationIterator<E> {
    
    private int limit;
    
    public LimitedIterator(Iterator<E> iterator, int limit) {
        super(iterator);
        
        if (limit < 0) throw new IllegalArgumentException("limit < 0");
        this.limit = limit;
    }
    
    @Override
    public E next() {
        if (limit == 0) throw new NoSuchElementException();
        limit--;
        return iterator.next();
    }
    
    @Override
    public boolean hasNext() {
        return (limit > 0) && iterator.hasNext();
    }
    
}