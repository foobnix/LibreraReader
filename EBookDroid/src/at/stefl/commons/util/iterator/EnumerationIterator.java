package at.stefl.commons.util.iterator;

import java.util.Enumeration;

public class EnumerationIterator<E> extends AbstractIterator<E> {
    
    private final Enumeration<? extends E> enumeration;
    
    public EnumerationIterator(Enumeration<? extends E> enumeration) {
        this.enumeration = enumeration;
    }
    
    @Override
    public boolean hasNext() {
        return enumeration.hasMoreElements();
    }
    
    @Override
    public E next() {
        return enumeration.nextElement();
    }
    
}