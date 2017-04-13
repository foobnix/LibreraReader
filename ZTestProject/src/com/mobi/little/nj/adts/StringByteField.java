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

import java.nio.charset.Charset;

public final class StringByteField extends ByteField {

    private Charset charset;

    public StringByteField(int l, String n, Charset c) {
        super(l, n);
        charset = c;
    }

    public StringByteField(int l, String n, Charset c, String d) {
        this(l, n, c);
        setValue(d);
    }

    @Override
    public String getValue() {
        return new String(getBytes(), charset).trim();
    }

    /**
     * Sets the string value, but will not overflow the field
     * @param x
     */
    @Override
    public void setValue(Object x) {
        byte[] in = x.toString().trim().getBytes(charset);
        setBytes(in);
    }
    
    public Charset getCharset() { return charset; }
    
    /* (non-Javadoc)
     * @see little.nj.adts.ByteField#clone()
     */
    @Override
    public ByteField clone() {
        StringByteField that = (StringByteField)super.clone();
        
        that.charset = charset;
        
        return that;
    }
}
