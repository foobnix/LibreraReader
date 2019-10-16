package at.stefl.commons.util.collection;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import at.stefl.commons.util.primitive.IntegerReference;

// TODO: handle size > Integer.MAX_VALUE
public abstract class MultisetWrapper<E> extends AbstractMultiset<E> {
    
    private class MultisetIterator implements Iterator<E> {
        
        private final Iterator<Entry<E, IntegerReference>> iterator = map
                .entrySet().iterator();
        private Entry<E, IntegerReference> entry;
        private int count;
        private boolean canRemove;
        
        @Override
        public boolean hasNext() {
            return (count > 0) || iterator.hasNext();
        }
        
        @Override
        public E next() {
            if (count <= 0) {
                entry = iterator.next();
                if (entry == null) {
                    canRemove = false;
                    return null;
                }
                
                count = entry.getValue().value;
            }
            
            count--;
            canRemove = true;
            return entry.getKey();
        }
        
        @Override
        public void remove() {
            if (!canRemove) throw new IllegalStateException();
            
            IntegerReference countReference = entry.getValue();
            if (countReference.value <= 1) iterator.remove();
            else countReference.value = count - 1;
            MultisetWrapper.this.size--;
            
            canRemove = false;
        }
    }
    
    private class UniqueIterator implements Iterator<E> {
        
        private final Iterator<Entry<E, IntegerReference>> iterator = map
                .entrySet().iterator();
        private Entry<E, IntegerReference> entry;
        private boolean canRemove;
        
        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }
        
        @Override
        public E next() {
            entry = iterator.next();
            canRemove = (entry != null);
            if (!canRemove) return null;
            return entry.getKey();
        }
        
        @Override
        public void remove() {
            if (!canRemove) throw new IllegalStateException();
            
            iterator.remove();
            MultisetWrapper.this.size -= entry.getValue().value;
            
            canRemove = false;
        }
    }
    
    private final Map<E, IntegerReference> map;
    
    private int size;
    
    public MultisetWrapper(Map<E, IntegerReference> map) {
        this.map = map;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MultisetWrapper)) return super.equals(o);
        MultisetWrapper<?> other = (MultisetWrapper<?>) o;
        
        if (size != other.size) return false;
        return map.equals(other.map);
    }
    
    @Override
    public int uniqueCount(Object o) {
        IntegerReference countReference = map.get(o);
        return (countReference == null) ? 0 : countReference.value;
    }
    
    @Override
    public Iterator<E> iterator() {
        return new MultisetIterator();
    }
    
    @Override
    public Iterator<E> uniqueIterator() {
        return new UniqueIterator();
    }
    
    @Override
    public int size() {
        return size;
    }
    
    @Override
    public int uniqueCount() {
        return map.size();
    }
    
    @Override
    public boolean isEmpty() {
        return size == 0;
    }
    
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }
    
    @Override
    public boolean contains(Object o, int c) {
        if (c <= 0) throw new IllegalArgumentException("c <= 0");
        return uniqueCount(o) >= c;
    }
    
    @Override
    public boolean containsExactly(Object o, int c) {
        if (c <= 0) throw new IllegalArgumentException("c <= 0");
        return uniqueCount(o) == c;
    }
    
    @Override
    public boolean add(E e, int c) {
        if (c <= 0) throw new IllegalArgumentException("c <= 0");
        
        IntegerReference countReference = map.get(e);
        if (countReference == null) map.put(e,
                countReference = new IntegerReference());
        
        countReference.value += c;
        size += c;
        return true;
    }
    
    @Override
    public boolean remove(Object o, int c) {
        if (c <= 0) throw new IllegalArgumentException("c <= 0");
        
        IntegerReference countReference = map.get(o);
        if (countReference == null) return false;
        
        if (countReference.value <= c) {
            map.remove(o);
            size -= countReference.value;
        } else {
            countReference.value -= c;
            size -= c;
        }
        
        return true;
    }
    
    @Override
    public boolean removeAll(Object o) {
        IntegerReference countReference = map.get(o);
        if (countReference == null) return false;
        
        map.remove(o);
        size -= countReference.value;
        return true;
    }
    
    @Override
    public void clear() {
        map.clear();
        size = 0;
    }
    
}