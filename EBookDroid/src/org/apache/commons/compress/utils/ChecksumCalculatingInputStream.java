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
package org.apache.commons.compress.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Checksum;

/**
 * A stream that calculates the checksum of the data read.
 * @NotThreadSafe
 * @since 1.14
 */
public class ChecksumCalculatingInputStream extends InputStream {
    private final InputStream in;
    private final Checksum checksum;

    public ChecksumCalculatingInputStream(final Checksum checksum, final InputStream in) {

        if ( checksum == null ){
            throw new NullPointerException("Parameter checksum must not be null");
        }

        if ( in == null ){
            throw new NullPointerException("Parameter in must not be null");
        }

        this.checksum = checksum;
        this.in = in;
    }

    /**
     * Reads a single byte from the stream
     * @throws IOException if the underlying stream throws or the
     * stream is exhausted and the Checksum doesn't match the expected
     * value
     */
    @Override
    public int read() throws IOException {
        final int ret = in.read();
        if (ret >= 0) {
            checksum.update(ret);
        }
        return ret;
    }

    /**
     * Reads a byte array from the stream
     * @throws IOException if the underlying stream throws or the
     * stream is exhausted and the Checksum doesn't match the expected
     * value
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads from the stream into a byte array.
     * @throws IOException if the underlying stream throws or the
     * stream is exhausted and the Checksum doesn't match the expected
     * value
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int ret = in.read(b, off, len);
        if (ret >= 0) {
            checksum.update(b, off, ret);
        }
        return ret;
    }

    @Override
    public long skip(final long n) throws IOException {
        // Can't really skip, we have to hash everything to verify the checksum
        if (read() >= 0) {
            return 1;
        }
        return 0;
    }

    /**
     * Returns the calculated checksum.
     * @return the calculated checksum.
     */
    public long getValue() {
        return checksum.getValue();
    }

}
