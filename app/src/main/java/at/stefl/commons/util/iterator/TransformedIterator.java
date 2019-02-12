package at.stefl.commons.util.iterator;

import java.util.Iterator;

import at.stefl.commons.util.object.ObjectTransformer;

public class TransformedIterator<E1, E2> extends DelegationIterator<E1, E2> {
    
    private final ObjectTransformer<? super E1, ? extends E2> transformer;
    
    public TransformedIterator(Iterator<? extends E1> iterator,
            ObjectTransformer<? super E1, ? extends E2> transformer) {
        super(iterator);
        
        this.transformer = transformer;
    }
    
    @Override
    public E2 next() {
        return transformer.transform(iterator.next());
    }
    
}