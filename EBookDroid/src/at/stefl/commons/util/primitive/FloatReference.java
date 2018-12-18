package at.stefl.commons.util.primitive;

public class FloatReference implements PrimitiveReference<Float> {
    
    public static final Class<Float> WRAPPER_CLASS = Float.class;
    
    public static final Class<?> PRIMITIVE_CLASS = float.class;
    
    public float value;
    
    public FloatReference() {}
    
    public FloatReference(float value) {
        this.value = value;
    }
    
    @Override
    public Float getWrapper() {
        return Float.valueOf(value);
    }
    
    @Override
    public Class<Float> getWrapperClass() {
        return WRAPPER_CLASS;
    }
    
    @Override
    public Class<?> getPrimitiveClass() {
        return PRIMITIVE_CLASS;
    }
    
}