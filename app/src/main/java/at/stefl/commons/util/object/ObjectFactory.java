package at.stefl.commons.util.object;

public interface ObjectFactory<T, C> {
    
    public T create(C context);
    
}