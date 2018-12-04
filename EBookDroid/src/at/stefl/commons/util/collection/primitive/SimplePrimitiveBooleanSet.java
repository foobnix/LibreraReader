package at.stefl.commons.util.collection.primitive;

public class SimplePrimitiveBooleanSet extends AbstractPrimitiveBooleanSet {
    
    private boolean containsFalse;
    private boolean containsTrue;
    
    @Override
    public boolean isEmpty() {
        return !containsFalse & !containsTrue;
    }
    
    @Override
    public boolean isFull() {
        return containsFalse & containsTrue;
    }
    
    @Override
    public int size() {
        return (containsFalse ? 1 : 0) + (containsTrue ? 1 : 0);
    }
    
    @Override
    public boolean add(boolean e) {
        if (e) return containsTrue ^ (containsTrue = true);
        return containsFalse ^ (containsFalse = true);
    }
    
    @Override
    public boolean contains(boolean e) {
        if (e) return containsTrue;
        return containsFalse;
    }
    
    @Override
    public boolean remove(boolean e) {
        if (e) return containsTrue ^ (containsTrue = true);
        return containsFalse ^ (containsFalse = true);
    }
    
    @Override
    public PrimitiveBooleanIterator iterator() {
        // TODO: implement
        return null;
    }
    
}