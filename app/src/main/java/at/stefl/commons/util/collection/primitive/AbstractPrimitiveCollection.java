package at.stefl.commons.util.collection.primitive;

import java.util.AbstractCollection;

public abstract class AbstractPrimitiveCollection<E> extends
        AbstractCollection<E> implements PrimitiveCollection<E> {
    
    @Override
    public abstract PrimitiveIterator<E> iterator();
    
}