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
package org.apache.commons.compress.compressors.xz;

import java.io.IOException;
import java.io.InputStream;

import org.tukaani.xz.XZ;
import org.tukaani.xz.SingleXZInputStream;
import org.tukaani.xz.XZInputStream;

import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * XZ decompressor.
 * @since 1.4
 */
public class XZCompressorInputStream extends CompressorInputStream
    implements InputStreamStatistics {

    private final CountingInputStream countingStream;
    private final InputStream in;

    /**
     * Checks if the signature matches what is expected for a .xz file.
     *
     * @param   signature     the bytes to check
     * @param   length        the number of bytes to check
     * @return  true if signature matches the .xz magic bytes, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < XZ.HEADER_MAGIC.length) {
            return false;
        }

        for (int i = 0; i < XZ.HEADER_MAGIC.length; ++i) {
            if (signature[i] != XZ.HEADER_MAGIC[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a new input stream that decompresses XZ-compressed data
     * from the specified input stream. This doesn't support
     * concatenated .xz files.
     *
     * @param       inputStream where to read the compressed data
     *
     * @throws      IOException if the input is not in the .xz format,
     *                          the input is corrupt or truncated, the .xz
     *                          headers specify options that are not supported
     *                          by this implementation, or the underlying
     *                          <code>inputStream</code> throws an exception
     */
    public XZCompressorInputStream(final InputStream inputStream)
            throws IOException {
        this(inputStream, false);
    }

    /**
     * Creates a new input stream that decompresses XZ-compressed data
     * from the specified input stream.
     *
     * @param       inputStream where to read the compressed data
     * @param       decompressConcatenated
     *                          if true, decompress until the end of the
     *                          input; if false, stop after the first .xz
     *                          stream and leave the input position to point
     *                          to the next byte after the .xz stream
     *
     * @throws      IOException if the input is not in the .xz format,
     *                          the input is corrupt or truncated, the .xz
     *                          headers specify options that are not supported
     *                          by this implementation, or the underlying
     *                          <code>inputStream</code> throws an exception
     */
    public XZCompressorInputStream(final InputStream inputStream,
                                   final boolean decompressConcatenated)
            throws IOException {
        this(inputStream, decompressConcatenated, -1);
    }

    /**
     * Creates a new input stream that decompresses XZ-compressed data
     * from the specified input stream.
     *
     * @param       inputStream where to read the compressed data
     * @param       decompressConcatenated
     *                          if true, decompress until the end of the
     *                          input; if false, stop after the first .xz
     *                          stream and leave the input position to point
     *                          to the next byte after the .xz stream
     * @param       memoryLimitInKb memory limit used when reading blocks.  If
     *                          the estimated memory limit is exceeded on {@link #read()},
     *                          a {@link MemoryLimitException} is thrown.
     *
     * @throws      IOException if the input is not in the .xz format,
     *                          the input is corrupt or truncated, the .xz
     *                          headers specify options that are not supported
     *                          by this implementation,
     *                          or the underlying <code>inputStream</code> throws an exception
     *
     * @since 1.14
     */
    public XZCompressorInputStream(InputStream inputStream,
                                   boolean decompressConcatenated, final int memoryLimitInKb)
            throws IOException {
        countingStream = new CountingInputStream(inputStream);
        if (decompressConcatenated) {
            in = new XZInputStream(countingStream, memoryLimitInKb);
        } else {
            in = new SingleXZInputStream(countingStream, memoryLimitInKb);
        }
    }

    @Override
    public int read() throws IOException {
        try {
            final int ret = in.read();
            count(ret == -1 ? -1 : 1);
            return ret;
        } catch (org.tukaani.xz.MemoryLimitException e) {
            throw new MemoryLimitException(e.getMemoryNeeded(), e.getMemoryLimit(), e);
        }
    }

    @Override
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        try {
            final int ret = in.read(buf, off, len);
            count(ret);
            return ret;
        } catch (org.tukaani.xz.MemoryLimitException e) {
            //convert to commons-compress MemoryLimtException
            throw new MemoryLimitException(e.getMemoryNeeded(), e.getMemoryLimit(), e);
        }
    }

    @Override
    public long skip(final long n) throws IOException {
        try {
            return IOUtils.skip(in, n);
        } catch (org.tukaani.xz.MemoryLimitException e) {
            //convert to commons-compress MemoryLimtException
            throw new MemoryLimitException(e.getMemoryNeeded(), e.getMemoryLimit(), e);
        }
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

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
}
