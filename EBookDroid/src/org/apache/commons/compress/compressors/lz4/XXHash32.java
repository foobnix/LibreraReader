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
package org.apache.commons.compress.compressors.lz4;

import static java.lang.Integer.rotateLeft;

import java.util.zip.Checksum;

import static org.apache.commons.compress.utils.ByteUtils.fromLittleEndian;

/**
 * Implementation of the xxhash32 hash algorithm.
 *
 * @see <a href="http://cyan4973.github.io/xxHash/">xxHash</a>
 * @NotThreadSafe
 * @since 1.14
 */
public class XXHash32 implements Checksum {

    private static final int BUF_SIZE = 16;
    private static final int ROTATE_BITS = 13;

    private static final int PRIME1 = (int) 2654435761L;
    private static final int PRIME2 = (int) 2246822519L;
    private static final int PRIME3 = (int) 3266489917L;
    private static final int PRIME4 =  668265263;
    private static final int PRIME5 =  374761393;

    private final byte[] oneByte = new byte[1];
    private final int[] state = new int[4];
    // Note: the code used to use ByteBuffer but the manual method is 50% faster
    // See: https://git-wip-us.apache.org/repos/asf/commons-compress/diff/2f56fb5c
    private final byte[] buffer = new byte[BUF_SIZE];
    private final int seed;

    private int totalLen;
    private int pos;

    /**
     * Creates an XXHash32 instance with a seed of 0.
     */
    public XXHash32() {
        this(0);
    }

    /**
     * Creates an XXHash32 instance.
     * @param seed the seed to use
     */
    public XXHash32(int seed) {
        this.seed = seed;
        initializeState();
    }

    @Override
    public void reset() {
        initializeState();
        totalLen = 0;
        pos = 0;
    }

    @Override
    public void update(int b) {
        oneByte[0] = (byte) (b & 0xff);
        update(oneByte, 0, 1);
    }

    @Override
    public void update(byte[] b, int off, final int len) {
        if (len <= 0) {
            return;
        }
        totalLen += len;

        final int end = off + len;

        if (pos + len < BUF_SIZE) {
            System.arraycopy(b, off, buffer, pos, len);
            pos += len;
            return;
        }

        if (pos > 0) {
            final int size = BUF_SIZE - pos;
            System.arraycopy(b, off, buffer, pos, size);
            process(buffer, 0);
            off += size;
        }

        final int limit = end - BUF_SIZE;
        while (off <= limit) {
            process(b, off);
            off += BUF_SIZE;
        }

        if (off < end) {
            pos = end - off;
            System.arraycopy(b, off, buffer, 0, pos);
        }
    }

    @Override
    public long getValue() {
        int hash;
        if (totalLen > BUF_SIZE) {
            hash =
                rotateLeft(state[0],  1) +
                rotateLeft(state[1],  7) +
                rotateLeft(state[2], 12) +
                rotateLeft(state[3], 18);
        } else {
            hash = state[2] + PRIME5;
        }
        hash += totalLen;

        int idx = 0;
        final int limit = pos - 4;
        for (; idx <= limit; idx += 4) {
            hash = rotateLeft(hash + getInt(buffer, idx) * PRIME3, 17) * PRIME4;
        }
        while (idx < pos) {
            hash = rotateLeft(hash + (buffer[idx++] & 0xff) * PRIME5, 11) * PRIME1;
        }

        hash ^= hash >>> 15;
        hash *= PRIME2;
        hash ^= hash >>> 13;
        hash *= PRIME3;
        hash ^= hash >>> 16;
        return hash & 0xffffffffL;
    }

    private static int getInt(byte[] buffer, int idx) {
        return (int) (fromLittleEndian(buffer, idx, 4) & 0xffffffffL);
    }

    private void initializeState() {
        state[0] = seed + PRIME1 + PRIME2;
        state[1] = seed + PRIME2;
        state[2] = seed;
        state[3] = seed - PRIME1;
    }

    private void process(byte[] b, int offset) {
        // local shadows for performance
        int s0 = state[0];
        int s1 = state[1];
        int s2 = state[2];
        int s3 = state[3];

        s0 = rotateLeft(s0 + getInt(b, offset) * PRIME2, ROTATE_BITS) * PRIME1;
        s1 = rotateLeft(s1 + getInt(b, offset + 4) * PRIME2, ROTATE_BITS) * PRIME1;
        s2 = rotateLeft(s2 + getInt(b, offset + 8) * PRIME2, ROTATE_BITS) * PRIME1;
        s3 = rotateLeft(s3 + getInt(b, offset + 12) * PRIME2, ROTATE_BITS) * PRIME1;

        state[0] = s0;
        state[1] = s1;
        state[2] = s2;
        state[3] = s3;

        pos = 0;
    }
}
