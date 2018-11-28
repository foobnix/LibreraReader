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
package org.apache.commons.compress.compressors.lzma;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.MemoryLimitException;
import org.tukaani.xz.LZMAInputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * LZMA decompressor.
 * @since 1.6
 */
public class LZMACompressorInputStream extends CompressorInputStream
    implements InputStreamStatistics {

    private final CountingInputStream countingStream;
    private final InputStream in;

    /**
     * Creates a new input stream that decompresses LZMA-compressed data
     * from the specified input stream.
     *
     * @param       inputStream where to read the compressed data
     *
     * @throws      IOException if the input is not in the .lzma format,
     *                          the input is corrupt or truncated, the .lzma
     *                          headers specify sizes that are not supported
     *                          by this implementation, or the underlying
     *                          <code>inputStream</code> throws an exception
     */
    public LZMACompressorInputStream(final InputStream inputStream)
            throws IOException {
        in = new LZMAInputStream(countingStream = new CountingInputStream(inputStream), -1);
    }

    /**
     * Creates a new input stream that decompresses LZMA-compressed data
     * from the specified input stream.
     *
     * @param       inputStream where to read the compressed data
     *
     * @param       memoryLimitInKb calculated memory use threshold.  Throws MemoryLimitException
     *                            if calculate memory use is above this threshold
     *
     * @throws      IOException if the input is not in the .lzma format,
     *                          the input is corrupt or truncated, the .lzma
     *                          headers specify sizes that are not supported
     *                          by this implementation, or the underlying
     *                          <code>inputStream</code> throws an exception
     *
     * @since 1.14
     */
    public LZMACompressorInputStream(final InputStream inputStream, int memoryLimitInKb)
            throws IOException {
        try {
            in = new LZMAInputStream(countingStream = new CountingInputStream(inputStream), memoryLimitInKb);
        } catch (org.tukaani.xz.MemoryLimitException e) {
            //convert to commons-compress exception
            throw new MemoryLimitException(e.getMemoryNeeded(), e.getMemoryLimit(), e);
        }
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
        in.close();
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return countingStream.getBytesRead();
    }

    /**
     * Checks if the signature matches what is expected for an lzma file.
     *
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this stream is an lzma  compressed stream, false otherwise
     *
     * @since 1.10
     */
    public static boolean matches(final byte[] signature, final int length) {
        return signature != null && length >= 3 &&
                signature[0] == 0x5d && signature[1] == 0 &&
                signature[2] == 0;
    }
}
