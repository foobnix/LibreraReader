package at.stefl.commons.util.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CycleIterator<E> extends AbstractIterator<E> {
    
    private final Iterable<? extends E> iterable;
    private Iterator<? extends E> iterator;
    
    private boolean hasNext;
    
    public CycleIterator(Iterable<? extends E> iterable) {
        this.iterable = iterable;
        
        reinit();
    }
    
    private void reinit() {
        this.iterator = iterable.iterator();
        this.hasNext = iterator.hasNext();
    }
    
    @Override
    public boolean hasNext() {
        return hasNext;
    }
    
    @Override
    public E next() {
        if (!hasNext) throw new NoSuchElementException();
        E result = iterator.next();
        if (!iterator.hasNext()) reinit();
        return result;
    }
    
    @Override
    public void remove() {
        if (!hasNext) throw new IllegalStateException();
        iterator.remove();
    }
    
}