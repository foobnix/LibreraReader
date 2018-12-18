package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class ByteDataOutputStream extends DelegationOutputStream {
    
    private final byte[] maxUnit = Endianness.getMaxUnitBuffer();
    
    private Endianness endianness;
    
    public ByteDataOutputStream(OutputStream out) {
        this(out, Endianness.getNative());
    }
    
    public ByteDataOutputStream(OutputStream out, Endianness endianness) {
        super(out);
        
        this.endianness = endianness;
    }
    
    public Endianness getEndianness() {
        return endianness;
    }
    
    public void setEndianness(Endianness endianness) {
        this.endianness = endianness;
    }
    
    // TODO: improve?
    public void writeUnits(int unit, byte[] b) throws IOException {
        b = b.clone();
        Endianness.swap(unit, b);
        write(b);
    }
    
    // TODO: improve?
    public void writeUnits(int unit, byte[] b, int off, int len)
            throws IOException {
        b = Arrays.copyOfRange(b, off, off + len);
        Endianness.swap(unit, b);
        write(b);
    }
    
    private void writeUnit(int size) throws IOException {
        out.write(maxUnit, 0, size);
    }
    
    public void writeBoolean(boolean v) throws IOException {
        out.write(v ? 1 : 0);
    }
    
    public void writeByte(byte v) throws IOException {
        out.write(v);
    }
    
    public void writeUnsignedByte(short v) throws IOException {
        writeByte((byte) v);
    }
    
    public void writeChar(char v) throws IOException {
        endianness.getBytes(v, maxUnit);
        writeUnit(2);
    }
    
    public void writeShort(short v) throws IOException {
        endianness.getBytes(v, maxUnit);
        writeUnit(2);
    }
    
    public void writeUnsignedShort(int v) throws IOException {
        writeShort((short) v);
    }
    
    public void writeInt(int v) throws IOException {
        endianness.getBytes(v, maxUnit);
        writeUnit(4);
    }
    
    public void writeUnsignedInt(long v) throws IOException {
        writeInt((int) v);
    }
    
    public void writeLong(long v) throws IOException {
        endianness.getBytes(v, maxUnit);
        writeUnit(8);
    }
    
}