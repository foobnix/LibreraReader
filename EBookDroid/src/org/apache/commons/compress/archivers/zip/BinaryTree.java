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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Binary tree of positive values.
 *
 * @author Emmanuel Bourg
 * @since 1.7
 */
class BinaryTree {

    /** Value in the array indicating an undefined node */
    private static final int UNDEFINED = -1;

    /** Value in the array indicating a non leaf node */
    private static final int NODE = -2;

    /**
     * The array representing the binary tree. The root is at index 0,
     * the left children are at 2*i+1 and the right children at 2*i+2.
     */
    private final int[] tree;

    public BinaryTree(final int depth) {
        tree = new int[(1 << (depth + 1)) - 1];
        Arrays.fill(tree, UNDEFINED);
    }

    /**
     * Adds a leaf to the tree.
     *
     * @param node   the index of the node where the path is appended
     * @param path   the path to the leaf (bits are parsed from the right to the left)
     * @param depth  the number of nodes in the path
     * @param value  the value of the leaf (must be positive)
     */
    public void addLeaf(final int node, final int path, final int depth, final int value) {
        if (depth == 0) {
            // end of the path reached, add the value to the current node
            if (tree[node] == UNDEFINED) {
                tree[node] = value;
            } else {
                throw new IllegalArgumentException("Tree value at index " + node + " has already been assigned (" + tree[node] + ")");
            }
        } else {
            // mark the current node as a non leaf node
            tree[node] = NODE;

            // move down the path recursively
            final int nextChild = 2 * node + 1 + (path & 1);
            addLeaf(nextChild, path >>> 1, depth - 1, value);
        }
    }

    /**
     * Reads a value from the specified bit stream.
     *
     * @param stream
     * @return the value decoded, or -1 if the end of the stream is reached
     */
    public int read(final BitStream stream) throws IOException {
        int currentIndex = 0;

        while (true) {
            final int bit = stream.nextBit();
            if (bit == -1) {
                return -1;
            }

            final int childIndex = 2 * currentIndex + 1 + bit;
            final int value = tree[childIndex];
            if (value == NODE) {
                // consume the next bit
                currentIndex = childIndex;
            } else if (value != UNDEFINED) {
                return value;
            } else {
                throw new IOException("The child " + bit + " of node at index " + currentIndex + " is not defined");
            }
        }
    }


    /**
     * Decodes the packed binary tree from the specified stream.
     */
    static BinaryTree decode(final InputStream in, final int totalNumberOfValues) throws IOException {
        // the first byte contains the size of the structure minus one
        final int size = in.read() + 1;
        if (size == 0) {
            throw new IOException("Cannot read the size of the encoded tree, unexpected end of stream");
        }

        final byte[] encodedTree = new byte[size];
        new DataInputStream(in).readFully(encodedTree);

        /** The maximum bit length for a value (16 or lower) */
        int maxLength = 0;

        final int[] originalBitLengths = new int[totalNumberOfValues];
        int pos = 0;
        for (final byte b : encodedTree) {
            // each byte encodes the number of values (upper 4 bits) for a bit length (lower 4 bits)
            final int numberOfValues = ((b & 0xF0) >> 4) + 1;
            final int bitLength = (b & 0x0F) + 1;

            for (int j = 0; j < numberOfValues; j++) {
                originalBitLengths[pos++] = bitLength;
            }

            maxLength = Math.max(maxLength, bitLength);
        }

        // sort the array of bit lengths and memorize the permutation used to restore the order of the codes
        final int[] permutation = new int[originalBitLengths.length];
        for (int k = 0; k < permutation.length; k++) {
            permutation[k] = k;
        }

        int c = 0;
        final int[] sortedBitLengths = new int[originalBitLengths.length];
        for (int k = 0; k < originalBitLengths.length; k++) {
            // iterate over the values
            for (int l = 0; l < originalBitLengths.length; l++) {
                // look for the value in the original array
                if (originalBitLengths[l] == k) {
                    // put the value at the current position in the sorted array...
                    sortedBitLengths[c] = k;

                    // ...and memorize the permutation
                    permutation[c] = l;

                    c++;
                }
            }
        }

        // decode the values of the tree
        int code = 0;
        int codeIncrement = 0;
        int lastBitLength = 0;

        final int[] codes = new int[totalNumberOfValues];

        for (int i = totalNumberOfValues - 1; i >= 0; i--) {
            code = code + codeIncrement;
            if (sortedBitLengths[i] != lastBitLength) {
                lastBitLength = sortedBitLengths[i];
                codeIncrement = 1 << (16 - lastBitLength);
            }
            codes[permutation[i]] = code;
        }

        // build the tree
        final BinaryTree tree = new BinaryTree(maxLength);

        for (int k = 0; k < codes.length; k++) {
            final int bitLength = originalBitLengths[k];
            if (bitLength > 0) {
                tree.addLeaf(0, Integer.reverse(codes[k] << 16), bitLength, k);
            }
        }

        return tree;
    }
}
