/**
 * Copyright (C) 2013 Nicholas J. Little <arealityfarbetween@googlemail.com>
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
package com.mobi;

import java.util.Arrays;


public class RawCodec implements Codec {

    /*
     * (non-Javadoc)
     * 
     * @see algorithms.ICodec#compress(byte[])
     */
    @Override
    public byte[] compress(byte[] input) {
        return Arrays.copyOf(input, input.length);
    }

    /*
     * (non-Javadoc)
     * 
     * @see algorithms.ICodec#decompress(byte[])
     */
    @Override
    public byte[] decompress(byte[] input) {
        return Arrays.copyOf(input, input.length);
    }
}
