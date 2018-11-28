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

package org.apache.commons.compress.archivers.zip;

import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;

import java.io.IOException;
import java.io.InputStream;

/**
 * The implode compression method was added to PKZIP 1.01 released in 1989.
 * It was then dropped from PKZIP 2.0 released in 1993 in favor of the deflate
 * method.
 * <p>
 * The algorithm is described in the ZIP File Format Specification.
 *
 * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">ZIP File Format Specification</a>
 *
 * @author Emmanuel Bourg
 * @since 1.7
 */
class ExplodingInputStream extends InputStream implements InputStreamStatistics {

    /** The underlying stream containing the compressed data */
    private final InputStream in;

    /** The stream of bits read from the input stream */
    private BitStream bits;

    /** The size of the sliding dictionary (4K or 8K) */
    private final int dictionarySize;

    /** The number of Shannon-Fano trees (2 or 3) */
    private final int numberOfTrees;

    private final int minimumMatchLength;

    /** The binary tree containing the 256 encoded literals (null when only two trees are used) */
    private BinaryTree literalTree;

    /** The binary tree containing the 64 encoded lengths */
    private BinaryTree lengthTree;

    /** The binary tree containing the 64 encoded distances */
    private BinaryTree distanceTree;

    /** Output buffer holding the decompressed data */
    private final CircularBuffer buffer = new CircularBuffer(32 * 1024);

    private long uncompressedCount = 0;

    private long treeSizes = 0;

    /**
     * Create a new stream decompressing the content of the specified stream
     * using the explode algorithm.
     *
     * @param dictionarySize the size of the sliding dictionary (4096 or 8192)
     * @param numberOfTrees  the number of trees (2 or 3)
     * @param in             the compressed data stream
     */
    public ExplodingInputStream(final int dictionarySize, final int numberOfTrees, final InputStream in) {
        if (dictionarySize != 4096 && dictionarySize != 8192) {
            throw new IllegalArgumentException("The dictionary size must be 4096 or 8192");
        }
        if (numberOfTrees != 2 && numberOfTrees != 3) {
            throw new IllegalArgumentException("The number of trees must be 2 or 3");
        }
        this.dictionarySize = dictionarySize;
        this.numberOfTrees = numberOfTrees;
        this.minimumMatchLength = numberOfTrees;
        this.in = in;
    }

    /**
     * Reads the encoded binary trees and prepares the bit stream.
     *
     * @throws IOException
     */
    private void init() throws IOException {
        if (bits == null) {
            try (CountingInputStream i = new CountingInputStream(in) {
                    @Override
                    public void close() {
                        // we do not want to close in
                    }
                }) {
                if (numberOfTrees == 3) {
                    literalTree = BinaryTree.decode(i, 256);
                }

                lengthTree = BinaryTree.decode(i, 64);
                distanceTree = BinaryTree.decode(i, 64);
                treeSizes += i.getBytesRead();
            }

            bits = new BitStream(in);
        }
    }

    @Override
    public int read() throws IOException {
        if (!buffer.available()) {
            fillBuffer();
        }

        final int ret = buffer.get();
        if (ret > -1) {
            uncompressedCount++;
        }
        return ret;
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return bits.getBytesRead() + treeSizes;
    }

    /**
     * @since 1.17
     */
    @Override
    public long getUncompressedCount() {
        return uncompressedCount;
    }

    /**
     * @since 1.17
     */
    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Fill the sliding dictionary with more data.
     * @throws IOException
     */
    private void fillBuffer() throws IOException {
        init();

        final int bit = bits.nextBit();
        if (bit == 1) {
            // literal value
            int literal;
            if (literalTree != null) {
                literal = literalTree.read(bits);
            } else {
                literal = bits.nextByte();
            }

            if (literal == -1) {
                // end of stream reached, nothing left to decode
                return;
            }

            buffer.put(literal);

        } else if (bit == 0) {
            // back reference
            final int distanceLowSize = dictionarySize == 4096 ? 6 : 7;
            final int distanceLow = (int) bits.nextBits(distanceLowSize);
            final int distanceHigh = distanceTree.read(bits);
            if (distanceHigh == -1 && distanceLow <= 0) {
                // end of stream reached, nothing left to decode
                return;
            }
            final int distance = distanceHigh << distanceLowSize | distanceLow;

            int length = lengthTree.read(bits);
            if (length == 63) {
                length += bits.nextBits(8);
            }
            length += minimumMatchLength;

            buffer.copy(distance + 1, length);
        }
    }

}
