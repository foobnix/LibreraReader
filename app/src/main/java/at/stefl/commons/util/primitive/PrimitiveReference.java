package at.stefl.commons.util.primitive;

public interface PrimitiveReference<T> {
    
    public T getWrapper();
    
    public Class<T> getWrapperClass();
    
    public Class<?> getPrimitiveClass();
    
}