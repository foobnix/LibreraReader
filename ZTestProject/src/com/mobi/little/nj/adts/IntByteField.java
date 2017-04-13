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

public final class IntByteField extends ByteField {

    public static final int LENGTH = 4;
    
    public IntByteField(String n) {
        super(LENGTH, n);
    }
    
    public IntByteField(String n, int d) {
        this(n);
        setValue(d);
    }

    @Override
    public Integer getValue() {
        raw.rewind();
        return raw.getInt();
    }

    @Override
    public void setValue(Object x) {
        raw.rewind();
        raw.putInt((Integer)x);
    }
}
