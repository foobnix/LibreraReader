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

package org.apache.commons.compress.compressors;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * Creates Compressor {@link CompressorInputStream}s and
 * {@link CompressorOutputStream}s.
 *
 * @since 1.13
 */
public interface CompressorStreamProvider {

    /**
     * Creates a compressor input stream from a compressor name and an input
     * stream.
     *
     * @param name
     *            of the compressor, i.e.
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#GZIP},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#BZIP2},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#XZ},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#LZMA},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#PACK200},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#SNAPPY_RAW},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#SNAPPY_FRAMED},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#Z}
     *            or
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#DEFLATE}
     * @param in
     *            the input stream
     * @param decompressUntilEOF
     *            if true, decompress until the end of the input; if false, stop
     *            after the first stream and leave the input position to point
     *            to the next byte after the stream. This setting applies to the
     *            gzip, bzip2 and xz formats only.
     * @return compressor input stream
     * @throws CompressorException
     *             if the compressor name is not known
     * @throws IllegalArgumentException
     *             if the name or input stream is null
     */
    CompressorInputStream createCompressorInputStream(final String name, final InputStream in,
            final boolean decompressUntilEOF) throws CompressorException;

    /**
     * Creates a compressor output stream from an compressor name and an output
     * stream.
     *
     * @param name
     *            the compressor name, i.e.
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#GZIP},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#BZIP2},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#XZ},
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#PACK200}
     *            or
     *            {@value org.apache.commons.compress.compressors.CompressorStreamFactory#DEFLATE}
     * @param out
     *            the output stream
     * @return the compressor output stream
     * @throws CompressorException
     *             if the archiver name is not known
     * @throws IllegalArgumentException
     *             if the archiver name or stream is null
     */
    CompressorOutputStream createCompressorOutputStream(final String name, final OutputStream out)
            throws CompressorException;

    /**
     * Gets all the input stream compressor names for this provider
     *
     * @return all the input compressor names for this provider
     */
    Set<String> getInputStreamCompressorNames();

    /**
     * Gets all the output stream compressor names for this provider
     *
     * @return all the output compressor names for this provider
     */
    Set<String> getOutputStreamCompressorNames();

}
