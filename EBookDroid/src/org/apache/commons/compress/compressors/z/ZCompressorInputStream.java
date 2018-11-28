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
package org.apache.commons.compress.compressors.z;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import org.apache.commons.compress.compressors.lzw.LZWInputStream;

/**
 * Input stream that decompresses .Z files.
 * @NotThreadSafe
 * @since 1.7
 */
public class ZCompressorInputStream extends LZWInputStream {
    private static final int MAGIC_1 = 0x1f;
    private static final int MAGIC_2 = 0x9d;
    private static final int BLOCK_MODE_MASK = 0x80;
    private static final int MAX_CODE_SIZE_MASK = 0x1f;
    private final boolean blockMode;
    private final int maxCodeSize;
    private long totalCodesRead = 0;

    public ZCompressorInputStream(final InputStream inputStream, final int memoryLimitInKb)
            throws IOException {
        super(inputStream, ByteOrder.LITTLE_ENDIAN);
        final int firstByte = (int) in.readBits(8);
        final int secondByte = (int) in.readBits(8);
        final int thirdByte = (int) in.readBits(8);
        if (firstByte != MAGIC_1 || secondByte != MAGIC_2 || thirdByte < 0) {
            throw new IOException("Input is not in .Z format");
        }
        blockMode = (thirdByte & BLOCK_MODE_MASK) != 0;
        maxCodeSize = thirdByte & MAX_CODE_SIZE_MASK;
        if (blockMode) {
            setClearCode(DEFAULT_CODE_SIZE);
        }
        initializeTables(maxCodeSize, memoryLimitInKb);
        clearEntries();
    }

    public ZCompressorInputStream(final InputStream inputStream) throws IOException {
        this(inputStream, -1);
    }

    private void clearEntries() {
        setTableSize((1 << 8) + (blockMode ? 1 : 0));
    }

    /**
     * {@inheritDoc}
     * <p><strong>This method is only protected for technical reasons
     * and is not part of Commons Compress' published API.  It may
     * change or disappear without warning.</strong></p>
     */
    @Override
    protected int readNextCode() throws IOException {
        final int code = super.readNextCode();
        if (code >= 0) {
            ++totalCodesRead;
        }
        return code;
    }

    private void reAlignReading() throws IOException {
        // "compress" works in multiples of 8 symbols, each codeBits bits long.
        // When codeBits changes, the remaining unused symbols in the current
        // group of 8 are still written out, in the old codeSize,
        // as garbage values (usually zeroes) that need to be skipped.
        long codeReadsToThrowAway = 8 - (totalCodesRead % 8);
        if (codeReadsToThrowAway == 8) {
            codeReadsToThrowAway = 0;
        }
        for (long i = 0; i < codeReadsToThrowAway; i++) {
            readNextCode();
        }
        in.clearBitCache();
    }

    /**
     * {@inheritDoc}
     * <p><strong>This method is only protected for technical reasons
     * and is not part of Commons Compress' published API.  It may
     * change or disappear without warning.</strong></p>
     */
    @Override
    protected int addEntry(final int previousCode, final byte character) throws IOException {
        final int maxTableSize = 1 << getCodeSize();
        final int r = addEntry(previousCode, character, maxTableSize);
        if (getTableSize() == maxTableSize && getCodeSize() < maxCodeSize) {
            reAlignReading();
            incrementCodeSize();
        }
        return r;
    }

    /**
     * {@inheritDoc}
     * <p><strong>This method is only protected for technical reasons
     * and is not part of Commons Compress' published API.  It may
     * change or disappear without warning.</strong></p>
     */
    @Override
    protected int decompressNextSymbol() throws IOException {
        //
        //                   table entry    table entry
        //                  _____________   _____
        //    table entry  /             \ /     \
        //    ____________/               \       \
        //   /           / \             / \       \
        //  +---+---+---+---+---+---+---+---+---+---+
        //  | . | . | . | . | . | . | . | . | . | . |
        //  +---+---+---+---+---+---+---+---+---+---+
        //  |<--------->|<------------->|<----->|<->|
        //     symbol        symbol      symbol  symbol
        //
        final int code = readNextCode();
        if (code < 0) {
            return -1;
        } else if (blockMode && code == getClearCode()) {
            clearEntries();
            reAlignReading();
            resetCodeSize();
            resetPreviousCode();
            return 0;
        } else {
            boolean addedUnfinishedEntry = false;
            if (code == getTableSize()) {
                addRepeatOfPreviousCode();
                addedUnfinishedEntry = true;
            } else if (code > getTableSize()) {
                throw new IOException(String.format("Invalid %d bit code 0x%x", getCodeSize(), code));
            }
            return expandCodeToOutputStack(code, addedUnfinishedEntry);
        }
    }

    /**
     * Checks if the signature matches what is expected for a Unix compress file.
     *
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this stream is a Unix compress compressed
     * stream, false otherwise
     *
     * @since 1.9
     */
    public static boolean matches(final byte[] signature, final int length) {
        return length > 3 && signature[0] == MAGIC_1 && signature[1] == (byte) MAGIC_2;
    }

}
