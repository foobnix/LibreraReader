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

import static com.mobi.format.headers.PdbHeader.CHARSET;

import java.nio.ByteBuffer;

import com.mobi.ByteFieldContainer;
import com.mobi.format.headers.Enumerations.Encoding;
import com.mobi.format.headers.Enumerations.Locale;
import com.mobi.format.headers.Enumerations.MobiType;
import com.mobi.little.nj.adts.ByteField;
import com.mobi.little.nj.adts.ByteFieldMapSet;
import com.mobi.little.nj.adts.IntByteField;
import com.mobi.little.nj.adts.ShortByteField;
import com.mobi.little.nj.adts.StringByteField;
import com.mobi.little.nj.algorithms.KmpSearch;

public class MobiDocHeader implements ByteFieldContainer {

    public static final String MOBI = "MOBI";

    public static final String INDX_RECORD = "INDX Record";

    public static final String EXTRA_RECORD_FLAGS = "Extra Record Flags";

    public static final String FLIS_COUNT = "FLIS Count";

    public static final String FLIS_RECORD = "FLIS Record";

    public static final String FCIS_COUNT = "FCIS Count";

    public static final String FCIS_RECORD = "FCIS Record";

    public static final String LAST_CONTENT_RECORD = "Last Content Record";

    public static final String FIRST_CONTENT_RECORD = "First Content Record";

    public static final String DRM_FLAGS = "DRM Flags";

    public static final String DRM_SIZE = "DRM Size";

    public static final String DRM_COUNT = "DRM Count";

    public static final String DRM_OFFSET = "DRM Offset";

    public static final String EXTH_FLAGS = "EXTH Flags";

    public static final String HUFFMAN_TABLE_LENGTH = "Huffman Table Length";

    public static final String HUFFMAN_TABLE_OFFSET = "Huffman Table Offset";

    public static final String HUFFMAN_RECORD_COUNT = "Huffman Record Count";

    public static final String FIRST_HUFFMAN_RECORD = "First Huffman Record";

    public static final String FIRST_IMAGE_RECORD = "First Image Record";

    public static final String MINIMUM_VERSION_COMPATIBLE_READER = "Minimum Version Compatible Reader";

    public static final String LANGUAGE_OUTPUT = "Language: Output";

    public static final String LANGUAGE_INPUT = "Language: Input";

    public static final String LOCALE = "Locale";

    public static final String FULL_NAME_LENGTH = "Full Name Length";

    public static final String FULL_NAME_OFFSET = "Full Name Offset";

    public static final String FIRST_NON_BOOK_RECORD = "First Non-Book Record";

    public static final String INDEX_EXTRA_FMT = "Index Extra %1$d";

    public static final String INDEX_KEYS = "Index Keys";

    public static final String INDEX_NAMES = "Index Names";

    public static final String INFLEXION_INDEX = "Inflexion Index";

    public static final String ORTHOGRAPHIC_INDEX = "Orthographic Index";

    public static final String VERSION = "Version";

    public static final String UNIQUE_ID = "Unique ID";

    public static final String ENCODING = "Encoding";

    public static final String MOBI_TYPE = "MobiType";

    public static final String LENGTH = "Length";

    public static final String IDENTIFIER = "Identifier";

    /**
     * A placeholder for all known fields. This is mainly useful for new headers
     * and allows us to store some defaults
     */
    public static final ByteFieldMapSet ALL_FIELDS = new ByteFieldMapSet();

    /**
     * The number of 'extra index' fields. These are unused
     */
    public static final short INDEX_EXTRAS = 6;

