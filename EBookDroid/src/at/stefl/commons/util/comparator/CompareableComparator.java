package at.stefl.commons.util.comparator;

import java.util.Comparator;

public class CompareableComparator<T extends Comparable<? super T>> implements
        Comparator<T> {
    
    @Override
    public int compare(T o1, T o2) {
        return o1.compareTo(o2);
    }
    
}