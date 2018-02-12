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
package org.apache.commons.compress.compressors.snappy;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.lz77support.AbstractLZ77CompressorInputStream;
import org.apache.commons.compress.utils.ByteUtils;

/**
 * CompressorInputStream for the raw Snappy format.
 *
 * <p>This implementation uses an internal buffer in order to handle
 * the back-references that are at the heart of the LZ77 algorithm.
 * The size of the buffer must be at least as big as the biggest
 * offset used in the compressed stream.  The current version of the
 * Snappy algorithm as defined by Google works on 32k blocks and
 * doesn't contain offsets bigger than 32k which is the default block
 * size used by this class.</p>
 *
 * @see <a href="https://github.com/google/snappy/blob/master/format_description.txt">Snappy compressed format description</a>
 * @since 1.7
 */
public class SnappyCompressorInputStream extends AbstractLZ77CompressorInputStream {

    /** Mask used to determine the type of "tag" is being processed */
    private static final int TAG_MASK = 0x03;

    /** Default block size */
    public static final int DEFAULT_BLOCK_SIZE = 32768;

    /** The size of the uncompressed data */
    private final int size;

    /** Number of uncompressed bytes still to be read. */
    private int uncompressedBytesRemaining;

    /** Current state of the stream */
    private State state = State.NO_BLOCK;

    private boolean endReached = false;

    /**
     * Constructor using the default buffer size of 32k.
     *
     * @param is
     *            An InputStream to read compressed data from
     *
     * @throws IOException if reading fails
     */
    public SnappyCompressorInputStream(final InputStream is) throws IOException {
        this(is, DEFAULT_BLOCK_SIZE);
    }

    /**
     * Constructor using a configurable buffer size.
     *
     * @param is
     *            An InputStream to read compressed data from
     * @param blockSize
     *            The block size used in compression
     *
     * @throws IOException if reading fails
     */
    public SnappyCompressorInputStream(final InputStream is, final int blockSize)
            throws IOException {
        super(is, blockSize);
        uncompressedBytesRemaining = size = (int) readSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (endReached) {
            return -1;
        }
        switch (state) {
        case NO_BLOCK:
            fill();
            return read(b, off, len);
        case IN_LITERAL:
            int litLen = readLiteral(b, off, len);
            if (!hasMoreDataInBlock()) {
                state = State.NO_BLOCK;
            }
            return litLen > 0 ? litLen : read(b, off, len);
        case IN_BACK_REFERENCE:
            int backReferenceLen = readBackReference(b, off, len);
            if (!hasMoreDataInBlock()) {
                state = State.NO_BLOCK;
            }
            return backReferenceLen > 0 ? backReferenceLen : read(b, off, len);
        default:
            throw new IOException("Unknown stream state " + state);
        }
    }

    /**
     * Try to fill the buffer with the next block of data.
     */
    private void fill() throws IOException {
        if (uncompressedBytesRemaining == 0) {
            endReached = true;
            return;
        }

        int b = readOneByte();
        if (b == -1) {
            throw new IOException("Premature end of stream reading block start");
        }
        int length = 0;
        int offset = 0;

        switch (b & TAG_MASK) {

        case 0x00:

            length = readLiteralLength(b);
            uncompressedBytesRemaining -= length;
            startLiteral(length);
            state = State.IN_LITERAL;
            break;

        case 0x01:

            /*
             * These elements can encode lengths between [4..11] bytes and
             * offsets between [0..2047] bytes. (len-4) occupies three bits
             * and is stored in bits [2..4] of the tag byte. The offset
             * occupies 11 bits, of which the upper three are stored in the
             * upper three bits ([5..7]) of the tag byte, and the lower
             * eight are stored in a byte following the tag byte.
             */

            length = 4 + ((b >> 2) & 0x07);
            uncompressedBytesRemaining -= length;
            offset = (b & 0xE0) << 3;
            b = readOneByte();
            if (b == -1) {
                throw new IOException("Premature end of stream reading back-reference length");
            }
            offset |= b;

            startBackReference(offset, length);
            state = State.IN_BACK_REFERENCE;
            break;

        case 0x02:

            /*
             * These elements can encode lengths between [1..64] and offsets
             * from [0..65535]. (len-1) occupies six bits and is stored in
             * the upper six bits ([2..7]) of the tag byte. The offset is
             * stored as a little-endian 16-bit integer in the two bytes
             * following the tag byte.
             */

            length = (b >> 2) + 1;
            uncompressedBytesRemaining -= length;

            offset = (int) ByteUtils.fromLittleEndian(supplier, 2);

            startBackReference(offset, length);
            state = State.IN_BACK_REFERENCE;
            break;

        case 0x03:

            /*
             * These are like the copies with 2-byte offsets (see previous
             * subsection), except that the offset is stored as a 32-bit
             * integer instead of a 16-bit integer (and thus will occupy
             * four bytes).
             */

            length = (b >> 2) + 1;
            uncompressedBytesRemaining -= length;

            offset = (int) ByteUtils.fromLittleEndian(supplier, 4) & 0x7fffffff;

            startBackReference(offset, length);
            state = State.IN_BACK_REFERENCE;
            break;
        default:
            // impossible as TAG_MASK is two bits and all four possible cases have been covered
            break;
        }
    }

    /*
     * For literals up to and including 60 bytes in length, the
     * upper six bits of the tag byte contain (len-1). The literal
     * follows immediately thereafter in the bytestream. - For
     * longer literals, the (len-1) value is stored after the tag
     * byte, little-endian. The upper six bits of the tag byte
     * describe how many bytes are used for the length; 60, 61, 62
     * or 63 for 1-4 bytes, respectively. The literal itself follows
     * after the length.
     */
    private int readLiteralLength(final int b) throws IOException {
        int length;
        switch (b >> 2) {
        case 60:
            length = readOneByte();
            if (length == -1) {
                throw new IOException("Premature end of stream reading literal length");
            }
            break;
        case 61:
            length = (int) ByteUtils.fromLittleEndian(supplier, 2);
            break;
        case 62:
            length = (int) ByteUtils.fromLittleEndian(supplier, 3);
            break;
        case 63:
            length = (int) ByteUtils.fromLittleEndian(supplier, 4);
            break;
        default:
            length = b >> 2;
            break;
        }

        return length + 1;
    }

    /**
     * The stream starts with the uncompressed length (up to a maximum of 2^32 -
     * 1), stored as a little-endian varint. Varints consist of a series of
     * bytes, where the lower 7 bits are data and the upper bit is set iff there
     * are more bytes to be read. In other words, an uncompressed length of 64
     * would be stored as 0x40, and an uncompressed length of 2097150 (0x1FFFFE)
     * would be stored as 0xFE 0xFF 0x7F.
     *
     * @return The size of the uncompressed data
     *
     * @throws IOException
     *             Could not read a byte
     */
    private long readSize() throws IOException {
        int index = 0;
        long sz = 0;
        int b = 0;

        do {
            b = readOneByte();
            if (b == -1) {
                throw new IOException("Premature end of stream reading size");
            }
            sz |= (b & 0x7f) << (index++ * 7);
        } while (0 != (b & 0x80));
        return sz;
    }

    /**
     * Get the uncompressed size of the stream
     *
     * @return the uncompressed size
     */
    @Override
    public int getSize() {
        return size;
    }

    private enum State {
        NO_BLOCK, IN_LITERAL, IN_BACK_REFERENCE
    }
}
