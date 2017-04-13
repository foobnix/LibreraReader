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
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TimeZone;

import com.mobi.ByteFieldContainer;
import com.mobi.little.nj.adts.ByteFieldMapSet;
import com.mobi.little.nj.adts.IntByteField;
import com.mobi.little.nj.adts.ShortByteField;
import com.mobi.little.nj.adts.StringByteField;

public class PdbHeader implements ByteFieldContainer {

    public static final String NEXT_RECORD_LIST_ID = "Next Record List ID";

    public static final String UNIQUE_SEED_ID = "Unique Seed ID";

    public static final String CREATOR = "Creator";

    public static final String TYPE = "Type";

    public static final String SORT_INFO_ID = "Sort Info ID";

    public static final String APP_INFO_ID = "App Info ID";

    public static final String MODIFICATION_NUMBER = "Modification Number";

    public static final String BACKEDUP_TIME = "Backedup Time";

    public static final String MODIFICATION_TIME = "Modification Time";

    public static final String CREATION_TIME = "Creation Time";

    public static final String VERSION = "Version";

    public static final String ATTRIBUTES = "Attributes";

    public static final String NAME = "Name";

    /**
     * A predefined, Cloneable set of fields
     * 
     * FIXME: This is not immutable
     */
    public static final ByteFieldMapSet ALL_FIELDS;

    /**
     * Defined as Charset.forName("US-ASCII")
     */
    public static final Charset CHARSET;

    /**
     * January 1, 1904 Etc/UTC
     * 
     * FIXME: This is not immutable
     */
    public static final Calendar EPOCH_MAC;

    /**
     * January 1, 1970 Etc/UTC
     * 
     * FIXME: This is not immutable
     */
    public static final Calendar EPOCH_NIX;

    /**
     * Length of the Name String field
     */
    public static final int LENGTH_NAME = 32;

    /**
     * TimeZone.getTimeZone("Etc/UTC")
     */
    public static final TimeZone TIMEZONE;

    static {
        CHARSET = Charset.forName("US-ASCII");

        TIMEZONE = TimeZone.getTimeZone("Etc/UTC");
        EPOCH_MAC = Calendar.getInstance(TIMEZONE);
        EPOCH_MAC.clear();
        EPOCH_MAC.set(1904, 0, 1);
        EPOCH_NIX = Calendar.getInstance(TIMEZONE);
        EPOCH_NIX.clear();
        EPOCH_NIX.set(1970, 0, 1);

        ALL_FIELDS = new ByteFieldMapSet();
        ALL_FIELDS.add(new StringByteField(LENGTH_NAME, NAME, CHARSET));
        ALL_FIELDS.add(new ShortByteField(ATTRIBUTES));
        ALL_FIELDS.add(new ShortByteField(VERSION));
        ALL_FIELDS.add(new IntByteField(CREATION_TIME, getPdbSeconds(
                Calendar.getInstance(), true)));
        ALL_FIELDS.add(new IntByteField(MODIFICATION_TIME, getPdbSeconds(
                Calendar.getInstance(), true)));
        ALL_FIELDS.add(new IntByteField(BACKEDUP_TIME));
        ALL_FIELDS.add(new IntByteField(MODIFICATION_NUMBER));
        ALL_FIELDS.add(new IntByteField(APP_INFO_ID));
        ALL_FIELDS.add(new IntByteField(SORT_INFO_ID));
        ALL_FIELDS.add(new StringByteField(4, TYPE, CHARSET));
        ALL_FIELDS.add(new StringByteField(4, CREATOR, CHARSET));
        ALL_FIELDS.add(new IntByteField(UNIQUE_SEED_ID));
        ALL_FIELDS.add(new IntByteField(NEXT_RECORD_LIST_ID));
    }

    private static Calendar getDate(int pdbsecs, boolean signed_date) {
        Calendar c = Calendar.getInstance();
        Calendar epoch = signed_date ? EPOCH_NIX : EPOCH_MAC;

        long time = pdbsecs * 1000L;
        c.setTimeInMillis(epoch.getTimeInMillis() + time);
        return c;
    }

    private static int getPdbSeconds(Calendar c, boolean signed_date) {
        long i = 0L;
        Calendar epoch = signed_date ? EPOCH_NIX : EPOCH_MAC;
        i = c.getTimeInMillis() - epoch.getTimeInMillis();
        i = i / 1000L;
        return (int) i;
    }

    private ByteFieldMapSet fields;

    private boolean signed_date;

    public PdbHeader() {
        fields = ALL_FIELDS.clone();
        signed_date = true;
    }

    public static PdbHeader parseBuffer(ByteBuffer raw) {
        PdbHeader header = new PdbHeader();
        header.parse(raw);
        return header;
    }

    public void parse(ByteBuffer raw) {
        fields.parseAll(raw);
        signed_date = fields.<IntByteField> getAs(CREATION_TIME).getValue() >>> 31 == 0;
    }

    @Override
    public ByteFieldMapSet getFields() {
        return fields;
    }

    public Calendar getBackedupTime() {
        return getDate(fields.<IntByteField> getAs(BACKEDUP_TIME).getValue(),
                signed_date);
    }

    public Calendar getCreationTime() {
        return getDate(fields.<IntByteField> getAs(CREATION_TIME).getValue(),
                signed_date);
    }

    public Calendar getModificationTime() {
        return getDate(fields.<IntByteField> getAs(MODIFICATION_TIME)
                .getValue(), signed_date);
    }

    public String getName() {
        return fields.<StringByteField> getAs(NAME).getValue().trim();
    }

    public void setBackedupTime(Calendar c) {
        int secs = getPdbSeconds(c, signed_date);
        fields.<IntByteField> getAs(BACKEDUP_TIME).setValue(secs);
    }

    public void setCreationTime(Calendar c) {
        int secs = getPdbSeconds(c, signed_date);
        fields.<IntByteField> getAs(CREATION_TIME).setValue(secs);
    }

    public void setModificationTime(Calendar c) {
        int secs = getPdbSeconds(c, signed_date);
        fields.<IntByteField> getAs(MODIFICATION_TIME).setValue(secs);
    }

    public void setName(String x) {
        fields.<StringByteField> getAs(NAME).setValue(x);
    }

    public void setBookType(String type) {
        fields.<StringByteField> getAs(TYPE).setValue(type);
    }

    public void setCreator(String creator) {
        fields.<StringByteField> getAs(CREATOR).setValue(creator);
    }

    public void write(ByteBuffer out) {
        setModificationTime(Calendar.getInstance());
        fields.write(out);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[::::PDB Header::::]\n");
        sb.append(fields);
        return sb.toString();
    }
}
