package at.stefl.commons.util.collection;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

// TODO: implement toString
public abstract class AbstractMultiset<E> extends AbstractSet<E> implements
        Multiset<E> {
    
    private class UniqueSet extends AbstractSet<E> {
        
        @Override
        public Iterator<E> iterator() {
            return uniqueIterator();
        }
        
        @Override
        public int size() {
            return uniqueCount();
        }
        
        @Override
        public boolean contains(Object o) {
            return AbstractMultiset.this.contains(o);
        }
        
        @Override
        public boolean remove(Object o) {
            return AbstractMultiset.this.remove(o);
        }
        
        @Override
        public void clear() {
            AbstractMultiset.this.clear();
        }
    }
    
    private UniqueSet uniqueSet;
    
    @Override
    public int hashCode() {
        int result = 0;
        
        for (E e : this) {
            if (e != null) result += e.hashCode();
        }
        
        return result;
    }
    
    // TODO: fix for collections
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        
        if (!(o instanceof Multiset)) return false;
        Multiset<?> other = (Multiset<?>) o;
        
        if (size() != other.size()) return false;
        if (uniqueCount() != other.uniqueCount()) return false;
        
        for (E e : this) {
            if (!contains(e)) return false;
            if (uniqueCount(e) != other.uniqueCount(e)) return false;
        }
        
        return true;
    }
    
    @Override
    public boolean contains(Object o, int c) {
        if (!contains(o)) return false;
        if (uniqueCount(o) < c) return false;
        return true;
    }
    
    @Override
    public boolean containsExactly(Object o, int c) {
        if (!contains(o)) return false;
        if (uniqueCount(o) != c) return false;
        return true;
    }
    
    @Override
    public boolean add(E e) {
        return add(e, 1);
    }
    
    @Override
    public boolean remove(Object o) {
        return remove(o, 1);
    }
    
    @Override
    public boolean removeAll(Object o) {
        return remove(o, uniqueCount(o));
    }
    
    @Override
    public Set<E> uniqueSet() {
        return (uniqueSet == null) ? (uniqueSet = new UniqueSet()) : uniqueSet;
    }
    
}