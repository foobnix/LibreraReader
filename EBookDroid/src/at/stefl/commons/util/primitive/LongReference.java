package at.stefl.commons.util.primitive;

public class LongReference implements PrimitiveReference<Long> {
    
    public static final Class<Long> WRAPPER_CLASS = Long.class;
    
    public static final Class<?> PRIMITIVE_CLASS = long.class;
    
    public long value;
    
    public LongReference() {}
    
    public LongReference(long value) {
        this.value = value;
    }
    
    @Override
    public Long getWrapper() {
        return Long.valueOf(value);
    }
    
    @Override
    public Class<Long> getWrapperClass() {
        return WRAPPER_CLASS;
    }
    
    @Override
    public Class<?> getPrimitiveClass() {
        return PRIMITIVE_CLASS;
    }
    
}