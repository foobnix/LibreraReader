package at.stefl.commons.io;

import java.nio.ByteOrder;

import at.stefl.commons.util.InaccessibleSectionException;
import at.stefl.commons.util.array.ArrayUtil;

public enum Endianness {
    
    LITTLE(ByteOrder.LITTLE_ENDIAN) {
        @Override
        public char getAsChar(byte[] b) {
            return (char) (((b[0] & 0xff) << 8) | (b[1] & 0xff));
        }
        
        @Override
        public char getAsChar(byte[] b, int off) {
            return (char) (((b[off++] & 0xff) << 8) | (b[off] & 0xff));
        }
        
        @Override
        public char getAsChar(byte b1, byte b2) {
            return (char) (((b1 & 0xff) << 8) | (b2 & 0xff));
        }
        
        @Override
        public short getAsShort(byte[] b) {
            return (short) (((b[0] & 0xff) << 8) | (b[1] & 0xff));
        }
        
        @Override
        public short getAsShort(byte[] b, int off) {
            return (short) (((b[off++] & 0xff) << 8) | (b[off] & 0xff));
        }
        
        @Override
        public short getAsShort(byte b1, byte b2) {
            return (short) (((b1 & 0xff) << 8) | (b2 & 0xff));
        }
        
        @Override
        public int getAsInt(byte[] b) {
            return ((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16)
                    | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
        }
        
        @Override
        public int getAsInt(byte[] b, int off) {
            return ((b[off++] & 0xff) << 24) | ((b[off++] & 0xff) << 16)
                    | ((b[off++] & 0xff) << 8) | (b[off] & 0xff);
        }
        
        @Override
        public int getAsInt(byte b1, byte b2, byte b3, byte b4) {
            return ((b1 & 0xff) << 24) | ((b2 & 0xff) << 16)
                    | ((b3 & 0xff) << 8) | (b4 & 0xff);
        }
        
        @Override
        public long getAsLong(byte[] b) {
            return ((b[0] & 0xffl) << 56) | ((b[1] & 0xffl) << 48)
                    | ((b[2] & 0xffl) << 40) | ((b[3] & 0xffl) << 32)
                    | ((b[4] & 0xffl) << 24) | ((b[5] & 0xffl) << 16)
                    | ((b[6] & 0xffl) << 8) | (b[7] & 0xffl);
        }
        
        @Override
        public long getAsLong(byte[] b, int off) {
            return ((b[off++] & 0xffl) << 56) | ((b[off++] & 0xffl) << 48)
                    | ((b[off++] & 0xffl) << 40) | ((b[off++] & 0xffl) << 32)
                    | ((b[off++] & 0xffl) << 24) | ((b[off++] & 0xffl) << 16)
                    | ((b[off++] & 0xffl) << 8) | (b[off] & 0xffl);
        }
        
        @Override
        public long getAsLong(byte b1, byte b2, byte b3, byte b4, byte b5,
                byte b6, byte b7, byte b8) {
            return ((b1 & 0xffl) << 56) | ((b2 & 0xffl) << 48)
                    | ((b3 & 0xffl) << 40) | ((b4 & 0xffl) << 32)
                    | ((b5 & 0xffl) << 24) | ((b6 & 0xffl) << 16)
                    | ((b7 & 0xffl) << 8) | (b8 & 0xffl);
        }
        
        @Override
        public void getBytes(char v, byte[] b) {
            b[0] = (byte) (v >> 8);
            b[1] = (byte) v;
        }
        
        @Override
        public void getBytes(char v, byte[] b, int off) {
            b[off++] = (byte) (v >> 8);
            b[off] = (byte) v;
        }
        
        @Override
        public void getBytes(short v, byte[] b) {
            b[0] = (byte) (v >> 8);
            b[1] = (byte) v;
        }
        
        @Override
        public void getBytes(short v, byte[] b, int off) {
            b[off++] = (byte) (v >> 8);
            b[off] = (byte) v;
        }
        
        @Override
        public void getBytes(int v, byte[] b) {
            b[0] = (byte) (v >> 24);
            b[1] = (byte) (v >> 16);
            b[2] = (byte) (v >> 8);
            b[3] = (byte) v;
        }
        
        @Override
        public void getBytes(int v, byte[] b, int off) {
            b[off++] = (byte) (v >> 24);
            b[off++] = (byte) (v >> 16);
            b[off++] = (byte) (v >> 8);
            b[off] = (byte) v;
        }
        
        @Override
        public void getBytes(long v, byte[] b) {
            b[0] = (byte) (v >> 56);
            b[1] = (byte) (v >> 48);
            b[2] = (byte) (v >> 40);
            b[3] = (byte) (v >> 32);
            b[4] = (byte) (v >> 24);
            b[5] = (byte) (v >> 16);
            b[6] = (byte) (v >> 8);
            b[7] = (byte) v;
        }
        
        @Override
        public void getBytes(long v, byte[] b, int off) {
            b[off++] = (byte) (v >> 56);
            b[off++] = (byte) (v >> 48);
            b[off++] = (byte) (v >> 40);
            b[off++] = (byte) (v >> 32);
            b[off++] = (byte) (v >> 24);
            b[off++] = (byte) (v >> 16);
            b[off++] = (byte) (v >> 8);
            b[off] = (byte) v;
        }
    },
    BIG(ByteOrder.BIG_ENDIAN) {
    	@Override
        public char getAsChar(byte[] b) {
            return (char) ((b[0] & 0xff) | ((b[1] & 0xff) << 8));
        }
        
        @Override
        public char getAsChar(byte[] b, int off) {
            return (char) ((b[off++] & 0xff) | ((b[off] & 0xff) << 8));
        }
        
        @Override
        public char getAsChar(byte b1, byte b2) {
            return (char) ((b1 & 0xff) | ((b2 & 0xff) << 8));
        }
        
        @Override
        public short getAsShort(byte[] b) {
            return (short) ((b[0] & 0xff) | ((b[1] & 0xff) << 8));
        }
        
        @Override
        public short getAsShort(byte[] b, int off) {
            return (short) ((b[off++] & 0xff) | ((b[off] & 0xff) << 8));
        }
        
        @Override
        public short getAsShort(byte b1, byte b2) {
            return (short) ((b1 & 0xff) | ((b2 & 0xff) << 8));
        }
        
        @Override
        public int getAsInt(byte[] b) {
            return (b[0] & 0xff) | ((b[1] & 0xff) << 8) | ((b[2] & 0xff) << 16)
                    | ((b[3] & 0xff) << 24);
        }
        
        @Override
        public int getAsInt(byte[] b, int off) {
            return (b[off++] & 0xff) | ((b[off++] & 0xff) << 8)
                    | ((b[off++] & 0xff) << 16) | ((b[off] & 0xff) << 24);
        }
        
        @Override
        public int getAsInt(byte b1, byte b2, byte b3, byte b4) {
            return (b1 & 0xff) | ((b2 & 0xff) << 8) | ((b3 & 0xff) << 16)
                    | ((b4 & 0xff) << 24);
        }
        
        @Override
        public long getAsLong(byte[] b) {
            return (b[0] & 0xffl) | ((b[1] & 0xffl) << 8)
                    | ((b[2] & 0xffl) << 16) | ((b[3] & 0xffl) << 24)
                    | ((b[4] & 0xffl) << 32) | ((b[5] & 0xffl) << 40)
                    | ((b[6] & 0xffl) << 48) | ((b[7] & 0xffl) << 56);
        }
        
        @Override
        public long getAsLong(byte[] b, int off) {
            return (b[off++] & 0xffl) | ((b[off++] & 0xffl) << 8)
                    | ((b[off++] & 0xffl) << 16) | ((b[off++] & 0xffl) << 24)
                    | ((b[off++] & 0xffl) << 32) | ((b[off++] & 0xffl) << 40)
                    | ((b[off++] & 0xffl) << 48) | ((b[off] & 0xffl) << 56);
        }
        
        @Override
        public long getAsLong(byte b1, byte b2, byte b3, byte b4, byte b5,
                byte b6, byte b7, byte b8) {
            return (b1 & 0xffl) | ((b2 & 0xffl) << 8) | ((b3 & 0xffl) << 16)
                    | ((b4 & 0xffl) << 24) | ((b5 & 0xffl) << 32)
                    | ((b6 & 0xffl) << 40) | ((b7 & 0xffl) << 48)
                    | ((b8 & 0xffl) << 56);
        }
        
        @Override
        public void getBytes(char v, byte[] b) {
            b[0] = (byte) v;
            b[1] = (byte) (v >> 8);
        }
        
        @Override
        public void getBytes(char v, byte[] b, int off) {
            b[off++] = (byte) v;
            b[off] = (byte) (v >> 8);
        }
        
        @Override
        public void getBytes(short v, byte[] b) {
            b[0] = (byte) v;
            b[1] = (byte) (v >> 8);
        }
        
        @Override
        public void getBytes(short v, byte[] b, int off) {
            b[off++] = (byte) v;
            b[off] = (byte) (v >> 8);
        }
        
        @Override
        public void getBytes(int v, byte[] b) {
            b[0] = (byte) v;
            b[1] = (byte) (v >> 8);
            b[2] = (byte) (v >> 16);
            b[3] = (byte) (v >> 24);
        }
        
        @Override
        public void getBytes(int v, byte[] b, int off) {
            b[off++] = (byte) v;
            b[off++] = (byte) (v >> 8);
            b[off++] = (byte) (v >> 16);
            b[off] = (byte) (v >> 24);
        }
        
        @Override
        public void getBytes(long v, byte[] b) {
            b[0] = (byte) v;
            b[1] = (byte) (v >> 8);
            b[2] = (byte) (v >> 16);
            b[3] = (byte) (v >> 24);
            b[4] = (byte) (v >> 32);
            b[5] = (byte) (v >> 40);
            b[6] = (byte) (v >> 48);
            b[7] = (byte) (v >> 56);
        }
        
        @Override
        public void getBytes(long v, byte[] b, int off) {
            b[off++] = (byte) v;
            b[off++] = (byte) (v >> 8);
            b[off++] = (byte) (v >> 16);
            b[off++] = (byte) (v >> 24);
            b[off++] = (byte) (v >> 32);
            b[off++] = (byte) (v >> 40);
            b[off++] = (byte) (v >> 48);
            b[off] = (byte) (v >> 56);
        }
    };
    
    public static final int MAX_UNIT_SIZE = 8;
    
    public static byte[] getMaxUnitBuffer() {
        return new byte[MAX_UNIT_SIZE];
    }
    
    public static short swap(short v) {
		return (short) (((v & 0x00ff) << 8) | ((v & 0xff00) >> 8));
	}
    
	public static int swap(int v) {
		return ((v & 0x000000ff) << 24) | ((v & 0x0000ff00) << 8)
				| ((v & 0x00ff0000) >>> 8) | ((v & 0xff000000) >>> 24);
	}
	
	public static long swap(long v) {
		return ((v & 0x00000000000000ffl) << 56) | ((v & 0x000000000000ff00l) << 40)
				| ((v & 0x0000000000ff0000l) << 24) | ((v & 0x00000000ff000000l) << 8)
				| ((v & 0x000000ff00000000l) >>> 8) | ((v & 0x0000ff0000000000l) >>> 24)
				| ((v & 0x00ff000000000000l) >>> 40) | ((v & 0xff00000000000000l) >>> 56);
	}
	
    public static void swap(byte[] b) {
        swap(b, 0, b.length);
    }
    
    public static void swap(byte[] b, int off, int len) {
        for (int i = off, j = off + len - 1; i < j; i++, j--) {
            ArrayUtil.swap(b, i, j);
        }
    }
    
    public static void swap(int unit, byte[] b) {
        swap(unit, b, 0, b.length);
    }
    
    public static void swap(int unit, byte[] b, int off, int len) {
        if (unit == 1) return;
        if ((len % unit) != 0) throw new IllegalArgumentException(
                "unti out of bounds");
        
        int end = off + len;
        
        for (int i = off; i < end; i += unit) {
            swap(b, i, unit);
        }
    }
    
    public static Endianness getByByteOrder(ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return Endianness.LITTLE;
        } else if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return Endianness.BIG;
        } else {
            throw new InaccessibleSectionException();
        }
    }
    
    public static Endianness getNative() {
        return getByByteOrder(ByteOrder.nativeOrder());
    }
    
    private final ByteOrder byteOrder;
    
    private Endianness(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }
    
    public ByteOrder getByteOrder() {
        return byteOrder;
    }
    
    public abstract char getAsChar(byte[] b);
    
    public abstract char getAsChar(byte[] b, int off);
    
    public abstract char getAsChar(byte b1, byte b2);
    
    public abstract short getAsShort(byte[] b);
    
    public abstract short getAsShort(byte[] b, int off);
    
    public abstract short getAsShort(byte b1, byte b2);
    
    public abstract int getAsInt(byte[] b);
    
    public abstract int getAsInt(byte[] b, int off);
    
    public abstract int getAsInt(byte b1, byte b2, byte b3, byte b4);
    
    public abstract long getAsLong(byte[] b);
    
    public abstract long getAsLong(byte[] b, int off);
    
    public abstract long getAsLong(byte b1, byte b2, byte b3, byte b4, byte b5,
            byte b6, byte b7, byte b8);
    
    public byte[] getBytes(char v) {
        byte[] result = new byte[2];
        getBytes(v, result);
        return result;
    }
    
    public abstract void getBytes(char v, byte[] b);
    
    public abstract void getBytes(char v, byte[] b, int off);
    
    public byte[] getBytes(short v) {
        byte[] result = new byte[2];
        getBytes(v, result);
        return result;
    }
    
    public abstract void getBytes(short v, byte[] b);
    
    public abstract void getBytes(short v, byte[] b, int off);
    
    public byte[] getBytes(int v) {
        byte[] result = new byte[4];
        getBytes(v, result);
        return result;
    }
    
    public abstract void getBytes(int v, byte[] b);
    
    public abstract void getBytes(int v, byte[] b, int off);
    
    public byte[] getBytes(long v) {
        byte[] result = new byte[8];
        getBytes(v, result);
        return result;
    }
    
    public abstract void getBytes(long v, byte[] b);
    
    public abstract void getBytes(long v, byte[] b, int off);
    
}