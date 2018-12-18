package at.stefl.commons.util.comparator;

import java.util.Comparator;

public class ComperatorUtil {
    
    @SuppressWarnings("unchecked")
    public static <T> int hybridCompare(T o1, T o2,
            Comparator<? super T> comparator) {
        if (comparator != null) return comparator.compare(o1, o2);
        Comparable<? super T> c1 = (Comparable<? super T>) o1;
        return c1.compareTo(o2);
    }
    
    public static <T> Comparator<T> reverseComparator() {
        return new ReverseComparator<T>();
    }
    
    public static <T> Comparator<T> reverseComparator(
            Comparator<? super T> comparator) {
        return new ReverseComparator<T>(comparator);
    }
    
    private ComperatorUtil() {}
    
}