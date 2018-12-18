package at.stefl.commons.util.comparator;

import java.util.Comparator;

public abstract class SimpleHybridDelegationComparator<T> extends
        HybridDelegationComparator<T, T> {
    
    public SimpleHybridDelegationComparator() {}
    
    public SimpleHybridDelegationComparator(Comparator<? super T> comparator) {
        super(comparator);
    }
    
    @Override
    public int compare(T o1, T o2) {
        return ComperatorUtil.hybridCompare(o1, o2, comparator);
    }
    
}