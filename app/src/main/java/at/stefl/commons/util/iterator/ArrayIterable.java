package at.stefl.commons.util.iterator;

import java.util.Iterator;

public class ArrayIterable<T> implements Iterable<T> {
    
    private final Object array;
    
    public ArrayIterable(Object array) {
        this.array = array;
    }
    
    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator<T>(array);
    }
    
}