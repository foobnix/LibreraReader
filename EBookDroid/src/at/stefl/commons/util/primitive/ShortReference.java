package at.stefl.commons.util.primitive;

public class ShortReference implements PrimitiveReference<Short> {
    
    public static final Class<Short> WRAPPER_CLASS = Short.class;
    
    public static final Class<?> PRIMITIVE_CLASS = short.class;
    
    public short value;
    
    public ShortReference() {}
    
    public ShortReference(short value) {
        this.value = value;
    }
    
    @Override
    public Short getWrapper() {
        return Short.valueOf(value);
    }
    
    @Override
    public Class<Short> getWrapperClass() {
        return WRAPPER_CLASS;
    }
    
    @Override
    public Class<?> getPrimitiveClass() {
        return PRIMITIVE_CLASS;
    }
    
}