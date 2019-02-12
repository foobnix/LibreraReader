package at.stefl.commons.util.collection;

import java.util.Iterator;
import java.util.Set;

// TODO: improve addAll, removeAll, ...
// TODO: implement count iterator
public interface Multiset<E> extends Set<E> {
    
    public int uniqueCount();
    
    public int uniqueCount(Object o);
    
    public boolean contains(Object o, int c);
    
    public boolean containsExactly(Object o, int c);
    
    public boolean add(E o, int c);
    
    public boolean remove(Object o, int c);
    
    public boolean removeAll(Object o);
    
    public Iterator<E> uniqueIterator();
    
    public Set<E> uniqueSet();
    
}