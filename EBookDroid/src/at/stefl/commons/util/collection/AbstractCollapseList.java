package at.stefl.commons.util.collection;

import java.util.AbstractList;

public abstract class AbstractCollapseList<E> extends AbstractList<E> implements
        CollapseList<E> {
    
    @Override
    public boolean add(E e) {
        add(size(), e);
        return true;
    }
    
    @Override
    public boolean add(E e, int count) {
        add(size(), e, count);
        return true;
    }
    
    @Override
    public void add(int index, E element) {
        add(index, element, 1);
    }
    
}