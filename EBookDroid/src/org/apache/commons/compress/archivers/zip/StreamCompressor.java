/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.zip;

import org.apache.commons.compress.parallel.ScatterGatherBackingStore;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

/**
 * Encapsulates a {@link Deflater} and crc calculator, handling multiple types of output streams.
 * Currently {@link java.util.zip.ZipEntry#DEFLATED} and {@link java.util.zip.ZipEntry#STORED} are the only
 * supported compression methods.
 *
 * @since 1.10
 */
public abstract class StreamCompressor implements Closeable {

    /*
     * Apparently Deflater.setInput gets slowed down a lot on Sun JVMs
     * when it gets handed a really big buffer.  See
     * https://issues.apache.org/bugzilla/show_bug.cgi?id=45396
     *
     * Using a buffer size of 8 kB proved to be a good compromise
     */
    private static final int DEFLATER_BLOCK_SIZE = 8192;

    private final Deflater def;

    private final CRC32 crc = new CRC32();

    private long writtenToOutputStreamForLastEntry = 0;
    private long sourcePayloadLength = 0;
    private long totalWrittenToOutputStream = 0;

    private static final int BUFFER_SIZE = 4096;
    private final byte[] outputBuffer = new byte[BUFFER_SIZE];
    private final byte[] readerBuf = new byte[BUFFER_SIZE];

    StreamCompressor(final Deflater deflater) {
        this.def = deflater;
    }

    /**
     * Create a stream compressor with the given compression level.
     *
     * @param os       The stream to receive output
     * @param deflater The deflater to use
     * @return A stream compressor
     */
    static StreamCompressor create(final OutputStream os, final Deflater deflater) {
        return new OutputStreamCompressor(deflater, os);
    }

