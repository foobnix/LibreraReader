package at.stefl.commons.util.comparator;

import java.util.Comparator;

public abstract class HybridDelegationComparator<T1, T2> extends
        DelegationComparator<T1, T2> {
    
    public HybridDelegationComparator() {}
    
    public HybridDelegationComparator(Comparator<? super T1> comparator) {
        super(comparator);
    }
    
    @Override
    public abstract int compare(T2 o1, T2 o2);
    
}