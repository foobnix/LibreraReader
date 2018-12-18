package at.stefl.commons.util.collection.primitive;

public interface PrimitiveBooleanCollection extends
        PrimitiveCollection<Boolean> {
    
    public boolean add(boolean e);
    
    public boolean addAll(boolean... a);
    
    public boolean addAll(boolean[] a, int off);
    
    public boolean addAll(boolean[] a, int off, int len);
    
    public boolean contains(boolean e);
    
    public boolean containsAll(boolean... a);
    
    public boolean containsAll(boolean[] a, int off);
    
    public boolean containsAll(boolean[] a, int off, int len);
    
    public boolean remove(boolean e);
    
    public boolean removeAll(boolean... a);
    
    public boolean removeAll(boolean[] a, int off);
    
    public boolean removeAll(boolean[] a, int off, int len);
    
    @Override
    public PrimitiveBooleanIterator iterator();
    
    public boolean[] toPrimitiveArray();
    
}