/**
 * Copyright (C) 2013 
Nicholas J. Little <arealityfarbetween@googlemail.com>
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

import static com.mobi.format.headers.PdbHeader.CHARSET;

import com.mobi.little.nj.adts.ByteFieldSet;
import com.mobi.little.nj.adts.IntByteField;
import com.mobi.little.nj.adts.StringByteField;

public class FlisRecord {

    private static final ByteFieldSet ALL_FIELDS = new ByteFieldSet();
    static {
        ALL_FIELDS.add(new StringByteField(4, "Identifier", CHARSET, "FLIS"));
        ALL_FIELDS.add(new IntByteField("Unknown", 8));
        ALL_FIELDS.add(new IntByteField("Unknown", 65));
        ALL_FIELDS.add(new IntByteField("Unknown", 0));
        ALL_FIELDS.add(new IntByteField("Unknown", 0));
        ALL_FIELDS.add(new IntByteField("Unknown", -1));
        ALL_FIELDS.add(new IntByteField("Unknown", 1));
        ALL_FIELDS.add(new IntByteField("Unknown", 3));
    }

    public static final ByteFieldSet getFields() {
        return ALL_FIELDS.clone();
    }
}
