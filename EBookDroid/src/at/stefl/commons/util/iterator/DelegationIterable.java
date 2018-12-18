package at.stefl.commons.util.iterator;

public abstract class DelegationIterable<T1, T2> implements Iterable<T2> {
    
    protected final Iterable<? extends T1> iterable;
    
    public DelegationIterable(Iterable<? extends T1> iterable) {
        this.iterable = iterable;
    }
    
}