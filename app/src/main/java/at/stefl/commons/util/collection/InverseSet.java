package at.stefl.commons.util.collection;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class InverseSet<E> extends AbstractSet<E> {
    
    private final Set<? super E> inverseSet;
    
    public InverseSet() {
        this(new HashSet<E>());
    }
    
    public InverseSet(Set<? super E> reverseSet) {
        this.inverseSet = reverseSet;
    }
    
    @Override
    public int hashCode() {
        return inverseSet.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        
        if (!(o instanceof InverseSet)) return false;
        InverseSet<?> other = (InverseSet<?>) o;
        
        if (inverseSet.size() != inverseSet.size()) return false;
        return inverseSet.containsAll(other.inverseSet);
    }
    
    @Override
    public String toString() {
        return "!" + inverseSet;
    }
    
    public Set<? super E> inverseSet() {
        return inverseSet;
    }
    
    @Override
    public boolean isEmpty() {
        return false;
    }
    
    @Override
    public int size() {
        return Integer.MAX_VALUE;
    }
    
    @Override
    public boolean contains(Object o) {
        return !inverseSet.contains(o);
    }
    
    @Override
    public boolean add(E e) {
        return inverseSet.remove(e);
    }
    
    public boolean removeElement(E e) {
        return inverseSet.add(e);
    }
    
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public <T extends Object> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
    
}