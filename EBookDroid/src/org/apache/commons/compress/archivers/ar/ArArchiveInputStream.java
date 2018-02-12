/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.ar;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Implements the "ar" archive format as an input stream.
 *
 * @NotThreadSafe
 *
 */
public class ArArchiveInputStream extends ArchiveInputStream {

    private final InputStream input;
    private long offset = 0;
    private boolean closed;

    /*
     * If getNextEnxtry has been called, the entry metadata is stored in
     * currentEntry.
     */
    private ArArchiveEntry currentEntry = null;

    // Storage area for extra long names (GNU ar)
    private byte[] namebuffer = null;

    /*
     * The offset where the current entry started. -1 if no entry has been
     * called
     */
    private long entryOffset = -1;

    // cached buffers - must only be used locally in the class (COMPRESS-172 - reduce garbage collection)
    private final byte[] nameBuf = new byte[16];
    private final byte[] lastModifiedBuf = new byte[12];
    private final byte[] idBuf = new byte[6];
    private final byte[] fileModeBuf = new byte[8];
    private final byte[] lengthBuf = new byte[10];

    /**
     * Constructs an Ar input stream with the referenced stream
     *
     * @param pInput
     *            the ar input stream
     */
    public ArArchiveInputStream(final InputStream pInput) {
        input = pInput;
        closed = false;
    }

    /**
     * Returns the next AR entry in this stream.
     *
     * @return the next AR entry.
     * @throws IOException
     *             if the entry could not be read
     */
    public ArArchiveEntry getNextArEntry() throws IOException {
        if (currentEntry != null) {
            final long entryEnd = entryOffset + currentEntry.getLength();
            IOUtils.skip(this, entryEnd - offset);
            currentEntry = null;
        }

        if (offset == 0) {
            final byte[] expected = ArchiveUtils.toAsciiBytes(ArArchiveEntry.HEADER);
            final byte[] realized = new byte[expected.length];
            final int read = IOUtils.readFully(this, realized);
            if (read != expected.length) {
                throw new IOException("failed to read header. Occured at byte: " + getBytesRead());
            }
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != realized[i]) {
                    throw new IOException("invalid header " + ArchiveUtils.toAsciiString(realized));
                }
            }
        }

        if (offset % 2 != 0 && read() < 0) {
            // hit eof
            return null;
        }

        if (input.available() == 0) {
            return null;
        }

        IOUtils.readFully(this, nameBuf);
        IOUtils.readFully(this, lastModifiedBuf);
        IOUtils.readFully(this, idBuf);
        final int userId = asInt(idBuf, true);
        IOUtils.readFully(this, idBuf);
        IOUtils.readFully(this, fileModeBuf);
        IOUtils.readFully(this, lengthBuf);

        {
            final byte[] expected = ArchiveUtils.toAsciiBytes(ArArchiveEntry.TRAILER);
            final byte[] realized = new byte[expected.length];
            final int read = IOUtils.readFully(this, realized);
            if (read != expected.length) {
                throw new IOException("failed to read entry trailer. Occured at byte: " + getBytesRead());
            }
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != realized[i]) {
                    throw new IOException("invalid entry trailer. not read the content? Occured at byte: " + getBytesRead());
                }
            }
        }

        entryOffset = offset;