    /**
     * Populate the ALL_FIELDS ByteFieldSet
     */
    static {
        ALL_FIELDS.add(new StringByteField(4, IDENTIFIER, CHARSET, MOBI));
        ALL_FIELDS.add(new IntByteField(LENGTH));
        ALL_FIELDS.add(new IntByteField(MOBI_TYPE, MobiType.PALM_BOOK
                .getValue()));
        ALL_FIELDS.add(new IntByteField(ENCODING, Encoding.UTF8.getValue()));
        ALL_FIELDS.add(new IntByteField(UNIQUE_ID));
        ALL_FIELDS.add(new IntByteField(VERSION, 6));
        ALL_FIELDS.add(new IntByteField(ORTHOGRAPHIC_INDEX, -1));
        ALL_FIELDS.add(new IntByteField(INFLEXION_INDEX, -1));
        ALL_FIELDS.add(new IntByteField(INDEX_NAMES, -1));
        ALL_FIELDS.add(new IntByteField(INDEX_KEYS, -1));
        for (int i = 0; i < INDEX_EXTRAS; i++)
            ALL_FIELDS.add(new IntByteField(String.format(INDEX_EXTRA_FMT, i),
                    -1));
        ALL_FIELDS.add(new IntByteField(FIRST_NON_BOOK_RECORD, -1));
        ALL_FIELDS.add(new IntByteField(FULL_NAME_OFFSET));
        ALL_FIELDS.add(new IntByteField(FULL_NAME_LENGTH));
        ALL_FIELDS.add(new IntByteField(LOCALE, Locale.UK_ENGLISH.getValue()));
        ALL_FIELDS.add(new IntByteField(LANGUAGE_INPUT));
        ALL_FIELDS.add(new IntByteField(LANGUAGE_OUTPUT));
        ALL_FIELDS.add(new IntByteField(MINIMUM_VERSION_COMPATIBLE_READER));
        ALL_FIELDS.add(new IntByteField(FIRST_IMAGE_RECORD, -1));
        ALL_FIELDS.add(new IntByteField(FIRST_HUFFMAN_RECORD, -1));
        ALL_FIELDS.add(new IntByteField(HUFFMAN_RECORD_COUNT));
        ALL_FIELDS.add(new IntByteField(HUFFMAN_TABLE_OFFSET));
        ALL_FIELDS.add(new IntByteField(HUFFMAN_TABLE_LENGTH));
        ALL_FIELDS.add(new IntByteField(EXTH_FLAGS));
        ALL_FIELDS.add(new ByteField(32, "32 Unknown Bytes"));
        ALL_FIELDS.add(new IntByteField(DRM_OFFSET, -1));
        ALL_FIELDS.add(new IntByteField(DRM_COUNT, -1));
        ALL_FIELDS.add(new IntByteField(DRM_SIZE));
        ALL_FIELDS.add(new IntByteField(DRM_FLAGS));
        ALL_FIELDS.add(new ByteField(12, "12 Unknown Bytes"));
        ALL_FIELDS.add(new ShortByteField(FIRST_CONTENT_RECORD, (short) -1));
        ALL_FIELDS.add(new ShortByteField(LAST_CONTENT_RECORD, (short) -1));
        ALL_FIELDS.add(new IntByteField("Unknown Integer", 1));
        ALL_FIELDS.add(new IntByteField(FCIS_RECORD, -1));
        ALL_FIELDS.add(new IntByteField(FCIS_COUNT));
        ALL_FIELDS.add(new IntByteField(FLIS_RECORD, -1));
        ALL_FIELDS.add(new IntByteField(FLIS_COUNT));
        ALL_FIELDS.add(new ByteField(8, "8 Unknown Bytes"));
        ALL_FIELDS.add(new IntByteField("Unknown Integer", -1));
        ALL_FIELDS.add(new IntByteField("Unknown Integer"));
        ALL_FIELDS.add(new IntByteField("Unknown Integer", -1));
        ALL_FIELDS.add(new IntByteField("Unknown Integer", -1));
        ALL_FIELDS.add(new IntByteField(EXTRA_RECORD_FLAGS));
        ALL_FIELDS.add(new IntByteField(INDX_RECORD, -1));
    }

    private ExthHeader exth;

    private ByteFieldMapSet fields;

    public MobiDocHeader() {
        fields = ALL_FIELDS.clone();
        exth = new ExthHeader(CHARSET);
    }

    public static MobiDocHeader parseBuffer(ByteBuffer in)
            throws InvalidHeaderException {

        MobiDocHeader header = new MobiDocHeader();
        header.parse(in);
        return header;

    }

    public void parse(ByteBuffer in) throws InvalidHeaderException {
        /*
         * Find the header identifier, throw if we don't find it
         */
        int offset = KmpSearch.indexOf(in.array(),
                MOBI.getBytes(PdbHeader.CHARSET));

        if (offset < 0)
            throw new InvalidHeaderException();

        in.position(offset - in.arrayOffset());
        in = in.slice();

        // Parse the length
        fields.parseBetween(in, 0, 8);
        int len = fields.<IntByteField> getAs(LENGTH).getValue();

        // And the rest
        fields.parseBetween(in, 8, len);
        
        try {
            System.out.println("Extracting ExthHeader...");
            exth = new ExthHeader(in, getEncoding().getCharset());
        } catch (InvalidHeaderException e) {
            e.printStackTrace();
        }
    }

    public ByteFieldMapSet getFields() { return fields; }

    public ExthHeader getExthHeader() { return exth; }

    public Encoding getEncoding() {
        return Encoding.valueOf(fields.<IntByteField> getAs(ENCODING)
                .getValue());
    }

