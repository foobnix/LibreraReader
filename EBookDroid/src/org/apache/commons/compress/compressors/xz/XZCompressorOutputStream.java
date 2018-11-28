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
import java.io.OutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;

/**
 * XZ compressor.
 * @since 1.4
 */
public class XZCompressorOutputStream extends CompressorOutputStream {
    private final XZOutputStream out;

    /**
     * Creates a new XZ compressor using the default LZMA2 options.
     * This is equivalent to <code>XZCompressorOutputStream(outputStream, 6)</code>.
     * @param outputStream the stream to wrap
     * @throws IOException on error
     */
    public XZCompressorOutputStream(final OutputStream outputStream)
            throws IOException {
        out = new XZOutputStream(outputStream, new LZMA2Options());
    }

    /**
     * Creates a new XZ compressor using the specified LZMA2 preset level.
     * <p>
     * The presets 0-3 are fast presets with medium compression.
     * The presets 4-6 are fairly slow presets with high compression.
     * The default preset is 6.
     * <p>
     * The presets 7-9 are like the preset 6 but use bigger dictionaries
     * and have higher compressor and decompressor memory requirements.
     * Unless the uncompressed size of the file exceeds 8&nbsp;MiB,
     * 16&nbsp;MiB, or 32&nbsp;MiB, it is waste of memory to use the
     * presets 7, 8, or 9, respectively.
     * @param outputStream the stream to wrap
     * @param preset the preset
     * @throws IOException on error
     */
    public XZCompressorOutputStream(final OutputStream outputStream, final int preset)
            throws IOException {
        out = new XZOutputStream(outputStream, new LZMA2Options(preset));
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
     * No more data can be written to this stream after finishing.
     * @throws IOException on error
     */
    public void finish() throws IOException {
        out.finish();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
