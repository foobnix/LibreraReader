package at.stefl.commons.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import at.stefl.commons.io.Endianness;

public class NumberUtil {
    
    public static byte parseByte(String value, byte nullValue) {
        return (value == null) ? nullValue : Byte.parseByte(value);
    }
    
    public static short parseShort(String value, short nullValue) {
        return (value == null) ? nullValue : Short.parseShort(value);
    }
    
    public static int parseInt(String value, int nullValue) {
        return (value == null) ? nullValue : Integer.parseInt(value);
    }
    
    public static long parseLong(String value, long nullValue) {
        return (value == null) ? nullValue : Long.parseLong(value);
    }
    
    public static float parseFloat(String value, float nullValue) {
        return (value == null) ? nullValue : Float.parseFloat(value);
    }
    
    public static double parseDouble(String value, double nullValue) {
        return (value == null) ? nullValue : Double.parseDouble(value);
    }
    
    public static boolean isValidUnsignedByte(int value) {
        return (value & 0xff00) == 0;
    }
    
    public static boolean isValidUnsignedShort(int value) {
        return (value & 0xffff0000) == 0;
    }
    
    public static boolean isValidUnsignedInt(long value) {
        return (value & 0xffffffff00000000l) == 0;
    }
    
    public static boolean isPowerOf2(int v) {
		return (v != 0) && ((v & (v - 1)) == 0);
	}
    
    public static boolean isPowerOf2(long v) {
		return (v != 0) && ((v & (v - 1)) == 0);
	}
    
    public static byte[] mapDoubleToInteger(double d, int len) {
		byte[] result = new byte[len];
		mapDoubleToInteger(d, result, 0, len);
		return result;
	}
    
    public static void mapDoubleToInteger(double d, byte[] b) {
    	mapDoubleToInteger(d, b, 0, b.length);
    }
    
	public static void mapDoubleToInteger(double d, byte[] b, int off, int len) {
		BigInteger max = BigInteger.ZERO.setBit(len * 8 - 1);
		
		if (d > 0) {
			max = max.subtract(BigInteger.ONE);
		}
		
		BigInteger value = BigDecimal.valueOf(d)
				.multiply(new BigDecimal(max)).toBigInteger();
		byte[] bytes = value.toByteArray();
		int from = off + bytes.length;
		int to = off + len;
		for (int i = 0; i < to; i++) {
			if (i < from) {
				b[i] = bytes[from - i - 1];
			} else {
				b[i] = (d < 0) ? (byte) 0xff : 0;
			}
		}
	}
	
	public static double mapIntegerToDouble(byte[] i) {
		switch (i.length) {
		case 1:
			return mapByteToDouble(i[0]);
		case 2:
			return mapShortToDouble(Endianness.BIG.getAsShort(i));
		case 4:
			return mapIntToDouble(Endianness.BIG.getAsInt(i));
		case 8:
			return mapLongToDouble(Endianness.BIG.getAsLong(i));
		default:
			BigInteger max = BigInteger.ZERO.setBit(i.length * 8 - 1);
			BigInteger value = new BigInteger(i);
			
			if (value.signum() == 1) {
				max = max.subtract(BigInteger.ONE);
			}
			
			return new BigDecimal(value).divide(new BigDecimal(max),
					MathContext.DECIMAL64).doubleValue();
		}
	}
	
	public static double mapByteToDouble(byte i) {
		double max = (i > 0) ? Byte.MAX_VALUE : -Byte.MIN_VALUE;
		return i / max;
	}
	
	public static double mapShortToDouble(short i) {
		double max = (i > 0) ? Short.MAX_VALUE : -Short.MIN_VALUE;
		return i / max;
	}
	
	public static double mapIntToDouble(int i) {
		double max = (i > 0) ? Integer.MAX_VALUE : -Integer.MIN_VALUE;
		return i / max;
	}
	
	public static double mapLongToDouble(long i) {
		double max = (i > 0) ? Long.MAX_VALUE : -Long.MIN_VALUE;
		return i / max;
	}
    
    private NumberUtil() {}
    
}