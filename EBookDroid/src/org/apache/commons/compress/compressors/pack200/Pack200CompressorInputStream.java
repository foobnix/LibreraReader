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

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * An input stream that decompresses from the Pack200 format to be read
 * as any other stream.
 *
 * <p>The {@link CompressorInputStream#getCount getCount} and {@link
 * CompressorInputStream#getBytesRead getBytesRead} methods always
 * return 0.</p>
 *
 * @NotThreadSafe
 * @since 1.3
 */
public class Pack200CompressorInputStream extends CompressorInputStream {
    private final InputStream originalInput;
    private final StreamBridge streamBridge;

    /**
     * Decompresses the given stream, caching the decompressed data in
     * memory.
     *
     * <p>When reading from a file the File-arg constructor may
     * provide better performance.</p>
     *
     * @param in the InputStream from which this object should be created
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final InputStream in)
        throws IOException {
        this(in, Pack200Strategy.IN_MEMORY);
    }

    /**
     * Decompresses the given stream using the given strategy to cache
     * the results.
     *
     * <p>When reading from a file the File-arg constructor may
     * provide better performance.</p>
     *
     * @param in the InputStream from which this object should be created
     * @param mode the strategy to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final InputStream in,
                                        final Pack200Strategy mode)
        throws IOException {
        this(in, null, mode, null);
    }

    /**
     * Decompresses the given stream, caching the decompressed data in
     * memory and using the given properties.
     *
     * <p>When reading from a file the File-arg constructor may
     * provide better performance.</p>
     *
     * @param in the InputStream from which this object should be created
     * @param props Pack200 properties to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final InputStream in,
                                        final Map<String, String> props)
        throws IOException {
        this(in, Pack200Strategy.IN_MEMORY, props);
    }

    /**
     * Decompresses the given stream using the given strategy to cache
     * the results and the given properties.
     *
     * <p>When reading from a file the File-arg constructor may
     * provide better performance.</p>
     *
     * @param in the InputStream from which this object should be created
     * @param mode the strategy to use
     * @param props Pack200 properties to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final InputStream in,
                                        final Pack200Strategy mode,
                                        final Map<String, String> props)
        throws IOException {
        this(in, null, mode, props);
    }

    /**
     * Decompresses the given file, caching the decompressed data in
     * memory.
     *
     * @param f the file to decompress
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final File f) throws IOException {
        this(f, Pack200Strategy.IN_MEMORY);
    }

    /**
     * Decompresses the given file using the given strategy to cache
     * the results.
     *
     * @param f the file to decompress
     * @param mode the strategy to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final File f, final Pack200Strategy mode)
        throws IOException {
        this(null, f, mode, null);
    }

    /**
     * Decompresses the given file, caching the decompressed data in
     * memory and using the given properties.
     *
     * @param f the file to decompress
     * @param props Pack200 properties to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final File f,
                                        final Map<String, String> props)
        throws IOException {
        this(f, Pack200Strategy.IN_MEMORY, props);
    }

    /**
     * Decompresses the given file using the given strategy to cache
     * the results and the given properties.
     *
     * @param f the file to decompress
     * @param mode the strategy to use
     * @param props Pack200 properties to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final File f, final Pack200Strategy mode,
                                        final Map<String, String> props)
        throws IOException {
        this(null, f, mode, props);
    }

    private Pack200CompressorInputStream(final InputStream in, final File f,
                                         final Pack200Strategy mode,
                                         final Map<String, String> props)
            throws IOException {
        originalInput = in;
        streamBridge = mode.newStreamBridge();
        try (final JarOutputStream jarOut = new JarOutputStream(streamBridge)) {
            final Pack200.Unpacker u = Pack200.newUnpacker();
            if (props != null) {
                u.properties().putAll(props);
            }
            if (f == null) {
                u.unpack(new FilterInputStream(in) {
                    @Override
                    public void close() {
                        // unpack would close this stream but we
                        // want to give the user code more control
                    }
                }, jarOut);
            } else {
                u.unpack(f, jarOut);
            }
        }
    }

    @Override
    public int read() throws IOException {
        return streamBridge.getInput().read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return streamBridge.getInput().read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int count) throws IOException {
        return streamBridge.getInput().read(b, off, count);
    }

    @Override
    public int available() throws IOException {
        return streamBridge.getInput().available();
    }

    @Override
    public boolean markSupported() {
        try {
            return streamBridge.getInput().markSupported();
        } catch (final IOException ex) {
            return false;
        }
    }

    @Override
    public void mark(final int limit) {
        try {
            streamBridge.getInput().mark(limit);
        } catch (final IOException ex) {
            throw new RuntimeException(ex); //NOSONAR
        }
    }

    @Override
    public void reset() throws IOException {
        streamBridge.getInput().reset();
    }

    @Override
    public long skip(final long count) throws IOException {
        return IOUtils.skip(streamBridge.getInput(), count);
    }

    @Override
    public void close() throws IOException {
        try {
            streamBridge.stop();
        } finally {
            if (originalInput != null) {
                originalInput.close();
            }
        }
    }

    private static final byte[] CAFE_DOOD = new byte[] {
        (byte) 0xCA, (byte) 0xFE, (byte) 0xD0, (byte) 0x0D
    };
    private static final int SIG_LENGTH = CAFE_DOOD.length;

    /**
     * Checks if the signature matches what is expected for a pack200
     * file (0xCAFED00D).
     *
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this stream is a pack200 compressed stream,
     * false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < SIG_LENGTH) {
            return false;
        }

        for (int i = 0; i < SIG_LENGTH; i++) {
            if (signature[i] != CAFE_DOOD[i]) {
                return false;
            }
        }

        return true;
    }
}
