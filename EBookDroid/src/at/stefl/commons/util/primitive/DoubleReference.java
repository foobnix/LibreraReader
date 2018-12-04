package at.stefl.commons.util.primitive;

public class DoubleReference implements PrimitiveReference<Double> {
    
    public static final Class<Double> WRAPPER_CLASS = Double.class;
    
    public static final Class<?> PRIMITIVE_CLASS = double.class;
    
    public double value;
    
    public DoubleReference() {}
    
    public DoubleReference(double value) {
        this.value = value;
    }
    
    @Override
    public Double getWrapper() {
        return Double.valueOf(value);
    }
    
    @Override
    public Class<Double> getWrapperClass() {
        return WRAPPER_CLASS;
    }
    
    @Override
    public Class<?> getPrimitiveClass() {
        return PRIMITIVE_CLASS;
    }
    
}