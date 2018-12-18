package at.stefl.commons.util.primitive;

public class IntegerReference implements PrimitiveReference<Integer> {
    
    public static final Class<Integer> WRAPPER_CLASS = Integer.class;
    
    public static final Class<?> PRIMITIVE_CLASS = int.class;
    
    public int value;
    
    public IntegerReference() {}
    
    public IntegerReference(int value) {
        this.value = value;
    }
    
    @Override
    public Integer getWrapper() {
        return Integer.valueOf(value);
    }
    
    @Override
    public Class<Integer> getWrapperClass() {
        return WRAPPER_CLASS;
    }
    
    @Override
    public Class<?> getPrimitiveClass() {
        return PRIMITIVE_CLASS;
    }
    
}