//        GNU ar uses a '/' to mark the end of the filename; this allows for the use of spaces without the use of an extended filename.

        // entry name is stored as ASCII string
        String temp = ArchiveUtils.toAsciiString(nameBuf).trim();
        if (isGNUStringTable(temp)) { // GNU extended filenames entry
            currentEntry = readGNUStringTable(lengthBuf);
            return getNextArEntry();
        }

        long len = asLong(lengthBuf);
        if (temp.endsWith("/")) { // GNU terminator
            temp = temp.substring(0, temp.length() - 1);
        } else if (isGNULongName(temp)) {
            final int off = Integer.parseInt(temp.substring(1));// get the offset
            temp = getExtendedName(off); // convert to the long name
        } else if (isBSDLongName(temp)) {
            temp = getBSDLongName(temp);
            // entry length contained the length of the file name in
            // addition to the real length of the entry.
            // assume file name was ASCII, there is no "standard" otherwise
            final int nameLen = temp.length();
            len -= nameLen;
            entryOffset += nameLen;
        }

        currentEntry = new ArArchiveEntry(temp, len, userId,
                                          asInt(idBuf, true),
                                          asInt(fileModeBuf, 8),
                                          asLong(lastModifiedBuf));
        return currentEntry;
    }

    /**
     * Get an extended name from the GNU extended name buffer.
     *
     * @param offset pointer to entry within the buffer
     * @return the extended file name; without trailing "/" if present.
     * @throws IOException if name not found or buffer not set up
     */
    private String getExtendedName(final int offset) throws IOException {
        if (namebuffer == null) {
            throw new IOException("Cannot process GNU long filename as no // record was found");
        }
        for (int i = offset; i < namebuffer.length; i++) {
            if (namebuffer[i] == '\012' || namebuffer[i] == 0) {
                if (namebuffer[i - 1] == '/') {
                    i--; // drop trailing /
                }
                return ArchiveUtils.toAsciiString(namebuffer, offset, i - offset);
            }
        }
        throw new IOException("Failed to read entry: " + offset);
    }

    private long asLong(final byte[] byteArray) {
        return Long.parseLong(ArchiveUtils.toAsciiString(byteArray).trim());
    }

    private int asInt(final byte[] byteArray) {
        return asInt(byteArray, 10, false);
    }

    private int asInt(final byte[] byteArray, final boolean treatBlankAsZero) {
        return asInt(byteArray, 10, treatBlankAsZero);
    }

    private int asInt(final byte[] byteArray, final int base) {
        return asInt(byteArray, base, false);
    }

    private int asInt(final byte[] byteArray, final int base, final boolean treatBlankAsZero) {
        final String string = ArchiveUtils.toAsciiString(byteArray).trim();
        if (string.length() == 0 && treatBlankAsZero) {
            return 0;
        }
        return Integer.parseInt(string, base);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.compress.archivers.ArchiveInputStream#getNextEntry()
     */
    @Override
    public ArchiveEntry getNextEntry() throws IOException {
        return getNextArEntry();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            input.close();
        }
        currentEntry = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        int toRead = len;
        if (currentEntry != null) {
            final long entryEnd = entryOffset + currentEntry.getLength();
            if (len > 0 && entryEnd > offset) {
                toRead = (int) Math.min(len, entryEnd - offset);
            } else {
                return -1;
            }
        }
        final int ret = this.input.read(b, off, toRead);
        count(ret);
        offset += ret > 0 ? ret : 0;
        return ret;
    }

    /**
     * Checks if the signature matches ASCII "!&lt;arch&gt;" followed by a single LF
     * control character
     *
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this stream is an Ar archive stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        // 3c21 7261 6863 0a3e

        return length >= 8 && signature[0] == 0x21 &&
                signature[1] == 0x3c && signature[2] == 0x61 &&
                signature[3] == 0x72 && signature[4] == 0x63 &&
                signature[5] == 0x68 && signature[6] == 0x3e &&
                signature[7] == 0x0a;
    }

    static final String BSD_LONGNAME_PREFIX = "#1/";
    private static final int BSD_LONGNAME_PREFIX_LEN =
        BSD_LONGNAME_PREFIX.length();
    private static final String BSD_LONGNAME_PATTERN =
        "^" + BSD_LONGNAME_PREFIX + "\\d+";

    /**
     * Does the name look like it is a long name (or a name containing
     * spaces) as encoded by BSD ar?
     *
     * <p>From the FreeBSD ar(5) man page:</p>
     * <pre>
     * BSD   In the BSD variant, names that are shorter than 16
     *       characters and without embedded spaces are stored
     *       directly in this field.  If a name has an embedded
     *       space, or if it is longer than 16 characters, then
     *       the string "#1/" followed by the decimal represen-
     *       tation of the length of the file name is placed in
     *       this field. The actual file name is stored immedi-
     *       ately after the archive header.  The content of the
     *       archive member follows the file name.  The ar_size
     *       field of the header (see below) will then hold the
     *       sum of the size of the file name and the size of
     *       the member.
     * </pre>
     *
     * @since 1.3
     */
    private static boolean isBSDLongName(final String name) {
        return name != null && name.matches(BSD_LONGNAME_PATTERN);
    }

    /**
     * Reads the real name from the current stream assuming the very
     * first bytes to be read are the real file name.
     *
     * @see #isBSDLongName
     *
     * @since 1.3
     */
    private String getBSDLongName(final String bsdLongName) throws IOException {
        final int nameLen =
            Integer.parseInt(bsdLongName.substring(BSD_LONGNAME_PREFIX_LEN));
        final byte[] name = new byte[nameLen];
        final int read = IOUtils.readFully(this, name);
        if (read != nameLen) {
            throw new EOFException();
        }
        return ArchiveUtils.toAsciiString(name);
    }

    private static final String GNU_STRING_TABLE_NAME = "//";

    /**
     * Is this the name of the "Archive String Table" as used by
     * SVR4/GNU to store long file names?
     *
     * <p>GNU ar stores multiple extended filenames in the data section
     * of a file with the name "//", this record is referred to by
     * future headers.</p>
     *
     * <p>A header references an extended filename by storing a "/"
     * followed by a decimal offset to the start of the filename in
     * the extended filename data section.</p>
     *
     * <p>The format of the "//" file itself is simply a list of the
     * long filenames, each separated by one or more LF
     * characters. Note that the decimal offsets are number of
     * characters, not line or string number within the "//" file.</p>
     */
    private static boolean isGNUStringTable(final String name) {
        return GNU_STRING_TABLE_NAME.equals(name);
    }

    /**
     * Reads the GNU archive String Table.
     *
     * @see #isGNUStringTable
     */
    private ArArchiveEntry readGNUStringTable(final byte[] length) throws IOException {
        final int bufflen = asInt(length); // Assume length will fit in an int
        namebuffer = new byte[bufflen];
        final int read = IOUtils.readFully(this, namebuffer, 0, bufflen);
        if (read != bufflen){
            throw new IOException("Failed to read complete // record: expected="
                                  + bufflen + " read=" + read);
        }
        return new ArArchiveEntry(GNU_STRING_TABLE_NAME, bufflen);
    }

    private static final String GNU_LONGNAME_PATTERN = "^/\\d+";

    /**
     * Does the name look like it is a long name (or a name containing
     * spaces) as encoded by SVR4/GNU ar?
     *
     * @see #isGNUStringTable
     */
    private boolean isGNULongName(final String name) {
        return name != null && name.matches(GNU_LONGNAME_PATTERN);
    }
}
