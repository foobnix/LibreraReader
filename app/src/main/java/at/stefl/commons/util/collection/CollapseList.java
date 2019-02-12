package at.stefl.commons.util.collection;

import java.util.List;

public interface CollapseList<E> extends List<E> {
    
    public boolean add(E e, int count);
    
    public void add(int index, E element, int count);
    
}