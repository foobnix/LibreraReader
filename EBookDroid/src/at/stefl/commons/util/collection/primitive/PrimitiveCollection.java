package at.stefl.commons.util.collection.primitive;

import java.util.Collection;

public interface PrimitiveCollection<E> extends Collection<E>, Iterable<E> {
    
    @Override
    public PrimitiveIterator<E> iterator();
    
}