package at.stefl.commons.util;

public abstract class PrimitiveUtil {
    
    public static boolean isValidUnsignedByte(short v) {
        return (v & 0xff00) == 0;
    }
    
    public static boolean isValidUnsignedShort(int v) {
        return (v & 0xffff0000) == 0;
    }
    
    public static boolean isValidUnsignedInt(long v) {
        return (v & 0xffffffff00000000l) == 0;
    }
    
    public static void checkUnsignedByte(short v) {
        if (!isValidUnsignedByte(v)) throw new IllegalArgumentException(
                "unsigned byte out of range");
    }
    
    public static void checkUnsignedShort(int v) {
        if (!isValidUnsignedShort(v)) throw new IllegalArgumentException(
                "unsigned short out of range");
    }
    
    public static void checkUnsignedInt(long v) {
        if (!isValidUnsignedInt(v)) throw new IllegalArgumentException(
                "unsigned int out of range");
    }
    
    private PrimitiveUtil() {}
    
}