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
package com.mobi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.mobi.StreamUtil.InputAction;
import com.mobi.StreamUtil.OutputAction;
import com.mobi.format.headers.PdbHeader;
import com.mobi.format.headers.PdbToc;

public class PdbFile {

    private static FileUtil FUTIL = new FileUtil();

    private File file;

    private PdbHeader header;

    private PdbToc toc;

    public PdbFile() {
        header = new PdbHeader();
        toc = new PdbToc();
    }

    public PdbFile(File in) throws IOException {
        file = in;
        load();
    }

    public int getFileLength() {
        return getToc().getTotalLength();
    }

    public PdbHeader getHeader() {
        return header;
    }

    public int getRecordCount() {
        return getToc().getCount();
    }

    public PdbToc getToc() {
        return toc;
    }

    public void parse(ByteBuffer raw) {
        System.out.println("Extracting PdbHeader...");
        header = PdbHeader.parseBuffer(raw);
        toc = PdbToc.parseBuffer(raw);
    }

    public byte[] buf;

    public void load() throws IOException {
        buf = new byte[(int) file.length()];

        if (FUTIL.read(file, new InputAction() {

            @Override
            public void act(InputStream stream) throws IOException {
                stream.read(buf);
            }
        })) {
            parse(ByteBuffer.wrap(buf));
        } else
            throw FUTIL.getFirstException();
    }

    public boolean canSave() {
        return file != null;
    }

    public boolean writeToFile() {
        return writeToFile(file);
    }

    public boolean writeToFile(File out) {
        /*
         * Prepare the output buffer
         */
        getToc().refresh();
        final ByteBuffer bb = ByteBuffer.allocate(getFileLength());
        header.write(bb);
        toc.write(bb);
        /*
         * Write the file
         */
        if (FUTIL.write(out, new OutputAction() {

            @Override
            public void act(OutputStream stream) throws IOException {
                stream.write(bb.array());

            }
        })) {
            file = out;
            return true;
        }
        return false;
    }
}
