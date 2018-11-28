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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.lz77support.Parameters;
import org.apache.commons.compress.utils.ByteUtils;

/**
 * CompressorOutputStream for the framing Snappy format.
 *
 * <p>Based on the "spec" in the version "Last revised: 2013-10-25"</p>
 *
 * @see <a href="https://github.com/google/snappy/blob/master/framing_format.txt">Snappy framing format description</a>
 * @since 1.14
 * @NotThreadSafe
 */
public class FramedSnappyCompressorOutputStream extends CompressorOutputStream {
    // see spec:
    // > However, we place an additional restriction that the uncompressed data
    // > in a chunk must be no longer than 65536 bytes. This allows consumers to
    // > easily use small fixed-size buffers.
    private static final int MAX_COMPRESSED_BUFFER_SIZE = 1 << 16;

    private final OutputStream out;
    private final Parameters params;
    private final PureJavaCrc32C checksum = new PureJavaCrc32C();
    // used in one-arg write method
    private final byte[] oneByte = new byte[1];
    private final byte[] buffer = new byte[MAX_COMPRESSED_BUFFER_SIZE];
    private int currentIndex = 0;

    private final ByteUtils.ByteConsumer consumer;

    /**
     * Constructs a new output stream that compresses
     * snappy-framed-compressed data to the specified output stream.
     * @param out the OutputStream to which to write the compressed data
     * @throws IOException if writing the signature fails
     */
    public FramedSnappyCompressorOutputStream(final OutputStream out) throws IOException {
        this(out, SnappyCompressorOutputStream.createParameterBuilder(SnappyCompressorInputStream.DEFAULT_BLOCK_SIZE)
             .build());
    }

    /**
     * Constructs a new output stream that compresses
     * snappy-framed-compressed data to the specified output stream.
     * @param out the OutputStream to which to write the compressed data
     * @param params parameters used to fine-tune compression, in
     * particular to balance compression ratio vs compression speed.
     * @throws IOException if writing the signature fails
     */
    public FramedSnappyCompressorOutputStream(final OutputStream out, Parameters params) throws IOException {
        this.out = out;
        this.params = params;
        consumer = new ByteUtils.OutputStreamByteConsumer(out);
        out.write(FramedSnappyCompressorInputStream.SZ_SIGNATURE);
    }

    @Override
    public void write(int b) throws IOException {
        oneByte[0] = (byte) (b & 0xff);
        write(oneByte);
    }

    @Override
    public void write(byte[] data, int off, int len) throws IOException {
        if (currentIndex + len > MAX_COMPRESSED_BUFFER_SIZE) {
            flushBuffer();
            while (len > MAX_COMPRESSED_BUFFER_SIZE) {
                System.arraycopy(data, off, buffer, 0, MAX_COMPRESSED_BUFFER_SIZE);
                off += MAX_COMPRESSED_BUFFER_SIZE;
                len -= MAX_COMPRESSED_BUFFER_SIZE;
                currentIndex = MAX_COMPRESSED_BUFFER_SIZE;
                flushBuffer();
            }
        }
        System.arraycopy(data, off, buffer, currentIndex, len);
        currentIndex += len;
    }

    @Override
    public void close() throws IOException {
        try {
            finish();
        } finally {
            out.close();
        }
    }

    /**
     * Compresses all remaining data and writes it to the stream,
     * doesn't close the underlying stream.
     * @throws IOException if an error occurs
     */
    public void finish() throws IOException {
        if (currentIndex > 0) {
            flushBuffer();
        }
    }

    private void flushBuffer() throws IOException {
        out.write(FramedSnappyCompressorInputStream.COMPRESSED_CHUNK_TYPE);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream o = new SnappyCompressorOutputStream(baos, currentIndex, params)) {
            o.write(buffer, 0, currentIndex);
        }
        byte[] b = baos.toByteArray();
        writeLittleEndian(3, b.length + 4L /* CRC */);
        writeCrc();
        out.write(b);
        currentIndex = 0;
    }

    private void writeLittleEndian(final int numBytes, long num) throws IOException {
        ByteUtils.toLittleEndian(consumer, num, numBytes);
    }

    private void writeCrc() throws IOException {
        checksum.update(buffer, 0, currentIndex);
        writeLittleEndian(4, mask(checksum.getValue()));
        checksum.reset();
    }

    static long mask(long x) {
        // ugly, maybe we should just have used ints and deal with the
        // overflow
        x = ((x >> 15) | (x << 17));
        x += FramedSnappyCompressorInputStream.MASK_OFFSET;
        x &= 0xffffFFFFL;
        return x;
    }
}
