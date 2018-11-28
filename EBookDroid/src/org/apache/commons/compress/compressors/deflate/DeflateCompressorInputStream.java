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
package org.apache.commons.compress.compressors.deflate;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * Deflate decompressor.
 * @since 1.9
 */
public class DeflateCompressorInputStream extends CompressorInputStream
    implements InputStreamStatistics {

    private static final int MAGIC_1 = 0x78;
    private static final int MAGIC_2a = 0x01;
    private static final int MAGIC_2b = 0x5e;
    private static final int MAGIC_2c = 0x9c;
    private static final int MAGIC_2d = 0xda;

    private final CountingInputStream countingStream;
    private final InputStream in;
    private final Inflater inflater;

    /**
     * Creates a new input stream that decompresses Deflate-compressed data
     * from the specified input stream.
     *
     * @param       inputStream where to read the compressed data
     *
     */
    public DeflateCompressorInputStream(final InputStream inputStream) {
        this(inputStream, new DeflateParameters());
    }

    /**
     * Creates a new input stream that decompresses Deflate-compressed data
     * from the specified input stream.
     *
     * @param       inputStream where to read the compressed data
     * @param       parameters parameters
     */
    public DeflateCompressorInputStream(final InputStream inputStream,
                                        final DeflateParameters parameters) {
        inflater = new Inflater(!parameters.withZlibHeader());
        in = new InflaterInputStream(countingStream = new CountingInputStream(inputStream), inflater);
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        final int ret = in.read();
        count(ret == -1 ? 0 : 1);
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        final int ret = in.read(buf, off, len);
        count(ret);
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public long skip(final long n) throws IOException {
        return IOUtils.skip(in, n);
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
        return in.available();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        try {
            in.close();
        } finally {
            inflater.end();
        }
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return countingStream.getBytesRead();
    }

    /**
     * Checks if the signature matches what is expected for a zlib / deflated file
     *  with the zlib header.
     *
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this stream is zlib / deflate compressed with a header
     * stream, false otherwise
     *
     * @since 1.10
     */
    public static boolean matches(final byte[] signature, final int length) {
        return length > 3 && signature[0] == MAGIC_1 && (
                signature[1] == (byte) MAGIC_2a ||
                signature[1] == (byte) MAGIC_2b ||
                signature[1] == (byte) MAGIC_2c ||
                signature[1] == (byte) MAGIC_2d);
    }
}
