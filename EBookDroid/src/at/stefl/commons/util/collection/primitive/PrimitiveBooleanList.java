package at.stefl.commons.util.collection.primitive;

public interface PrimitiveBooleanList extends PrimitiveList<Boolean>,
        PrimitiveBooleanCollection {
    
    public void add(int index, boolean element);
    
    public boolean getPrimitive(int index);
    
    public int indexOf(boolean e);
    
    public int lastIndexOf(boolean e);
    
    public Boolean set(int index, boolean element);
    
    @Override
    public PrimitiveBooleanList subList(int fromIndex, int toIndex);
    
}