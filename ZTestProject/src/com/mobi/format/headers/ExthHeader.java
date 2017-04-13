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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.mobi.StringUtil;
import com.mobi.format.headers.ExthHeader.ExthRecord;
import com.mobi.little.nj.adts.ByteFieldMapSet;
import com.mobi.little.nj.adts.IntByteField;
import com.mobi.little.nj.adts.StringByteField;
import com.mobi.little.nj.algorithms.KmpSearch;

public class ExthHeader implements Iterable<ExthRecord> {

    private static final int INT_SIZE = 4;

    private static final String EXTH = "EXTH";

    private static final String COUNT = "Count";

    private static final String LENGTH = "Length";

    private static final String IDENTIFIER = "Identifier";

    public static enum DataType {
        INT, STRING;
    }

    public static final ByteFieldMapSet ALL_FIELDS = new ByteFieldMapSet();

    public static final int AUTHOR = 100;

    public static final int BLURB = 103;

    public static final int COVER = 201;

    public static final int CREATOR_ID = 204;

    public static final int CREATOR_STRING = 108;

    public static final int FAKECOVER = 203;

    public static final int ISBN = 104;

    public static final int THUMB = 202;

    public static final int TITLE = 503;

    /**
     * Used by the {@link ExthRecord#getType()} function
     */
    public static final Map<Integer, DataType> ENCODE_MAP;

    static {
        ALL_FIELDS.add(new StringByteField(4, IDENTIFIER, CHARSET, EXTH));
        ALL_FIELDS.add(new IntByteField(LENGTH));
        ALL_FIELDS.add(new IntByteField(COUNT));

        ENCODE_MAP = new HashMap<Integer, DataType>();
        ENCODE_MAP.put(AUTHOR, DataType.STRING);
        ENCODE_MAP.put(BLURB, DataType.STRING);
        ENCODE_MAP.put(ISBN, DataType.STRING);
        ENCODE_MAP.put(CREATOR_STRING, DataType.STRING);
        ENCODE_MAP.put(TITLE, DataType.STRING);

        ENCODE_MAP.put(COVER, DataType.INT);
        ENCODE_MAP.put(THUMB, DataType.INT);
        ENCODE_MAP.put(FAKECOVER, DataType.INT);
        ENCODE_MAP.put(CREATOR_ID, DataType.INT);
    }

    private Charset charset;

    private ByteFieldMapSet fields;

    private Map<Integer, ExthRecord> records;

    public ExthHeader(Charset ch) {
        fields = ALL_FIELDS.clone();
        records = new HashMap<Integer, ExthRecord>();
        charset = ch;
    }

    public ExthHeader(ByteBuffer in, Charset ch) 
            throws InvalidHeaderException {
        
        this(ch);
        parse(in);
    }

    public void parse(ByteBuffer raw) throws InvalidHeaderException {
        int offset = KmpSearch.indexOf(raw.array(), EXTH.getBytes(CHARSET));
        
        if (offset < 0)
            throw new InvalidHeaderException();
        
        raw.position(offset - raw.arrayOffset());
        raw = raw.slice();

        fields.parseAll(raw);
        int count = fields.<IntByteField> getAs(COUNT).getValue();
        for (int i = 0; i < count; i++) {
            ExthRecord rec = new ExthRecord(raw);
            records.put(rec.id, rec);
        }
    }

    public ExthRecord getRecord(int id) {
        return records.get(id);
    }

    public void addRecord(int id, byte[] data) {
        records.put(id, new ExthRecord(id, data));
    }
    
    public void removeRecord(int id) {
        records.remove(id);
    }

    protected String getStringValue(int id) {
        ExthRecord rec = getRecord(id);

        return null != rec ? rec.asString() : StringUtil.EMPTY_STRING;
    }

    protected int getIntValue(int id) {
        ExthRecord rec = getRecord(id);

        if (null == rec)
            return -1;

        return rec.asInt();
    }

    protected void setRecord(int id, byte[] data) {
        ExthRecord rec = getRecord(id);

        if (rec == null)
            addRecord(id, data);
        else
            rec.setData(data);
    }

    protected void setRecord(int id, int value) {
        ExthRecord rec = getRecord(id);

        if (null == rec)
            addRecord(id, ByteBuffer.allocate(INT_SIZE).putInt(value).array());
        else
            rec.set(value);
    }

    public String getAuthor() {
        return getStringValue(AUTHOR);
    }

    public String getBlurb() {
        return getStringValue(BLURB);
    }

    public int getCover() {
        return getIntValue(COVER);
    }

    public int getThumb() {
        return getIntValue(THUMB);
    }

    public String getTitle() {
        return getStringValue(TITLE);
    }

    public void setAuthor(String s) {
        setRecord(AUTHOR, s.getBytes(charset));
    }

    public void setBlurb(String s) {
        setRecord(BLURB, s.getBytes(charset));
    }

    public void setCover(int i) {
        setRecord(COVER, i);
    }

    public void setThumb(int i) {
        setRecord(THUMB, i);
    }

    public void setTitle(String s) {
        setRecord(TITLE, s.getBytes(charset));
    }

    public int getCount() {
        return records.size();
    }
    
    public int getLength() {
        int rtn = fields.length();
        for (ExthRecord i : records.values())
            rtn += i.getLength();
        return rtn;
    }

    @Override
    public Iterator<ExthRecord> iterator() {
        return records.values().iterator();
    }

    public void write(ByteBuffer out) {
        fields.<IntByteField> getAs(LENGTH).setValue(getLength());
        fields.<IntByteField> getAs(COUNT).setValue(records.size());
        fields.write(out);
        for (ExthRecord i : records.values())
            out.put(i.getBuffer());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[::::EXTH Header:::]\n");
        sb.append(fields.toString() + "\n");
        
        Iterator<ExthRecord> it = iterator();
        while (it.hasNext())
            sb.append(it.next() + "\n");
        
        return sb.toString();
    }

    public class ExthRecord {

        private static final int STATIC_DATA = 8;

        private final int id;

        private byte[] data;

        private DataType type;

        public ExthRecord(ByteBuffer in) {
            id = in.getInt();
            int length = in.getInt();

            data = new byte[length - STATIC_DATA];
            in.get(data);
        }

        public ExthRecord(int id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        public int getId() {
            return id;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public int getLength() {
            return data.length + STATIC_DATA;
        }

        @Deprecated
        protected DataType getType() {
            if (null == type) {
                type = ENCODE_MAP.get(id);

                /*
                 * HACK: Unknown ID, let's take a look as a string
                 */
                if (null == type) {
                    type = DataType.STRING;
                }
            }
            return type;
        }

        public String asString() {
            return new String(data, charset);
        }

        public int asInt() {
            return ByteBuffer.wrap(data).getInt();
        }

        public void set(String in) {
            setData(in.getBytes(charset));
        }

        public void set(int in) {
            ByteBuffer.wrap(data).putInt(in);
        }

        public byte[] getBytes() {
            return getBuffer().array();
        }

        public ByteBuffer getBuffer() {
            int length = getLength();
            byte[] data = getData();
            return (ByteBuffer) ByteBuffer.allocate(length).putInt(id)
                    .putInt(length).put(data).rewind();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ID: " + id + ", Length: " + getData().length);
            sb.append(String.format(
                    "ID: %d, Length: %d%nAs Integer: %d%nAs String: %s",
                    id, getLength(), asInt(), asString()
                    ));
            return sb.toString();
        }
    }
}
