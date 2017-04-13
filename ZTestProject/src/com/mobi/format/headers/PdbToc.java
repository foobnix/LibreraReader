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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.mobi.format.records.PdbRecord;

public class PdbToc implements Iterable<PdbRecord> {

    public static final int START_OFFSET = 76; // Length of the PDB Header

    private List<PdbRecord> records;

    private int total_length;

    public PdbToc() {
        records = new LinkedList<PdbRecord>();
    }

    public static PdbToc parseBuffer(ByteBuffer raw) {
        PdbToc toc = new PdbToc();
        toc.total_length = raw.capacity();
        toc.parse(raw);
        return toc;
    }

    private void parse(ByteBuffer raw) {
        raw.position(START_OFFSET);
        int count = raw.getShort();
        records = new ArrayList<PdbRecord>();
        for (int i = 0; i < count; ++i)
            records.add(PdbRecord.parseBuffer(raw));

        // Fill each one with it's data
        ListIterator<PdbRecord> it = records.listIterator(records.size());
        int last_offset = total_length;
        while (it.hasPrevious()) {
            PdbRecord item = it.previous();
            item.readData(raw, last_offset);
            last_offset = item.getOffset();
        }
    }

    public void clear() {
        records.clear();
        total_length = START_OFFSET + 4;
    }

    public short getCount() {
        return (short) records.size();
    }

    public int getTotalLength() {
        return total_length;
    }

    public List<PdbRecord> records() {
        return records;
    }

    @Override
    public ListIterator<PdbRecord> iterator() {
        return records.listIterator();
    }

    public ListIterator<PdbRecord> iterator(int i) {
        return records.listIterator(i);
    }

    public void refresh() {
        /*
         * Calculate the offset into the file for each record, starting from the
         * end of the PDB header; accounting for two bytes count, eight per
         * record and two padding
         */
        int offset = START_OFFSET + 4 + records.size() * 8;
        int j = 0;
        for (PdbRecord i : records) {
            i.setOffset(offset);

            offset += i.getLength();

            /*
             * HACK: If not set, make the record ID its ordinal FIXME: I guess
             * these should be unique, but we don't check for that
             */
            if (i.getID() == 0 && j > 0)
                i.setID(j);

            ++j;
        }

        // Update the length attribute
        total_length = offset;
    }

    public void write(ByteBuffer out) {
        refresh();

        out.position(START_OFFSET);

        // Place count
        out.putShort(getCount());
        for (PdbRecord i : records) {
            // Place the record TOC entry
            out.put(i.getTocBuffer());

            // Save position
            int pos = out.position();

            // Place the record data
            out.position(i.getOffset());
            out.put(i.getBuffer());

            // Return to pos for next record
            out.position(pos);
        }
        // Two bytes padding
        out.putShort((short) 0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[::::PDB Table of Contents::::]\n");
        sb.append("Records: " + records.size() + "\n");

        int j = 0;
        for (PdbRecord i : records)
            sb.append(String.format("%-3d. %s\n", j++, i.toString()));

        return sb.toString();
    }
}
