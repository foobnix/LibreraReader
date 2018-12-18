package at.stefl.commons.util.comparator;

import java.util.Comparator;
import java.util.Map.Entry;

public class MapEntryValueComparator<T> extends
        HybridDelegationComparator<T, Entry<?, ? extends T>> {
    
    public MapEntryValueComparator() {}
    
    public MapEntryValueComparator(Comparator<? super T> comparator) {
        super(comparator);
    }
    
    @Override
    public int compare(Entry<?, ? extends T> o1, Entry<?, ? extends T> o2) {
        return comparator.compare(o1.getValue(), o2.getValue());
    }
    
}