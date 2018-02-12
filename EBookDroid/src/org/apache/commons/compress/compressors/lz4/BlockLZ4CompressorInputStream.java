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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.lz77support.AbstractLZ77CompressorInputStream;
import org.apache.commons.compress.utils.ByteUtils;

/**
 * CompressorInputStream for the LZ4 block format.
 *
 * @see <a href="http://lz4.github.io/lz4/lz4_Block_format.html">LZ4 Block Format Description</a>
 * @since 1.14
 * @NotThreadSafe
 */
public class BlockLZ4CompressorInputStream extends AbstractLZ77CompressorInputStream {

    static final int WINDOW_SIZE = 1 << 16;
    static final int SIZE_BITS = 4;
    static final int BACK_REFERENCE_SIZE_MASK = (1 << SIZE_BITS) - 1;
    static final int LITERAL_SIZE_MASK = BACK_REFERENCE_SIZE_MASK << SIZE_BITS;

    /** Back-Reference-size part of the block starting byte. */
    private int nextBackReferenceSize;

    /** Current state of the stream */
    private State state = State.NO_BLOCK;

    /**
     * Creates a new LZ4 input stream.
     *
     * @param is
     *            An InputStream to read compressed data from
     *
     * @throws IOException if reading fails
     */
    public BlockLZ4CompressorInputStream(final InputStream is) throws IOException {
        super(is, WINDOW_SIZE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        switch (state) {
        case EOF:
            return -1;
        case NO_BLOCK: // NOSONAR - fallthrough intended
            readSizes();
            /*FALLTHROUGH*/
        case IN_LITERAL:
            int litLen = readLiteral(b, off, len);
            if (!hasMoreDataInBlock()) {
                state = State.LOOKING_FOR_BACK_REFERENCE;
            }
            return litLen > 0 ? litLen : read(b, off, len);
        case LOOKING_FOR_BACK_REFERENCE: // NOSONAR - fallthrough intended
            if (!initializeBackReference()) {
                state = State.EOF;
                return -1;
            }
            /*FALLTHROUGH*/
        case IN_BACK_REFERENCE:
            int backReferenceLen = readBackReference(b, off, len);
            if (!hasMoreDataInBlock()) {
                state = State.NO_BLOCK;
            }
            return backReferenceLen > 0 ? backReferenceLen : read(b, off, len);
        default:
            throw new IOException("Unknown stream state " + state);
        }
    }

    private void readSizes() throws IOException {
        int nextBlock = readOneByte();
        if (nextBlock == -1) {
            throw new IOException("Premature end of stream while looking for next block");
        }
        nextBackReferenceSize = nextBlock & BACK_REFERENCE_SIZE_MASK;
        long literalSizePart = (nextBlock & LITERAL_SIZE_MASK) >> SIZE_BITS;
        if (literalSizePart == BACK_REFERENCE_SIZE_MASK) {
            literalSizePart += readSizeBytes();
        }
        startLiteral(literalSizePart);
        state = State.IN_LITERAL;
    }

    private long readSizeBytes() throws IOException {
        long accum = 0;
        int nextByte;
        do {
            nextByte = readOneByte();
            if (nextByte == -1) {
                throw new IOException("Premature end of stream while parsing length");
            }
            accum += nextByte;
        } while (nextByte == 255);
        return accum;
    }

    /**
     * @return false if there is no more back-reference - this means this is the
     * last block of the stream.
     */
    private boolean initializeBackReference() throws IOException {
        int backReferenceOffset = 0;
        try {
            backReferenceOffset = (int) ByteUtils.fromLittleEndian(supplier, 2);
        } catch (IOException ex) {
            if (nextBackReferenceSize == 0) { // the last block has no back-reference
                return false;
            }
            throw ex;
        }
        long backReferenceSize = nextBackReferenceSize;
        if (nextBackReferenceSize == BACK_REFERENCE_SIZE_MASK) {
            backReferenceSize += readSizeBytes();
        }
        // minimal match length 4 is encoded as 0
        startBackReference(backReferenceOffset, backReferenceSize + 4);
        state = State.IN_BACK_REFERENCE;
        return true;
    }

    private enum State {
        NO_BLOCK, IN_LITERAL, LOOKING_FOR_BACK_REFERENCE, IN_BACK_REFERENCE, EOF
    }
}
