package at.stefl.commons.util.comparator;

import java.util.Comparator;

public abstract class DelegationComparator<T1, T2> implements Comparator<T2> {
    
    protected Comparator<? super T1> comparator;
    
    DelegationComparator() {}
    
    public DelegationComparator(Comparator<? super T1> comparator) {
        if (comparator == null) throw new NullPointerException();
        
        this.comparator = comparator;
    }
    
    @Override
    public abstract int compare(T2 o1, T2 o2);
    
}