    public int getExtendedFlags() {
        return fields.<IntByteField> getAs(EXTRA_RECORD_FLAGS).getValue();
    }
    
    public int getFcisRecord() {
        return fields.<IntByteField> getAs(FCIS_RECORD).getValue();
    }

    public short getFirstContentRecord() {
        return fields.<ShortByteField> getAs(FIRST_CONTENT_RECORD).getValue();
    }

    public int getFirstImageRecord() {
        return fields.<IntByteField> getAs(FIRST_IMAGE_RECORD).getValue();
    }

    public int getFirstNonBookRecord() {
        return fields.<IntByteField> getAs(FIRST_NON_BOOK_RECORD).getValue();
    }

    public int getFlisRecord() {
        return fields.<IntByteField> getAs(FLIS_RECORD).getValue();
    }

    public int getFullNameLength() {
        return fields.<IntByteField> getAs(FULL_NAME_LENGTH).getValue();
    }

    public int getFullNameOffset() {
        return fields.<IntByteField> getAs(FULL_NAME_OFFSET).getValue();
    }

    public int getHuffmanCount() {
        return fields.<IntByteField> getAs(HUFFMAN_RECORD_COUNT).getValue();
    }

    public int getHuffmanRecord() {
        return fields.<IntByteField> getAs(FIRST_HUFFMAN_RECORD).getValue();
    }

    public int getIndxRecord() {
        return fields.<IntByteField> getAs(INDX_RECORD).getValue();
    }

    public short getLastContentRecord() {
        return fields.<ShortByteField> getAs(LAST_CONTENT_RECORD).getValue();
    }
    
    public MobiType getType() {
        return MobiType.valueOf(fields.<IntByteField> getAs(MOBI_TYPE)
                .getValue());
    }

    public void setEncoding(Encoding e) {
        fields.<IntByteField> getAs(ENCODING).setValue(e.getValue());
    }

    public void setExtendedFlags(int i) {
        fields.<IntByteField> getAs(EXTRA_RECORD_FLAGS).setValue(i);
    }

    public void setExthHeader(boolean enable) {
        int value = enable ? 0x50 : 0x0;
        fields.<IntByteField> getAs(EXTH_FLAGS).setValue(value);
    }
    
    public boolean hasExthHeader() { 
        return (fields.<IntByteField> getAs(EXTH_FLAGS).getValue() & 0x50) != 0; 
    }

    public void setFcisRecord(int i) {
        fields.<IntByteField> getAs(FCIS_RECORD).setValue(i);
    }

    public void setFirstContentRecord(int i) {
        fields.<ShortByteField> getAs(FIRST_CONTENT_RECORD).setValue((short) i);
    }

    public void setFirstImageRecord(int i) {
        fields.<IntByteField> getAs(FIRST_IMAGE_RECORD).setValue(i);
    }

    public void setFirstNonBookRecord(int i) {
        fields.<IntByteField> getAs(FIRST_NON_BOOK_RECORD).setValue(i);
    }

    public void setFlisRecord(int i) {
        fields.<IntByteField> getAs(FLIS_RECORD).setValue(i);
    }

    public void setFullNameLength(int i) {
        fields.<IntByteField> getAs(FULL_NAME_LENGTH).setValue(i);
    }

    public void setFullNameOffset(int i) {
        fields.<IntByteField> getAs(FULL_NAME_OFFSET).setValue(i);
    }

    public void setHuffmanCount(int i) {
        fields.<IntByteField> getAs(HUFFMAN_RECORD_COUNT).setValue(i);
    }

    public void setHuffmanRecord(int i) {
        fields.<IntByteField> getAs(FIRST_HUFFMAN_RECORD).setValue(i);
    }

    public void setIndxRecord(int i) {
        fields.<IntByteField> getAs(INDX_RECORD).setValue(i);
    }

    public void setLastContentRecord(int i) {
        fields.<ShortByteField> getAs(LAST_CONTENT_RECORD).setValue((short) i);
    }

    public void setType(MobiType i) {
        fields.<IntByteField> getAs(MOBI_TYPE).setValue(i.getValue());
    }
    
    public int getLength() {
        int len = fields.length();
        
        if (hasExthHeader())
            len += exth.getLength();
        
        return len;
    }
    
    public void write(ByteBuffer out) {
        fields.<IntByteField> getAs(LENGTH).setValue(fields.length());
        fields.write(out);
        
        if (hasExthHeader())
            exth.write(out);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[::::MobiDocHeader::::]\n");
        sb.append(fields);
        return sb.toString();
    }
}
