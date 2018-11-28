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
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;

/**
 * Deflate compressor.
 * @since 1.9
 */
public class DeflateCompressorOutputStream extends CompressorOutputStream {
    private final DeflaterOutputStream out;
    private final Deflater deflater;

    /**
     * Creates a Deflate compressed output stream with the default parameters.
     * @param outputStream the stream to wrap
     * @throws IOException on error
     */
    public DeflateCompressorOutputStream(final OutputStream outputStream) throws IOException {
        this(outputStream, new DeflateParameters());
    }

    /**
     * Creates a Deflate compressed output stream with the specified parameters.
     * @param outputStream the stream to wrap
     * @param parameters the deflate parameters to apply
     * @throws IOException on error
     */
    public DeflateCompressorOutputStream(final OutputStream outputStream,
                                         final DeflateParameters parameters) throws IOException {
        this.deflater = new Deflater(parameters.getCompressionLevel(), !parameters.withZlibHeader());
        this.out = new DeflaterOutputStream(outputStream, deflater);
    }

    @Override
    public void write(final int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(final byte[] buf, final int off, final int len) throws IOException {
        out.write(buf, off, len);
    }

    /**
     * Flushes the encoder and calls <code>outputStream.flush()</code>.
     * All buffered pending data will then be decompressible from
     * the output stream. Calling this function very often may increase
     * the compressed file size a lot.
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Finishes compression without closing the underlying stream.
     * <p>No more data can be written to this stream after finishing.</p>
     * @throws IOException on error
     */
    public void finish() throws IOException {
        out.finish();
    }

    @Override
    public void close() throws IOException {
        try {
            out.close();
        } finally {
            deflater.end();
        }
    }
}
