package at.stefl.commons.util.comparator;

import java.util.Comparator;

public class ReverseComparator<T> extends SimpleHybridDelegationComparator<T> {
    
    public ReverseComparator() {}
    
    public ReverseComparator(Comparator<? super T> comparator) {
        super(comparator);
    }
    
    @Override
    public int compare(T o1, T o2) {
        return -super.compare(o1, o2);
    }
    
}