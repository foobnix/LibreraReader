package at.stefl.commons.util.comparator;

import java.util.Comparator;

public abstract class SimpleDelegationComparator<T> extends
        DelegationComparator<T, T> {
    
    SimpleDelegationComparator() {}
    
    public SimpleDelegationComparator(Comparator<? super T> comparator) {
        super(comparator);
    }
    
    protected T translate(T o) {
        return o;
    }
    
    @Override
    public int compare(T o1, T o2) {
        return comparator.compare(o1, o2);
    }
    
}