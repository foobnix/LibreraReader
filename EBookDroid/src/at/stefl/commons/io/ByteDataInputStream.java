package at.stefl.commons.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class ByteDataInputStream extends DelegationInputStream {
    
    private final byte[] maxUnit = Endianness.getMaxUnitBuffer();
    
    private Endianness endianness;
    
    public ByteDataInputStream(InputStream in) {
        this(in, Endianness.getNative());
    }
    
    public ByteDataInputStream(InputStream in, Endianness endianness) {
        super(in);
        
        this.endianness = endianness;
    }
    
    public Endianness getEndianness() {
        return endianness;
    }
    
    public void setEndianness(Endianness endianness) {
        this.endianness = endianness;
    }
    
    public byte[] readFully(int len) throws IOException {
        return ByteStreamUtil.readFully(in, len);
    }
    
    public void readFully(byte[] b) throws IOException {
        ByteStreamUtil.readFully(in, b);
    }
    
    public void readFully(byte[] b, int off, int len) throws IOException {
        ByteStreamUtil.readFully(in, b, off, len);
    }
    
    public byte[] readUnits(int unit, int len) throws IOException {
        byte[] result = ByteStreamUtil.readFully(in, len);
        Endianness.swap(unit, result);
        return result;
    }
    
    public void readUnits(int unit, byte[] b) throws IOException {
        ByteStreamUtil.readFully(in, b);
        Endianness.swap(unit, b);
    }
    
    public void readUnits(int unit, byte[] b, int off, int len)
            throws IOException {
        ByteStreamUtil.readFully(in, b, off, len);
        Endianness.swap(unit, b, off, len);
    }
    
    private void readUnit(int size) throws IOException {
        ByteStreamUtil.readFully(in, maxUnit, 0, size);
    }
    
    public boolean readBoolean() throws IOException {
        int read = in.read();
        if (read == -1) throw new EOFException();
        return read != 0;
    }
    
    public byte readByte() throws IOException {
        int read = in.read();
        if (read == -1) throw new EOFException();
        return (byte) read;
    }
    
    public short readUnsignedByte() throws IOException {
        return (short) (readByte() & 0xff);
    }
    
    public char readChar() throws IOException {
        readUnit(2);
        return endianness.getAsChar(maxUnit);
    }
    
    public short readShort() throws IOException {
        readUnit(2);
        return endianness.getAsShort(maxUnit);
    }
    
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xffff;
    }
    
    public int readInt() throws IOException {
        readUnit(4);
        return endianness.getAsInt(maxUnit);
    }
    
    public long readUnsignedInt() throws IOException {
        return readInt() & 0xffffffffl;
    }
    
    public long readLong() throws IOException {
        readUnit(8);
        return endianness.getAsLong(maxUnit);
    }
    
}