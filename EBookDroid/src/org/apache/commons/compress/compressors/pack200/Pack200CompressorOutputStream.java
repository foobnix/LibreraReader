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

package org.apache.commons.compress.compressors.pack200;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Pack200;

import org.apache.commons.compress.compressors.CompressorOutputStream;

/**
 * An output stream that compresses using the Pack200 format.
 *
 * @NotThreadSafe
 * @since 1.3
 */
public class Pack200CompressorOutputStream extends CompressorOutputStream {
    private boolean finished = false;
    private final OutputStream originalOutput;
    private final StreamBridge streamBridge;
    private final Map<String, String> properties;

    /**
     * Compresses the given stream, caching the compressed data in
     * memory.
     *
     * @param out the stream to write to
     * @throws IOException if writing fails
     */
    public Pack200CompressorOutputStream(final OutputStream out)
        throws IOException {
        this(out, Pack200Strategy.IN_MEMORY);
    }

    /**
     * Compresses the given stream using the given strategy to cache
     * the results.
     *
     * @param out the stream to write to
     * @param mode the strategy to use
     * @throws IOException if writing fails
     */
    public Pack200CompressorOutputStream(final OutputStream out,
                                         final Pack200Strategy mode)
        throws IOException {
        this(out, mode, null);
    }

    /**
     * Compresses the given stream, caching the compressed data in
     * memory and using the given properties.
     *
     * @param out the stream to write to
     * @param props Pack200 properties to use
     * @throws IOException if writing fails
     */
    public Pack200CompressorOutputStream(final OutputStream out,
                                         final Map<String, String> props)
        throws IOException {
        this(out, Pack200Strategy.IN_MEMORY, props);
    }

    /**
     * Compresses the given stream using the given strategy to cache
     * the results and the given properties.
     *
     * @param out the stream to write to
     * @param mode the strategy to use
     * @param props Pack200 properties to use
     * @throws IOException if writing fails
     */
    public Pack200CompressorOutputStream(final OutputStream out,
                                         final Pack200Strategy mode,
                                         final Map<String, String> props)
        throws IOException {
        originalOutput = out;
        streamBridge = mode.newStreamBridge();
        properties = props;
    }

    @Override
    public void write(final int b) throws IOException {
        streamBridge.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        streamBridge.write(b);
    }

    @Override
    public void write(final byte[] b, final int from, final int length) throws IOException {
        streamBridge.write(b, from, length);
    }

    @Override
    public void close() throws IOException {
        try {
            finish();
        } finally {
            try {
                streamBridge.stop();
            } finally {
                originalOutput.close();
            }
        }
    }

    public void finish() throws IOException {
        if (!finished) {
            finished = true;
            final Pack200.Packer p = Pack200.newPacker();
            if (properties != null) {
                p.properties().putAll(properties);
            }
            try (JarInputStream ji = new JarInputStream(streamBridge.getInput())) {
                p.pack(ji, originalOutput);
            }
        }
    }
}
