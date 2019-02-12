package at.stefl.commons.util.comparator;

import java.util.Comparator;

import at.stefl.commons.util.object.ObjectTransformer;

public class TransformedComparator<T1, T2> extends DelegationComparator<T1, T2> {
    
    private final ObjectTransformer<? super T2, ? extends T1> transformer;
    
    public TransformedComparator(Comparator<? super T1> comparator,
            ObjectTransformer<? super T2, ? extends T1> transformer) {
        super(comparator);
        
        this.transformer = transformer;
    }
    
    @Override
    public int compare(T2 o1, T2 o2) {
        return comparator.compare(transformer.transform(o1),
                transformer.transform(o2));
    }
    
}