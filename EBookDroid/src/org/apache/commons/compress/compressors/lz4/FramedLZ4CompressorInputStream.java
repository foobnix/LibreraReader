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
package org.apache.commons.compress.compressors.lz4;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.ChecksumCalculatingInputStream;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * CompressorInputStream for the LZ4 frame format.
 *
 * <p>Based on the "spec" in the version "1.5.1 (31/03/2015)"</p>
 *
 * @see <a href="http://lz4.github.io/lz4/lz4_Frame_format.html">LZ4 Frame Format Description</a>
 * @since 1.14
 * @NotThreadSafe
 */
public class FramedLZ4CompressorInputStream extends CompressorInputStream
    implements InputStreamStatistics {

    // used by FramedLZ4CompressorOutputStream as well
    static final byte[] LZ4_SIGNATURE = new byte[] { //NOSONAR
        4, 0x22, 0x4d, 0x18
    };
    private static final byte[] SKIPPABLE_FRAME_TRAILER = new byte[] {
        0x2a, 0x4d, 0x18
    };
    private static final byte SKIPPABLE_FRAME_PREFIX_BYTE_MASK = 0x50;

    static final int VERSION_MASK = 0xC0;
    static final int SUPPORTED_VERSION = 0x40;
    static final int BLOCK_INDEPENDENCE_MASK = 0x20;
    static final int BLOCK_CHECKSUM_MASK = 0x10;
    static final int CONTENT_SIZE_MASK = 0x08;
    static final int CONTENT_CHECKSUM_MASK = 0x04;
    static final int BLOCK_MAX_SIZE_MASK = 0x70;
    static final int UNCOMPRESSED_FLAG_MASK = 0x80000000;

    // used in no-arg read method
    private final byte[] oneByte = new byte[1];

    private final ByteUtils.ByteSupplier supplier = new ByteUtils.ByteSupplier() {
        @Override
        public int getAsByte() throws IOException {
            return readOneByte();
        }
    };

    private final CountingInputStream in;
    private final boolean decompressConcatenated;

    private boolean expectBlockChecksum;
    private boolean expectBlockDependency;
    private boolean expectContentSize;
    private boolean expectContentChecksum;

    private InputStream currentBlock;
    private boolean endReached, inUncompressed;

    // used for frame header checksum and content checksum, if present
    private final XXHash32 contentHash = new XXHash32();

    // used for block checksum, if present
    private final XXHash32 blockHash = new XXHash32();

    // only created if the frame doesn't set the block independence flag
    private byte[] blockDependencyBuffer;

    /**
     * Creates a new input stream that decompresses streams compressed
     * using the LZ4 frame format and stops after decompressing the
     * first frame.
     * @param in  the InputStream from which to read the compressed data
     * @throws IOException if reading fails
     */
    public FramedLZ4CompressorInputStream(InputStream in) throws IOException {
        this(in, false);
    }

    /**
     * Creates a new input stream that decompresses streams compressed
     * using the LZ4 frame format.
     * @param in  the InputStream from which to read the compressed data
     * @param decompressConcatenated if true, decompress until the end
     *          of the input; if false, stop after the first LZ4 frame
     *          and leave the input position to point to the next byte
     *          after the frame stream
     * @throws IOException if reading fails
     */
    public FramedLZ4CompressorInputStream(InputStream in, boolean decompressConcatenated) throws IOException {
        this.in = new CountingInputStream(in);
        this.decompressConcatenated = decompressConcatenated;
        init(true);
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
            if (currentBlock != null) {
                currentBlock.close();
                currentBlock = null;
            }
        } finally {
            in.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (endReached) {
            return -1;
        }
        int r = readOnce(b, off, len);
        if (r == -1) {
            nextBlock();
            if (!endReached) {
                r = readOnce(b, off, len);
            }
        }
        if (r != -1) {
            if (expectBlockDependency) {
                appendToBlockDependencyBuffer(b, off, r);
            }
            if (expectContentChecksum) {
                contentHash.update(b, off, r);
            }
        }
        return r;
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return in.getBytesRead();
    }

    private void init(boolean firstFrame) throws IOException {
        if (readSignature(firstFrame)) {
            readFrameDescriptor();
            nextBlock();
        }
    }

    private boolean readSignature(boolean firstFrame) throws IOException {
        String garbageMessage = firstFrame ? "Not a LZ4 frame stream" : "LZ4 frame stream followed by garbage";
        final byte[] b = new byte[4];
        int read = IOUtils.readFully(in, b);
        count(read);
        if (0 == read && !firstFrame) {
            // good LZ4 frame and nothing after it
            endReached = true;
            return false;
        }
        if (4 != read) {
            throw new IOException(garbageMessage);
        }

        read = skipSkippableFrame(b);
        if (0 == read && !firstFrame) {
            // good LZ4 frame with only some skippable frames after it
            endReached = true;
            return false;
        }
        if (4 != read || !matches(b, 4)) {
            throw new IOException(garbageMessage);
        }
        return true;
    }

    private void readFrameDescriptor() throws IOException {
        int flags = readOneByte();
        if (flags == -1) {
            throw new IOException("Premature end of stream while reading frame flags");
        }
        contentHash.update(flags);
        if ((flags & VERSION_MASK) != SUPPORTED_VERSION) {
            throw new IOException("Unsupported version " + (flags >> 6));
        }
        expectBlockDependency = (flags & BLOCK_INDEPENDENCE_MASK) == 0;
        if (expectBlockDependency) {
            if (blockDependencyBuffer == null) {
                blockDependencyBuffer = new byte[BlockLZ4CompressorInputStream.WINDOW_SIZE];
            }
        } else {
            blockDependencyBuffer = null;
        }
        expectBlockChecksum = (flags & BLOCK_CHECKSUM_MASK) != 0;
        expectContentSize = (flags & CONTENT_SIZE_MASK) != 0;
        expectContentChecksum = (flags & CONTENT_CHECKSUM_MASK) != 0;
        int bdByte = readOneByte();
        if (bdByte == -1) { // max size is irrelevant for this implementation
            throw new IOException("Premature end of stream while reading frame BD byte");
        }
        contentHash.update(bdByte);
        if (expectContentSize) { // for now we don't care, contains the uncompressed size
            byte[] contentSize = new byte[8];
            int skipped = IOUtils.readFully(in, contentSize);
            count(skipped);
            if (8 != skipped) {
                throw new IOException("Premature end of stream while reading content size");
            }
            contentHash.update(contentSize, 0, contentSize.length);
        }
        int headerHash = readOneByte();
        if (headerHash == -1) { // partial hash of header.
            throw new IOException("Premature end of stream while reading frame header checksum");
        }
        int expectedHash = (int) ((contentHash.getValue() >> 8) & 0xff);
        contentHash.reset();
        if (headerHash != expectedHash) {
            throw new IOException("frame header checksum mismatch.");
        }
    }

    private void nextBlock() throws IOException {
        maybeFinishCurrentBlock();
        long len = ByteUtils.fromLittleEndian(supplier, 4);
        boolean uncompressed = (len & UNCOMPRESSED_FLAG_MASK) != 0;
        int realLen = (int) (len & (~UNCOMPRESSED_FLAG_MASK));
        if (realLen == 0) {
            verifyContentChecksum();
            if (!decompressConcatenated) {
                endReached = true;
            } else {
                init(false);
            }
            return;
        }
        InputStream capped = new BoundedInputStream(in, realLen);
        if (expectBlockChecksum) {
            capped = new ChecksumCalculatingInputStream(blockHash, capped);
        }
        if (uncompressed) {
            inUncompressed = true;
            currentBlock = capped;
        } else {
            inUncompressed = false;
            BlockLZ4CompressorInputStream s = new BlockLZ4CompressorInputStream(capped);
            if (expectBlockDependency) {
                s.prefill(blockDependencyBuffer);
            }
            currentBlock = s;
        }
    }

    private void maybeFinishCurrentBlock() throws IOException {
        if (currentBlock != null) {
            currentBlock.close();
            currentBlock = null;
            if (expectBlockChecksum) {
                verifyChecksum(blockHash, "block");
                blockHash.reset();
            }
        }
    }

    private void verifyContentChecksum() throws IOException {
        if (expectContentChecksum) {
            verifyChecksum(contentHash, "content");
        }
        contentHash.reset();
    }

    private void verifyChecksum(XXHash32 hash, String kind) throws IOException {
        byte[] checksum = new byte[4];
        int read = IOUtils.readFully(in, checksum);
        count(read);
        if (4 != read) {
            throw new IOException("Premature end of stream while reading " + kind + " checksum");
        }
        long expectedHash = hash.getValue();
        if (expectedHash != ByteUtils.fromLittleEndian(checksum)) {
            throw new IOException(kind + " checksum mismatch.");
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

    private int readOnce(byte[] b, int off, int len) throws IOException {
        if (inUncompressed) {
            int cnt = currentBlock.read(b, off, len);
            count(cnt);
            return cnt;
        }
        BlockLZ4CompressorInputStream l = (BlockLZ4CompressorInputStream) currentBlock;
        long before = l.getBytesRead();
        int cnt = currentBlock.read(b, off, len);
        count(l.getBytesRead() - before);
        return cnt;
    }

    private static boolean isSkippableFrameSignature(byte[] b) {
        if ((b[0] & SKIPPABLE_FRAME_PREFIX_BYTE_MASK) != SKIPPABLE_FRAME_PREFIX_BYTE_MASK) {
            return false;
        }
        for (int i = 1; i < 4; i++) {
            if (b[i] != SKIPPABLE_FRAME_TRAILER[i - 1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Skips over the contents of a skippable frame as well as
     * skippable frames following it.
     *
     * <p>It then tries to read four more bytes which are supposed to
     * hold an LZ4 signature and returns the number of bytes read
     * while storing the bytes in the given array.</p>
     */
    private int skipSkippableFrame(byte[] b) throws IOException {
        int read = 4;
        while (read == 4 && isSkippableFrameSignature(b)) {
            long len = ByteUtils.fromLittleEndian(supplier, 4);
            long skipped = IOUtils.skip(in, len);
            count(skipped);
            if (len != skipped) {
                throw new IOException("Premature end of stream while skipping frame");
            }
            read = IOUtils.readFully(in, b);
            count(read);
        }
        return read;
    }

    private void appendToBlockDependencyBuffer(final byte[] b, final int off, int len) {
        len = Math.min(len, blockDependencyBuffer.length);
        if (len > 0) {
            int keep = blockDependencyBuffer.length - len;
            if (keep > 0) {
                // move last keep bytes towards the start of the buffer
                System.arraycopy(blockDependencyBuffer, len, blockDependencyBuffer, 0, keep);
            }
            // append new data
            System.arraycopy(b, off, blockDependencyBuffer, keep, len);
        }
    }

    /**
     * Checks if the signature matches what is expected for a .lz4 file.
     *
     * <p>.lz4 files start with a four byte signature.</p>
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return          true if this is a .sz stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {

        if (length < LZ4_SIGNATURE.length) {
            return false;
        }

        byte[] shortenedSig = signature;
        if (signature.length > LZ4_SIGNATURE.length) {
            shortenedSig = new byte[LZ4_SIGNATURE.length];
            System.arraycopy(signature, 0, shortenedSig, 0, LZ4_SIGNATURE.length);
        }

        return Arrays.equals(shortenedSig, LZ4_SIGNATURE);
    }
}
