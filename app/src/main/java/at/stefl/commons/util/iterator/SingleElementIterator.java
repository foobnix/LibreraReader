package at.stefl.commons.util.iterator;

import java.util.NoSuchElementException;

public class SingleElementIterator<E> extends AbstractIterator<E> {
    
    private E element;
    private boolean hasNext = true;
    private boolean first = true;
    private boolean removed;
    
    public SingleElementIterator(E element) {
        this.element = element;
    }
    
    @Override
    public boolean hasNext() {
        return hasNext;
    }
    
    @Override
    public E next() {
        if (!hasNext) throw new NoSuchElementException();
        hasNext = false;
        first = false;
        return element;
    }
    
    @Override
    public void remove() {
        if (first || !hasNext || removed) throw new IllegalStateException();
        element = null;
        removed = true;
    }
    
}