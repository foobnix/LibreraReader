package at.stefl.commons.util.primitive;

public class ByteReference implements PrimitiveReference<Byte> {
    
    public static final Class<Byte> WRAPPER_CLASS = Byte.class;
    
    public static final Class<?> PRIMITIVE_CLASS = byte.class;
    
    public byte value;
    
    public ByteReference() {}
    
    public ByteReference(byte value) {
        this.value = value;
    }
    
    @Override
    public Byte getWrapper() {
        return Byte.valueOf(value);
    }
    
    @Override
    public Class<Byte> getWrapperClass() {
        return WRAPPER_CLASS;
    }
    
    @Override
    public Class<?> getPrimitiveClass() {
        return PRIMITIVE_CLASS;
    }
    
}