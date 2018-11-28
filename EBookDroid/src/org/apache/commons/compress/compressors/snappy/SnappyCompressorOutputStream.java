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
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.lz77support.LZ77Compressor;
import org.apache.commons.compress.compressors.lz77support.Parameters;
import org.apache.commons.compress.utils.ByteUtils;

/**
 * CompressorOutputStream for the raw Snappy format.
 *
 * <p>This implementation uses an internal buffer in order to handle
 * the back-references that are at the heart of the LZ77 algorithm.
 * The size of the buffer must be at least as big as the biggest
 * offset used in the compressed stream.  The current version of the
 * Snappy algorithm as defined by Google works on 32k blocks and
 * doesn't contain offsets bigger than 32k which is the default block
 * size used by this class.</p>
 *
 * <p>The raw Snappy format requires the uncompressed size to be
 * written at the beginning of the stream using a varint
 * representation, i.e. the number of bytes needed to write the
 * information is not known before the uncompressed size is
 * known. We've chosen to make the uncompressedSize a parameter of the
 * constructor in favor of buffering the whole output until the size
 * is known. When using the {@link FramedSnappyCompressorOutputStream}
 * this limitation is taken care of by the warpping framing
 * format.</p>
 *
 * @see <a href="https://github.com/google/snappy/blob/master/format_description.txt">Snappy compressed format description</a>
 * @since 1.14
 * @NotThreadSafe
 */
public class SnappyCompressorOutputStream extends CompressorOutputStream {
    private final LZ77Compressor compressor;
    private final OutputStream os;
    private final ByteUtils.ByteConsumer consumer;

    // used in one-arg write method
    private final byte[] oneByte = new byte[1];

    private boolean finished = false;

    /**
     * Constructor using the default block size of 32k.
     *
     * @param os the outputstream to write compressed data to
     * @param uncompressedSize the uncompressed size of data
     * @throws IOException if writing of the size fails
     */
    public SnappyCompressorOutputStream(final OutputStream os, final long uncompressedSize) throws IOException {
        this(os, uncompressedSize, SnappyCompressorInputStream.DEFAULT_BLOCK_SIZE);
    }

    /**
     * Constructor using a configurable block size.
     *
     * @param os the outputstream to write compressed data to
     * @param uncompressedSize the uncompressed size of data
     * @param blockSize the block size used - must be a power of two
     * @throws IOException if writing of the size fails
     */
    public SnappyCompressorOutputStream(final OutputStream os, final long uncompressedSize, final int blockSize)
        throws IOException {
        this(os, uncompressedSize, createParameterBuilder(blockSize).build());
    }

    /**
     * Constructor providing full control over the underlying LZ77 compressor.
     *
     * @param os the outputstream to write compressed data to
     * @param uncompressedSize the uncompressed size of data
     * @param params the parameters to use by the compressor - note
     * that the format itself imposes some limits like a maximum match
     * length of 64 bytes
     * @throws IOException if writing of the size fails
     */
    public SnappyCompressorOutputStream(final OutputStream os, final long uncompressedSize, Parameters params)
        throws IOException {
        this.os = os;
        consumer = new ByteUtils.OutputStreamByteConsumer(os);
        compressor = new LZ77Compressor(params, new LZ77Compressor.Callback() {
                @Override
                public void accept(LZ77Compressor.Block block) throws IOException {
                    switch (block.getType()) {
                    case LITERAL:
                        writeLiteralBlock((LZ77Compressor.LiteralBlock) block);
                        break;
                    case BACK_REFERENCE:
                        writeBackReference((LZ77Compressor.BackReference) block);
                        break;
                    case EOD:
                        break;
                    }
                }
            });
        writeUncompressedSize(uncompressedSize);
    }

    @Override
    public void write(int b) throws IOException {
        oneByte[0] = (byte) (b & 0xff);
        write(oneByte);
    }

    @Override
    public void write(byte[] data, int off, int len) throws IOException {
        compressor.compress(data, off, len);
    }

    @Override
    public void close() throws IOException {
        try {
            finish();
        } finally {
            os.close();
        }
    }

    /**
     * Compresses all remaining data and writes it to the stream,
     * doesn't close the underlying stream.
     * @throws IOException if an error occurs
     */
    public void finish() throws IOException {
        if (!finished) {
            compressor.finish();
            finished = true;
        }
    }

    private void writeUncompressedSize(long uncompressedSize) throws IOException {
        boolean more = false;
        do {
            int currentByte = (int) (uncompressedSize & 0x7F);
            more = uncompressedSize > currentByte;
            if (more) {
                currentByte |= 0x80;
            }
            os.write(currentByte);
            uncompressedSize >>= 7;
        } while (more);
    }

    // literal length is stored as (len - 1) either inside the tag
    // (six bits minus four flags) or in 1 to 4 bytes after the tag
    private static final int MAX_LITERAL_SIZE_WITHOUT_SIZE_BYTES = 60;
    private static final int MAX_LITERAL_SIZE_WITH_ONE_SIZE_BYTE = 1 << 8;
    private static final int MAX_LITERAL_SIZE_WITH_TWO_SIZE_BYTES = 1 << 16;
    private static final int MAX_LITERAL_SIZE_WITH_THREE_SIZE_BYTES = 1 << 24;

    private static final int ONE_SIZE_BYTE_MARKER = 60 << 2;
    private static final int TWO_SIZE_BYTE_MARKER = 61 << 2;
    private static final int THREE_SIZE_BYTE_MARKER = 62 << 2;
    private static final int FOUR_SIZE_BYTE_MARKER = 63 << 2;

