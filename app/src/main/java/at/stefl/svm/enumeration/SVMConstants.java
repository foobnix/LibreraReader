package at.stefl.svm.enumeration;

import at.stefl.commons.io.Endianness;

public class SVMConstants {
    
    public static final byte[] MAGIC_NUMBER = { 'V', 'C', 'L', 'M', 'T', 'F' };
    
    public static final Endianness ENDIANNESS = Endianness.LITTLE;
    
    private SVMConstants() {}
    
}