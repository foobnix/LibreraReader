package at.stefl.commons.util.comparator;

import java.util.Collection;
import java.util.Comparator;

public class CollectionSizeComparator implements Comparator<Collection<?>> {
    
    @Override
    public int compare(Collection<?> o1, Collection<?> o2) {
        return o1.size() - o2.size();
    }
    
}