    /**
     * Create a stream compressor with the default compression level.
     *
     * @param os The stream to receive output
     * @return A stream compressor
     */
    static StreamCompressor create(final OutputStream os) {
        return create(os, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
    }

    /**
     * Create a stream compressor with the given compression level.
     *
     * @param os       The DataOutput to receive output
     * @param deflater The deflater to use for the compressor
     * @return A stream compressor
     */
    static StreamCompressor create(final DataOutput os, final Deflater deflater) {
        return new DataOutputCompressor(deflater, os);
    }

    /**
     * Create a stream compressor with the given compression level.
     *
     * @param os       The SeekableByteChannel to receive output
     * @param deflater The deflater to use for the compressor
     * @return A stream compressor
     * @since 1.13
     */
    static StreamCompressor create(final SeekableByteChannel os, final Deflater deflater) {
        return new SeekableByteChannelCompressor(deflater, os);
    }

    /**
     * Create a stream compressor with the given compression level.
     *
     * @param compressionLevel The {@link Deflater}  compression level
     * @param bs               The ScatterGatherBackingStore to receive output
     * @return A stream compressor
     */
    public static StreamCompressor create(final int compressionLevel, final ScatterGatherBackingStore bs) {
        final Deflater deflater = new Deflater(compressionLevel, true);
        return new ScatterGatherBackingStoreCompressor(deflater, bs);
    }

    /**
     * Create a stream compressor with the default compression level.
     *
     * @param bs The ScatterGatherBackingStore to receive output
     * @return A stream compressor
     */
    public static StreamCompressor create(final ScatterGatherBackingStore bs) {
        return create(Deflater.DEFAULT_COMPRESSION, bs);
    }

    /**
     * The crc32 of the last deflated file
     *
     * @return the crc32
     */

    public long getCrc32() {
        return crc.getValue();
    }

    /**
     * Return the number of bytes read from the source stream
     *
     * @return The number of bytes read, never negative
     */
    public long getBytesRead() {
        return sourcePayloadLength;
    }

    /**
     * The number of bytes written to the output for the last entry
     *
     * @return The number of bytes, never negative
     */
    public long getBytesWrittenForLastEntry() {
        return writtenToOutputStreamForLastEntry;
    }

    /**
     * The total number of bytes written to the output for all files
     *
     * @return The number of bytes, never negative
     */
    public long getTotalBytesWritten() {
        return totalWrittenToOutputStream;
    }


    /**
     * Deflate the given source using the supplied compression method
     *
     * @param source The source to compress
     * @param method The #ZipArchiveEntry compression method
     * @throws IOException When failures happen
     */

    public void deflate(final InputStream source, final int method) throws IOException {
        reset();
        int length;

        while ((length = source.read(readerBuf, 0, readerBuf.length)) >= 0) {
            write(readerBuf, 0, length, method);
        }
        if (method == ZipEntry.DEFLATED) {
            flushDeflater();
        }
    }

    /**
     * Writes bytes to ZIP entry.
     *
     * @param b      the byte array to write
     * @param offset the start position to write from
     * @param length the number of bytes to write
     * @param method the comrpession method to use
     * @return the number of bytes written to the stream this time
     * @throws IOException on error
     */
    long write(final byte[] b, final int offset, final int length, final int method) throws IOException {
        final long current = writtenToOutputStreamForLastEntry;
        crc.update(b, offset, length);
        if (method == ZipEntry.DEFLATED) {
            writeDeflated(b, offset, length);
        } else {
            writeCounted(b, offset, length);
        }
        sourcePayloadLength += length;
        return writtenToOutputStreamForLastEntry - current;
    }


    void reset() {
        crc.reset();
        def.reset();
        sourcePayloadLength = 0;
        writtenToOutputStreamForLastEntry = 0;
    }

    @Override
    public void close() throws IOException {
        def.end();
    }

    void flushDeflater() throws IOException {
        def.finish();
        while (!def.finished()) {
            deflate();
        }
    }

    private void writeDeflated(final byte[] b, final int offset, final int length)
            throws IOException {
        if (length > 0 && !def.finished()) {
            if (length <= DEFLATER_BLOCK_SIZE) {
                def.setInput(b, offset, length);
                deflateUntilInputIsNeeded();
            } else {
                final int fullblocks = length / DEFLATER_BLOCK_SIZE;
                for (int i = 0; i < fullblocks; i++) {
                    def.setInput(b, offset + i * DEFLATER_BLOCK_SIZE,
                            DEFLATER_BLOCK_SIZE);
                    deflateUntilInputIsNeeded();
                }
                final int done = fullblocks * DEFLATER_BLOCK_SIZE;
                if (done < length) {
                    def.setInput(b, offset + done, length - done);
                    deflateUntilInputIsNeeded();
                }
            }
        }
    }

    private void deflateUntilInputIsNeeded() throws IOException {
        while (!def.needsInput()) {
            deflate();
        }
    }

    void deflate() throws IOException {
        final int len = def.deflate(outputBuffer, 0, outputBuffer.length);
        if (len > 0) {
            writeCounted(outputBuffer, 0, len);
        }
    }

    public void writeCounted(final byte[] data) throws IOException {
        writeCounted(data, 0, data.length);
    }

    public void writeCounted(final byte[] data, final int offset, final int length) throws IOException {
        writeOut(data, offset, length);
        writtenToOutputStreamForLastEntry += length;
        totalWrittenToOutputStream += length;
    }

    protected abstract void writeOut(byte[] data, int offset, int length) throws IOException;

    private static final class ScatterGatherBackingStoreCompressor extends StreamCompressor {
        private final ScatterGatherBackingStore bs;

        public ScatterGatherBackingStoreCompressor(final Deflater deflater, final ScatterGatherBackingStore bs) {
            super(deflater);
            this.bs = bs;
        }

        @Override
        protected final void writeOut(final byte[] data, final int offset, final int length)
                throws IOException {
            bs.writeOut(data, offset, length);
        }
    }

    private static final class OutputStreamCompressor extends StreamCompressor {
        private final OutputStream os;

        public OutputStreamCompressor(final Deflater deflater, final OutputStream os) {
            super(deflater);
            this.os = os;
        }

        @Override
        protected final void writeOut(final byte[] data, final int offset, final int length)
                throws IOException {
            os.write(data, offset, length);
        }
    }

    private static final class DataOutputCompressor extends StreamCompressor {
        private final DataOutput raf;

        public DataOutputCompressor(final Deflater deflater, final DataOutput raf) {
            super(deflater);
            this.raf = raf;
        }

        @Override
        protected final void writeOut(final byte[] data, final int offset, final int length)
                throws IOException {
            raf.write(data, offset, length);
        }
    }

    private static final class SeekableByteChannelCompressor extends StreamCompressor {
        private final SeekableByteChannel channel;

        public SeekableByteChannelCompressor(final Deflater deflater,
                                             final SeekableByteChannel channel) {
            super(deflater);
            this.channel = channel;
        }

        @Override
        protected final void writeOut(final byte[] data, final int offset, final int length)
                throws IOException {
            channel.write(ByteBuffer.wrap(data, offset, length));
        }
    }
}
