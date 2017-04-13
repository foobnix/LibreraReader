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
package com.mobi.format.headers;

import java.nio.ByteBuffer;

import com.mobi.ByteFieldContainer;
import com.mobi.format.headers.Enumerations.Compression;
import com.mobi.little.nj.adts.ByteFieldMapSet;
import com.mobi.little.nj.adts.IntByteField;
import com.mobi.little.nj.adts.ShortByteField;

public class PalmDocHeader implements ByteFieldContainer {

    public static final String CURRENT_POSITION = "Current Position";

    public static final short RECORD_LENGTH = 4096;

    public static final String RECORD_SIZE = "Record Size";

    public static final String RECORD_COUNT = "Record Count";

    public static final String UNCOMPRESSED_TEXT_LENGTH = "Uncompressed Text Length";

    public static final String UNUSED = "Unused";

    public static final String COMPRESSION = "Compression";

    public static final ByteFieldMapSet ALL_FIELDS = new ByteFieldMapSet();

    public static final short LENGTH = 16;
    static {
        ALL_FIELDS.add(new ShortByteField(COMPRESSION, Compression.NONE
                .getValue()));
        ALL_FIELDS.add(new ShortByteField(UNUSED));
        ALL_FIELDS.add(new IntByteField(UNCOMPRESSED_TEXT_LENGTH));
        ALL_FIELDS.add(new ShortByteField(RECORD_COUNT));
        ALL_FIELDS.add(new ShortByteField(RECORD_SIZE, RECORD_LENGTH));
        ALL_FIELDS.add(new IntByteField(CURRENT_POSITION));
    }

    protected ByteFieldMapSet fields;

    public PalmDocHeader() {
        fields = ALL_FIELDS.clone();
    }

    public static PalmDocHeader parseBuffer(ByteBuffer raw) {
        PalmDocHeader header = new PalmDocHeader();
        header.parse(raw);
        return header;
    }

    public void parse(ByteBuffer in) {
        fields.parseAll(in);
    }

    public ByteFieldMapSet getFields() {
        return fields;
    }

    public Compression getCompression() {
        return Compression.valueOf(fields.<ShortByteField> getAs(COMPRESSION)
                .getValue());
    }

    public short getTextRecordCount() {
        return fields.<ShortByteField> getAs(RECORD_COUNT).getValue();
    }

    public short getTextRecordLength() {
        return fields.<ShortByteField> getAs(RECORD_SIZE).getValue();
    }

    public int getUncompressedTextLength() {
        return fields.<IntByteField> getAs(UNCOMPRESSED_TEXT_LENGTH).getValue();
    }

    public void setCompression(Compression c) {
        fields.<ShortByteField> getAs(COMPRESSION).setValue(c.getValue());
    }

    public void setTextRecordCount(int i) {
        fields.<ShortByteField> getAs(RECORD_COUNT).setValue((short) i);
    }

    public void setUncompressedTextLength(int i) {
        fields.<IntByteField> getAs(UNCOMPRESSED_TEXT_LENGTH).setValue(i);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[::::PalmDOC Header::::]\n");
        sb.append(fields);
        return sb.toString();
    }
}
