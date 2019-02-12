package at.stefl.commons.util.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractIterator<E> implements Iterator<E> {
    
    @Override
    public boolean hasNext() {
        return false;
    }
    
    @Override
    public E next() {
        throw new NoSuchElementException();
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
}