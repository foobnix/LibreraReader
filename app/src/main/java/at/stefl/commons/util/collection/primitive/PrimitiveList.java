package at.stefl.commons.util.collection.primitive;

import java.util.List;

public interface PrimitiveList<E> extends List<E>, PrimitiveCollection<E> {
    
    @Override
    public PrimitiveList<E> subList(int fromIndex, int toIndex);
    
}