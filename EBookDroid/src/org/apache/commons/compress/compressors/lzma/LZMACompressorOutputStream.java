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
import java.io.OutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;

/**
 * LZMA compressor.
 * @since 1.13
 */
public class LZMACompressorOutputStream extends CompressorOutputStream {
    private final LZMAOutputStream out;

    /**
     * Creates a LZMA compressor.
     *
     * @param       outputStream the stream to wrap
     * @throws      IOException on error
     */
    public LZMACompressorOutputStream(final OutputStream outputStream)
            throws IOException {
        out = new LZMAOutputStream(outputStream, new LZMA2Options(), -1);
    }

    /** {@inheritDoc} */
    @Override
    public void write(final int b) throws IOException {
        out.write(b);
    }

    /** {@inheritDoc} */
    @Override
    public void write(final byte[] buf, final int off, final int len) throws IOException {
        out.write(buf, off, len);
    }

    /**
     * Doesn't do anything as {@link LZMAOutputStream} doesn't support flushing.
     */
    @Override
    public void flush() throws IOException {
    }

    /**
     * Finishes compression without closing the underlying stream.
     * No more data can be written to this stream after finishing.
     * @throws IOException on error
     */
    public void finish() throws IOException {
        out.finish();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        out.close();
    }
}
