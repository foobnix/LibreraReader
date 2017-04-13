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
package com.mobi.format.records;

import com.mobi.little.nj.adts.ByteField;
import com.mobi.little.nj.adts.ByteFieldSet;

public class EofRecord {

    private static final ByteFieldSet ALL_FIELDS = new ByteFieldSet();
    static {
        ALL_FIELDS.add(new ByteField(1, "Unknown", new byte[] { -23 }));
        ALL_FIELDS.add(new ByteField(1, "Unknown", new byte[] { -114 }));
        ALL_FIELDS.add(new ByteField(1, "Unknown", new byte[] { 13 }));
        ALL_FIELDS.add(new ByteField(1, "Unknown", new byte[] { 10 }));
    }

    public static final ByteFieldSet getFields() {
        return ALL_FIELDS.clone();
    }
}
