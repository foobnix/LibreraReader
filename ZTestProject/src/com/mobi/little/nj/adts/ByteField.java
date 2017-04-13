/**
 * Copyright (C) 2013 
 * Nicholas J. Little <arealityfarbetween@googlemail.com>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.mobi.little.nj.adts;

import java.nio.ByteBuffer;



public class ByteField implements Comparable<ByteField>, Cloneable {

    private String       name;

    private Integer      offset;

    protected ByteBuffer raw;

    ByteField(int o) {
        setOffset(o);
    }

    public ByteField(int l, String n) {
        this(0);
        name = n;
        raw = ByteBuffer.allocate(l);
    }
    
    public ByteField(int o, int l, String n) {
        this(l, n);
        setOffset(o);
    }
    
    public ByteField(int l, String n, byte[] d) {
        this(l, n);
        setBytes(d);
    }
    
    public ByteField(int o, int l, String n, byte[] d) {
        this(l, n, d);
        setOffset(o);
    }
    
    /**
     * Extracts data for this ByteField from the ByteBuffer, r.
     * 
     * @param r ByteBuffer to extract from
     */
    public void parse(ByteBuffer r) {
        r.position(offset.intValue());
        byte[] tmp = new byte[raw.capacity()];
        r.get(tmp);
        setBytes(tmp);
    }

    public int getLength() {
        return raw.capacity();
    }

    public String getName() {
        return name;
    }

    public int getOffset() {
        return offset.intValue();
    }
    
    public void setOffset(int i) {
        offset = Integer.valueOf(i);
    }

    public byte[] getBytes() {
        return raw.array();
    }

    /**
     * Fills the buffer with the specified bytes, excluding
     * any overflow.
     *  
     * @param bytes
     */
    public final void setBytes(byte[] bytes) {
        raw.rewind();
        raw.put(bytes, 0, raw.capacity() < bytes.length ? raw.capacity()
                : bytes.length);
        
        while(raw.position() < raw.capacity())
            raw.put((byte)0x0);
    }

    public ByteBuffer getBuffer() {
        raw.rewind();
        return raw.duplicate();
    }
    
    public Object getValue() { return null; }
    
    public void setValue(Object x) { }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ByteField b) {
        return offset.compareTo(b.offset);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return offset.hashCode();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        
        if (!(obj instanceof ByteField))
            return false;
            
        return compareTo((ByteField) obj) == 0;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public ByteField clone() {
        ByteField that;
        try {
            that = (ByteField) super.clone();
            
            that.offset = Integer.valueOf(offset);
            that.raw = ByteBuffer.allocate(raw.capacity());
            that.setBytes(getBytes());
         
            return that;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("Name: %s, Offset: %d, Length: %d\n", 
                name, offset, getLength()));
        
        
        return sb.toString();
    }
}
