package at.stefl.commons.util.collection.primitive;

import java.util.Collection;
import java.util.Iterator;

public abstract class AbstractPrimitiveBooleanCollection extends
        AbstractPrimitiveCollection<Boolean> implements
        PrimitiveBooleanCollection {
    
    @Override
    public boolean add(Boolean e) {
        if (e == null) throw new NullPointerException();
        return add((boolean) e);
    }
    
    @Override
    public boolean addAll(Collection<? extends Boolean> c) {
        boolean result = false;
        
        if (c instanceof PrimitiveBooleanCollection) {
            PrimitiveBooleanIterator iterator = (PrimitiveBooleanIterator) c
                    .iterator();
            while (iterator.hasNext())
                result |= add(iterator.nextPrimitive());
        } else {
            for (Boolean e : c)
                result |= add(e);
        }
        
        return result;
    }
    
    @Override
    public boolean addAll(boolean... a) {
        return addAll(a, 0, a.length);
    }
    
    @Override
    public boolean addAll(boolean[] a, int off) {
        return addAll(a, off, a.length - off);
    }
    
    @Override
    public boolean addAll(boolean[] a, int off, int len) {
        boolean result = false;
        int end = off + len;
        for (int i = off; i < end; i++)
            result |= add(a[i]);
        return result;
    }
    
    @Override
    public boolean contains(Object o) {
        if (o == null) throw new NullPointerException();
        if (!(o instanceof Boolean)) throw new IllegalArgumentException();
        return contains((boolean) (Boolean) o);
    }
    
    @Override
    public boolean containsAll(Collection<?> c) {
        if (c instanceof PrimitiveBooleanCollection) {
            PrimitiveBooleanIterator iterator = (PrimitiveBooleanIterator) c
                    .iterator();
            while (iterator.hasNext())
                if (!contains(iterator.nextPrimitive())) return false;
        } else {
            for (Object e : c)
                if (!contains(e)) return false;
        }
        
        return true;
    }
    
    @Override
    public boolean containsAll(boolean... a) {
        return containsAll(a, 0, a.length);
    }
    
    @Override
    public boolean containsAll(boolean[] a, int off) {
        return containsAll(a, off, a.length - off);
    }
    
    @Override
    public boolean containsAll(boolean[] a, int off, int len) {
        int end = off + len;
        for (int i = off; i < end; i++)
            if (!contains(a[i])) return false;
        return true;
    }
    
    @Override
    public boolean remove(Object o) {
        if (o == null) throw new NullPointerException();
        if (!(o instanceof Boolean)) throw new IllegalArgumentException();
        return remove((boolean) (Boolean) o);
    }
    
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        
        if (c instanceof PrimitiveBooleanCollection) {
            PrimitiveBooleanIterator iterator = (PrimitiveBooleanIterator) c
                    .iterator();
            while (iterator.hasNext())
                result |= remove(iterator.nextPrimitive());
        } else {
            for (Object e : c)
                result |= remove(e);
        }
        
        return result;
    }
    
    @Override
    public boolean removeAll(boolean... a) {
        return removeAll(a, 0, a.length);
    }
    
    @Override
    public boolean removeAll(boolean[] a, int off) {
        return removeAll(a, off, a.length - off);
    }
    
    @Override
    public boolean removeAll(boolean[] a, int off, int len) {
        boolean result = false;
        int end = off + len;
        for (int i = off; i < end; i++)
            result |= remove(a[i]);
        return result;
    }
    
    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = false;
        
        if (c instanceof PrimitiveBooleanCollection) {
            PrimitiveBooleanIterator iterator = (PrimitiveBooleanIterator) c
                    .iterator();
            while (iterator.hasNext()) {
                if (!c.contains(iterator.nextPrimitive())) {
                    iterator.remove();
                    result = true;
                }
            }
        } else {
            Iterator<Boolean> iterator = iterator();
            while (iterator.hasNext()) {
                if (!c.contains(iterator.next())) {
                    iterator.remove();
                    result = true;
                }
            }
        }
        
        return result;
    }
    
    @Override
    public abstract PrimitiveBooleanIterator iterator();
    
    @Override
    public boolean[] toPrimitiveArray() {
        int size = size();
        boolean[] result = new boolean[size];
        PrimitiveBooleanIterator iterator = iterator();
        for (int i = 0; i < size; i++)
            result[i] = iterator.nextPrimitive();
        return result;
    }
    
}