    private void writeLiteralBlock(LZ77Compressor.LiteralBlock block) throws IOException {
        int len = block.getLength();
        if (len <= MAX_LITERAL_SIZE_WITHOUT_SIZE_BYTES) {
            writeLiteralBlockNoSizeBytes(block, len);
        } else if (len <= MAX_LITERAL_SIZE_WITH_ONE_SIZE_BYTE) {
            writeLiteralBlockOneSizeByte(block, len);
        } else if (len <= MAX_LITERAL_SIZE_WITH_TWO_SIZE_BYTES) {
            writeLiteralBlockTwoSizeBytes(block, len);
        } else if (len <= MAX_LITERAL_SIZE_WITH_THREE_SIZE_BYTES) {
            writeLiteralBlockThreeSizeBytes(block, len);
        } else {
            writeLiteralBlockFourSizeBytes(block, len);
        }
    }

    private void writeLiteralBlockNoSizeBytes(LZ77Compressor.LiteralBlock block, int len) throws IOException {
        writeLiteralBlockWithSize(len - 1 << 2, 0, len, block);
    }

    private void writeLiteralBlockOneSizeByte(LZ77Compressor.LiteralBlock block, int len) throws IOException {
        writeLiteralBlockWithSize(ONE_SIZE_BYTE_MARKER, 1, len, block);
    }

    private void writeLiteralBlockTwoSizeBytes(LZ77Compressor.LiteralBlock block, int len) throws IOException {
        writeLiteralBlockWithSize(TWO_SIZE_BYTE_MARKER, 2, len, block);
    }

    private void writeLiteralBlockThreeSizeBytes(LZ77Compressor.LiteralBlock block, int len) throws IOException {
        writeLiteralBlockWithSize(THREE_SIZE_BYTE_MARKER, 3, len, block);
    }

    private void writeLiteralBlockFourSizeBytes(LZ77Compressor.LiteralBlock block, int len) throws IOException {
        writeLiteralBlockWithSize(FOUR_SIZE_BYTE_MARKER, 4, len, block);
    }

    private void writeLiteralBlockWithSize(int tagByte, int sizeBytes, int len, LZ77Compressor.LiteralBlock block)
        throws IOException {
        os.write(tagByte);
        writeLittleEndian(sizeBytes, len - 1);
        os.write(block.getData(), block.getOffset(), len);
    }

    private void writeLittleEndian(final int numBytes, int num) throws IOException {
        ByteUtils.toLittleEndian(consumer, num, numBytes);
    }

    // Back-references ("copies") have their offset/size information
    // in two, three or five bytes.
    private static final int MIN_MATCH_LENGTH_WITH_ONE_OFFSET_BYTE = 4;
    private static final int MAX_MATCH_LENGTH_WITH_ONE_OFFSET_BYTE = 11;
    private static final int MAX_OFFSET_WITH_ONE_OFFSET_BYTE = 1 << 11 - 1;
    private static final int MAX_OFFSET_WITH_TWO_OFFSET_BYTES = 1 << 16 - 1;

    private static final int ONE_BYTE_COPY_TAG = 1;
    private static final int TWO_BYTE_COPY_TAG = 2;
    private static final int FOUR_BYTE_COPY_TAG = 3;

    private void writeBackReference(LZ77Compressor.BackReference block) throws IOException {
        final int len = block.getLength();
        final int offset = block.getOffset();
        if (len >= MIN_MATCH_LENGTH_WITH_ONE_OFFSET_BYTE && len <= MAX_MATCH_LENGTH_WITH_ONE_OFFSET_BYTE
            && offset <= MAX_OFFSET_WITH_ONE_OFFSET_BYTE) {
            writeBackReferenceWithOneOffsetByte(len, offset);
        } else if (offset < MAX_OFFSET_WITH_TWO_OFFSET_BYTES) {
            writeBackReferenceWithTwoOffsetBytes(len, offset);
        } else {
            writeBackReferenceWithFourOffsetBytes(len, offset);
        }
    }

    private void writeBackReferenceWithOneOffsetByte(int len, int offset) throws IOException {
        os.write(ONE_BYTE_COPY_TAG | ((len - 4) << 2) | ((offset & 0x700) >> 3));
        os.write(offset & 0xff);
    }

    private void writeBackReferenceWithTwoOffsetBytes(int len, int offset) throws IOException {
        writeBackReferenceWithLittleEndianOffset(TWO_BYTE_COPY_TAG, 2, len, offset);
    }

    private void writeBackReferenceWithFourOffsetBytes(int len, int offset) throws IOException {
        writeBackReferenceWithLittleEndianOffset(FOUR_BYTE_COPY_TAG, 4, len, offset);
    }

    private void writeBackReferenceWithLittleEndianOffset(int tag, int offsetBytes, int len, int offset)
        throws IOException {
        os.write(tag | ((len - 1) << 2));
        writeLittleEndian(offsetBytes, offset);
    }

    // technically the format could use shorter matches but with a
    // length of three the offset would be encoded as at least two
    // bytes in addition to the tag, so yield no compression at all
    private static final int MIN_MATCH_LENGTH = 4;
    // Snappy stores the match length in six bits of the tag
    private static final int MAX_MATCH_LENGTH = 64;

    /**
     * Returns a builder correctly configured for the Snappy algorithm using the gven block size.
     * @param blockSize the block size.
     * @return a builder correctly configured for the Snappy algorithm using the gven block size
     */
    public static Parameters.Builder createParameterBuilder(int blockSize) {
        // the max offset and max literal length defined by the format
        // are 2^32 - 1 and 2^32 respectively - with blockSize being
        // an integer we will never exceed that
        return Parameters.builder(blockSize)
            .withMinBackReferenceLength(MIN_MATCH_LENGTH)
            .withMaxBackReferenceLength(MAX_MATCH_LENGTH)
            .withMaxOffset(blockSize)
            .withMaxLiteralLength(blockSize);
    }
}
