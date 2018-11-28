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
import java.io.PushbackInputStream;
import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * CompressorInputStream for the framing Snappy format.
 *
 * <p>Based on the "spec" in the version "Last revised: 2013-10-25"</p>
 *
 * @see <a href="https://github.com/google/snappy/blob/master/framing_format.txt">Snappy framing format description</a>
 * @since 1.7
 */
public class FramedSnappyCompressorInputStream extends CompressorInputStream
    implements InputStreamStatistics {

    /**
     * package private for tests only.
     */
    static final long MASK_OFFSET = 0xa282ead8L;

    private static final int STREAM_IDENTIFIER_TYPE = 0xff;
    static final int COMPRESSED_CHUNK_TYPE = 0;
    private static final int UNCOMPRESSED_CHUNK_TYPE = 1;
    private static final int PADDING_CHUNK_TYPE = 0xfe;
    private static final int MIN_UNSKIPPABLE_TYPE = 2;
    private static final int MAX_UNSKIPPABLE_TYPE = 0x7f;
    private static final int MAX_SKIPPABLE_TYPE = 0xfd;

    // used by FramedSnappyCompressorOutputStream as well
    static final byte[] SZ_SIGNATURE = new byte[] { //NOSONAR
        (byte) STREAM_IDENTIFIER_TYPE, // tag
        6, 0, 0, // length
        's', 'N', 'a', 'P', 'p', 'Y'
    };

    private long unreadBytes;
    private final CountingInputStream countingStream;

    /** The underlying stream to read compressed data from */
    private final PushbackInputStream in;

    /** The dialect to expect */
    private final FramedSnappyDialect dialect;

    private SnappyCompressorInputStream currentCompressedChunk;

    // used in no-arg read method
    private final byte[] oneByte = new byte[1];

    private boolean endReached, inUncompressedChunk;

    private int uncompressedBytesRemaining;
    private long expectedChecksum = -1;
    private final int blockSize;
    private final PureJavaCrc32C checksum = new PureJavaCrc32C();

    private final ByteUtils.ByteSupplier supplier = new ByteUtils.ByteSupplier() {
        @Override
        public int getAsByte() throws IOException {
            return readOneByte();
        }
    };

    /**
     * Constructs a new input stream that decompresses
     * snappy-framed-compressed data from the specified input stream
     * using the {@link FramedSnappyDialect#STANDARD} dialect.
     * @param in  the InputStream from which to read the compressed data
     * @throws IOException if reading fails
     */
    public FramedSnappyCompressorInputStream(final InputStream in) throws IOException {
        this(in, FramedSnappyDialect.STANDARD);
    }

    /**
     * Constructs a new input stream that decompresses snappy-framed-compressed data
     * from the specified input stream.
     * @param in  the InputStream from which to read the compressed data
     * @param dialect the dialect used by the compressed stream
     * @throws IOException if reading fails
     */
    public FramedSnappyCompressorInputStream(final InputStream in,
                                             final FramedSnappyDialect dialect)
        throws IOException {
        this(in, SnappyCompressorInputStream.DEFAULT_BLOCK_SIZE, dialect);
    }

    /**
     * Constructs a new input stream that decompresses snappy-framed-compressed data
     * from the specified input stream.
     * @param in  the InputStream from which to read the compressed data
     * @param blockSize the block size to use for the compressed stream
     * @param dialect the dialect used by the compressed stream
     * @throws IOException if reading fails
     * @since 1.14
     */
    public FramedSnappyCompressorInputStream(final InputStream in,
                                             final int blockSize,
                                             final FramedSnappyDialect dialect)
        throws IOException {
        countingStream = new CountingInputStream(in);
        this.in = new PushbackInputStream(countingStream, 1);
        this.blockSize = blockSize;
        this.dialect = dialect;
        if (dialect.hasStreamIdentifier()) {
            readStreamIdentifier();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return read(oneByte, 0, 1) == -1 ? -1 : oneByte[0] & 0xFF;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        try {
            if (currentCompressedChunk != null) {
                currentCompressedChunk.close();
                currentCompressedChunk = null;
            }
        } finally {
            in.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        int read = readOnce(b, off, len);
        if (read == -1) {
            readNextBlock();
            if (endReached) {
                return -1;
            }
            read = readOnce(b, off, len);
        }
        return read;
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
        if (inUncompressedChunk) {
            return Math.min(uncompressedBytesRemaining,
                            in.available());
        } else if (currentCompressedChunk != null) {
            return currentCompressedChunk.available();
        }
        return 0;
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return countingStream.getBytesRead() - unreadBytes;
    }

    /**
     * Read from the current chunk into the given array.
     *
     * @return -1 if there is no current chunk or the number of bytes
     * read from the current chunk (which may be -1 if the end of the
     * chunk is reached).
     */
    private int readOnce(final byte[] b, final int off, final int len) throws IOException {
        int read = -1;
        if (inUncompressedChunk) {
            final int amount = Math.min(uncompressedBytesRemaining, len);
            if (amount == 0) {
                return -1;
            }
            read = in.read(b, off, amount);
            if (read != -1) {
                uncompressedBytesRemaining -= read;
                count(read);
            }
        } else if (currentCompressedChunk != null) {
            final long before = currentCompressedChunk.getBytesRead();
            read = currentCompressedChunk.read(b, off, len);
            if (read == -1) {
                currentCompressedChunk.close();
                currentCompressedChunk = null;
            } else {
                count(currentCompressedChunk.getBytesRead() - before);
            }
        }
        if (read > 0) {
            checksum.update(b, off, read);
        }
        return read;
    }

    private void readNextBlock() throws IOException {
        verifyLastChecksumAndReset();
        inUncompressedChunk = false;
        final int type = readOneByte();
        if (type == -1) {
            endReached = true;
        } else if (type == STREAM_IDENTIFIER_TYPE) {
            in.unread(type);
            unreadBytes++;
            pushedBackBytes(1);
            readStreamIdentifier();
            readNextBlock();
        } else if (type == PADDING_CHUNK_TYPE
                   || (type > MAX_UNSKIPPABLE_TYPE && type <= MAX_SKIPPABLE_TYPE)) {
            skipBlock();
            readNextBlock();
        } else if (type >= MIN_UNSKIPPABLE_TYPE && type <= MAX_UNSKIPPABLE_TYPE) {
            throw new IOException("unskippable chunk with type " + type
                                  + " (hex " + Integer.toHexString(type) + ")"
                                  + " detected.");
        } else if (type == UNCOMPRESSED_CHUNK_TYPE) {
            inUncompressedChunk = true;
            uncompressedBytesRemaining = readSize() - 4 /* CRC */;
            expectedChecksum = unmask(readCrc());
        } else if (type == COMPRESSED_CHUNK_TYPE) {
            final boolean expectChecksum = dialect.usesChecksumWithCompressedChunks();
            final long size = readSize() - (expectChecksum ? 4L : 0L);
            if (expectChecksum) {
                expectedChecksum = unmask(readCrc());
            } else {
                expectedChecksum = -1;
            }
            currentCompressedChunk =
                new SnappyCompressorInputStream(new BoundedInputStream(in, size), blockSize);
            // constructor reads uncompressed size
            count(currentCompressedChunk.getBytesRead());
        } else {
            // impossible as all potential byte values have been covered
            throw new IOException("unknown chunk type " + type
                                  + " detected.");
        }
    }

    private long readCrc() throws IOException {
        final byte[] b = new byte[4];
        final int read = IOUtils.readFully(in, b);
        count(read);
        if (read != 4) {
            throw new IOException("premature end of stream");
        }
        return ByteUtils.fromLittleEndian(b);
    }

    static long unmask(long x) {
        // ugly, maybe we should just have used ints and deal with the
        // overflow
        x -= MASK_OFFSET;
        x &= 0xffffFFFFL;
        return ((x >> 17) | (x << 15)) & 0xffffFFFFL;
    }

    private int readSize() throws IOException {
        return (int) ByteUtils.fromLittleEndian(supplier, 3);
    }

    private void skipBlock() throws IOException {
        final int size = readSize();
        final long read = IOUtils.skip(in, size);
        count(read);
        if (read != size) {
            throw new IOException("premature end of stream");
        }
    }

    private void readStreamIdentifier() throws IOException {
        final byte[] b = new byte[10];
        final int read = IOUtils.readFully(in, b);
        count(read);
        if (10 != read || !matches(b, 10)) {
            throw new IOException("Not a framed Snappy stream");
        }
    }

    private int readOneByte() throws IOException {
        final int b = in.read();
        if (b != -1) {
            count(1);
            return b & 0xFF;
        }
        return -1;
    }

    private void verifyLastChecksumAndReset() throws IOException {
        if (expectedChecksum >= 0 && expectedChecksum != checksum.getValue()) {
            throw new IOException("Checksum verification failed");
        }
        expectedChecksum = -1;
        checksum.reset();
    }

    /**
     * Checks if the signature matches what is expected for a .sz file.
     *
     * <p>.sz files start with a chunk with tag 0xff and content sNaPpY.</p>
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return          true if this is a .sz stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {

        if (length < SZ_SIGNATURE.length) {
            return false;
        }

        byte[] shortenedSig = signature;
        if (signature.length > SZ_SIGNATURE.length) {
            shortenedSig = new byte[SZ_SIGNATURE.length];
            System.arraycopy(signature, 0, shortenedSig, 0, SZ_SIGNATURE.length);
        }

        return Arrays.equals(shortenedSig, SZ_SIGNATURE);
    }

}
