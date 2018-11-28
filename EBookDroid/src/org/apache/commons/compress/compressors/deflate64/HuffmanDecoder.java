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
package org.apache.commons.compress.compressors.deflate64;

import org.apache.commons.compress.utils.BitInputStream;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.apache.commons.compress.compressors.deflate64.HuffmanState.*;

class HuffmanDecoder implements Closeable {
    
    /**
     * <pre>
     * --------------------------------------------------------------------
     * idx  xtra  base     idx  xtra  base     idx  xtra  base
     * --------------------------------------------------------------------
     * 257   0     3       267   1   15,16     277   4   67-82
     * 258   0     4       268   1   17,18     278   4   83-98
     * 259   0     5       269   2   19-22     279   4   99-114
     * 260   0     6       270   2   23-26     280   4   115-130
     * 261   0     7       271   2   27-30     281   5   131-162
     * 262   0     8       272   2   31-34     282   5   163-194
     * 263   0     9       273   3   35-42     283   5   195-226
     * 264   0     10      274   3   43-50     284   5   227-257
     * 265   1     11,12   275   3   51-58     285   16  3
     * 266   1     13,14   276   3   59-66
     * --------------------------------------------------------------------
     * </pre>
     * value = (base of run length) << 5 | (number of extra bits to read)
     */
    private static final short[] RUN_LENGTH_TABLE = {
            96, 128, 160, 192, 224, 256, 288, 320, 353, 417, 481, 545, 610, 738, 866,
            994, 1123, 1379, 1635, 1891, 2148, 2660, 3172, 3684, 4197, 5221, 6245, 7269, 112
    };

    /**
     * <pre>
     * --------------------------------------------------------------------
     * idx  xtra  dist     idx  xtra  dist       idx  xtra  dist
     * --------------------------------------------------------------------
     * 0    0     1        10   4     33-48      20    9   1025-1536
     * 1    0     2        11   4     49-64      21    9   1537-2048
     * 2    0     3        12   5     65-96      22   10   2049-3072
     * 3    0     4        13   5     97-128     23   10   3073-4096
     * 4    1     5,6      14   6     129-192    24   11   4097-6144
     * 5    1     7,8      15   6     193-256    25   11   6145-8192
     * 6    2     9-12     16   7     257-384    26   12   8193-12288
     * 7    2     13-16    17   7     385-512    27   12   12289-16384
     * 8    3     17-24    18   8     513-768    28   13   16385-24576
     * 9    3     25-32    19   8     769-1024   29   13   24577-32768
     * 30   14   32769-49152
     * 31   14   49153-65536
     * --------------------------------------------------------------------
     * </pre>
     * value = (base of distance) << 4 | (number of extra bits to read)
     */
    private static final int[] DISTANCE_TABLE = {
            16, 32, 48, 64, 81, 113, 146, 210, 275, 403,  // 0-9
            532, 788, 1045, 1557, 2070, 3094, 4119, 6167, 8216, 12312, // 10-19
            16409, 24601, 32794, 49178, 65563, 98331, 131100, 196636, 262173, 393245, // 20-29
            524318, 786462 // 30-31
    };

    /**
     * When using dynamic huffman codes the order in which the values are stored
     * follows the positioning below
     */
    private static final int[] CODE_LENGTHS_ORDER =
            {16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};

    /**
     * Huffman Fixed Literal / Distance tables for mode 1
     */
    private static final int[] FIXED_LITERALS;
    private static final int[] FIXED_DISTANCE;

    static {
        FIXED_LITERALS = new int[288];
        Arrays.fill(FIXED_LITERALS, 0, 144, 8);
        Arrays.fill(FIXED_LITERALS, 144, 256, 9);
        Arrays.fill(FIXED_LITERALS, 256, 280, 7);
        Arrays.fill(FIXED_LITERALS, 280, 288, 8);

        FIXED_DISTANCE = new int[32];
        Arrays.fill(FIXED_DISTANCE, 5);
    }

    private boolean finalBlock = false;
    private DecoderState state;
    private BitInputStream reader;
    private final InputStream in;

    private final DecodingMemory memory = new DecodingMemory();

    HuffmanDecoder(InputStream in) {
        this.reader = new BitInputStream(in, ByteOrder.LITTLE_ENDIAN);
        this.in = in;
        state = new InitialState();
    }

    @Override
    public void close() {
        state = new InitialState();
        reader = null;
    }

    public int decode(byte[] b) throws IOException {
        return decode(b, 0, b.length);
    }

    public int decode(byte[] b, int off, int len) throws IOException {
        while (!finalBlock || state.hasData()) {
            if (state.state() == INITIAL) {
                finalBlock = readBits(1) == 1;
                int mode = (int) readBits(2);
                switch (mode) {
                case 0:
                    switchToUncompressedState();
                    break;
                case 1:
                    state = new HuffmanCodes(FIXED_CODES, FIXED_LITERALS, FIXED_DISTANCE);
                    break;
                case 2:
                    int[][] tables = readDynamicTables();
                    state = new HuffmanCodes(DYNAMIC_CODES, tables[0], tables[1]);
                    break;
                default:
                    throw new IllegalStateException("Unsupported compression: " + mode);
                }
            } else {
                return state.read(b, off, len);
            }
        }
        return -1;
    }

    /**
     * @since 1.17
     */
    long getBytesRead() {
        return reader.getBytesRead();
    }

    private void switchToUncompressedState() throws IOException {
        reader.alignWithByteBoundary();
        long bLen = readBits(16);
        long bNLen = readBits(16);
        if (((bLen ^ 0xFFFF) & 0xFFFF) != bNLen) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalStateException("Illegal LEN / NLEN values");
        }
        state = new UncompressedState(bLen);
    }

    private int[][] readDynamicTables() throws IOException {
        int[][] result = new int[2][];
        int literals = (int) (readBits(5) + 257);
        result[0] = new int[literals];

        int distances = (int) (readBits(5) + 1);
        result[1] = new int[distances];

        populateDynamicTables(reader, result[0], result[1]);
        return result;
    }

    int available() throws IOException {
        return state.available();
    }

    private abstract static class DecoderState {
        abstract HuffmanState state();

        abstract int read(byte[] b, int off, int len) throws IOException;

        abstract boolean hasData();

        abstract int available() throws IOException ;
    }

    private class UncompressedState extends DecoderState {
        private final long blockLength;
        private long read;

        private UncompressedState(long blockLength) {
            this.blockLength = blockLength;
        }

        @Override
        HuffmanState state() {
            return read < blockLength ? STORED : INITIAL;
        }

        @Override
        int read(byte[] b, int off, int len) throws IOException {
            // as len is an int and (blockLength - read) is >= 0 the min must fit into an int as well
            int max = (int) Math.min(blockLength - read, len);
            int readSoFar = 0;
            while (readSoFar < max) {
                int readNow;
                if (reader.bitsCached() > 0) {
                    byte next = (byte) readBits(Byte.SIZE);
                    b[off + readSoFar] = memory.add(next);
                    readNow = 1;
                } else {
                    readNow = in.read(b, off + readSoFar, max - readSoFar);
                    if (readNow == -1) {
                        throw new EOFException("Truncated Deflate64 Stream");
                    }
                    memory.add(b, off + readSoFar, readNow);
                }
                read += readNow;
                readSoFar += readNow;
            }
            return max;
        }

        @Override
        boolean hasData() {
            return read < blockLength;
        }

        @Override
        int available() throws IOException {
            return (int) Math.min(blockLength - read, reader.bitsAvailable() / Byte.SIZE);
        }
    }

    private class InitialState extends DecoderState {
        @Override
        HuffmanState state() {
            return INITIAL;
        }

        @Override
        int read(byte[] b, int off, int len) throws IOException {
            throw new IllegalStateException("Cannot read in this state");
        }

        @Override
        boolean hasData() {
            return false;
        }

        @Override
        int available() {
            return 0;
        }
    }

    private class HuffmanCodes extends DecoderState {
        private boolean endOfBlock = false;
        private final HuffmanState state;
        private final BinaryTreeNode lengthTree;
        private final BinaryTreeNode distanceTree;

        private int runBufferPos = 0;
        private byte[] runBuffer = new byte[0];
        private int runBufferLength = 0;

        HuffmanCodes(HuffmanState state, int[] lengths, int[] distance) {
            this.state = state;
            lengthTree = buildTree(lengths);
            distanceTree = buildTree(distance);
        }

        @Override
        HuffmanState state() {
            return endOfBlock ? INITIAL : state;
        }

        @Override
        int read(byte[] b, int off, int len) throws IOException {
            return decodeNext(b, off, len);
        }

        private int decodeNext(byte[] b, int off, int len) throws IOException {
            if (endOfBlock) {
                return -1;
            }
            int result = copyFromRunBuffer(b, off, len);

            while (result < len) {
                int symbol = nextSymbol(reader, lengthTree);
                if (symbol < 256) {
                    b[off + result++] = memory.add((byte) symbol);
                } else if (symbol > 256) {
                    int runMask = RUN_LENGTH_TABLE[symbol - 257];
                    int run = runMask >>> 5;
                    int runXtra = runMask & 0x1F;
                    run += readBits(runXtra);

                    int distSym = nextSymbol(reader, distanceTree);

                    int distMask = DISTANCE_TABLE[distSym];
                    int dist = distMask >>> 4;
                    int distXtra = distMask & 0xF;
                    dist += readBits(distXtra);

                    if (runBuffer.length < run) {
                        runBuffer = new byte[run];
                    }
                    runBufferLength = run;
                    runBufferPos = 0;
                    memory.recordToBuffer(dist, run, runBuffer);

                    result += copyFromRunBuffer(b, off + result, len - result);
                } else {
                    endOfBlock = true;
                    return result;
                }
            }

            return result;
        }

        private int copyFromRunBuffer(byte[] b, int off, int len) {
            int bytesInBuffer = runBufferLength - runBufferPos;
            int copiedBytes = 0;
            if (bytesInBuffer > 0) {
                copiedBytes = Math.min(len, bytesInBuffer);
                System.arraycopy(runBuffer, runBufferPos, b, off, copiedBytes);
                runBufferPos += copiedBytes;
            }
            return copiedBytes;
        }

        @Override
        boolean hasData() {
            return !endOfBlock;
        }

        @Override
        int available() {
            return runBufferLength - runBufferPos;
        }
    }

    private static int nextSymbol(BitInputStream reader, BinaryTreeNode tree) throws IOException {
        BinaryTreeNode node = tree;
        while (node != null && node.literal == -1) {
            long bit = readBits(reader, 1);
            node = bit == 0 ? node.leftNode : node.rightNode;
        }
        return node != null ? node.literal : -1;
    }

    private static void populateDynamicTables(BitInputStream reader, int[] literals, int[] distances) throws IOException {
        int codeLengths = (int) (readBits(reader, 4) + 4);

        int[] codeLengthValues = new int[19];
        for (int cLen = 0; cLen < codeLengths; cLen++) {
            codeLengthValues[CODE_LENGTHS_ORDER[cLen]] = (int) readBits(reader, 3);
        }

        BinaryTreeNode codeLengthTree = buildTree(codeLengthValues);

        final int[] auxBuffer = new int[literals.length + distances.length];

        int value = -1;
        int length = 0;
        int off = 0;
        while (off < auxBuffer.length) {
            if (length > 0) {
                auxBuffer[off++] = value;
                length--;
            } else {
                int symbol = nextSymbol(reader, codeLengthTree);
                if (symbol < 16) {
                    value = symbol;
                    auxBuffer[off++] = value;
                } else if (symbol == 16) {
                    length = (int) (readBits(reader, 2) + 3);
                } else if (symbol == 17) {
                    value = 0;
                    length = (int) (readBits(reader, 3) + 3);
                } else if (symbol == 18) {
                    value = 0;
                    length = (int) (readBits(reader, 7) + 11);
                }
            }
        }

        System.arraycopy(auxBuffer, 0, literals, 0, literals.length);
        System.arraycopy(auxBuffer, literals.length, distances, 0, distances.length);
    }

    private static class BinaryTreeNode {
        private final int bits;
        int literal = -1;
        BinaryTreeNode leftNode;
        BinaryTreeNode rightNode;

        private BinaryTreeNode(int bits) {
            this.bits = bits;
        }

        void leaf(int symbol) {
            literal = symbol;
            leftNode = null;
            rightNode = null;
        }

        BinaryTreeNode left() {
            if (leftNode == null && literal == -1) {
                leftNode = new BinaryTreeNode(bits + 1);
            }
            return leftNode;
        }

        BinaryTreeNode right() {
            if (rightNode == null && literal == -1) {
                rightNode = new BinaryTreeNode(bits + 1);
            }
            return rightNode;
        }
    }

    private static BinaryTreeNode buildTree(int[] litTable) {
        int[] literalCodes = getCodes(litTable);

        BinaryTreeNode root = new BinaryTreeNode(0);

        for (int i = 0; i < litTable.length; i++) {
            int len = litTable[i];
            if (len != 0) {
                BinaryTreeNode node = root;
                int lit = literalCodes[len - 1];
                for (int p = len - 1; p >= 0; p--) {
                    int bit = lit & (1 << p);
                    node = bit == 0 ? node.left() : node.right();
                }
                node.leaf(i);
                literalCodes[len - 1]++;
            }
        }
        return root;
    }

    private static int[] getCodes(int[] litTable) {
        int max = 0;
        int[] blCount = new int[65];

        for (int aLitTable : litTable) {
            max = Math.max(max, aLitTable);
            blCount[aLitTable]++;
        }
        blCount = Arrays.copyOf(blCount, max + 1);

        int code = 0;
        int[] nextCode = new int[max + 1];
        for (int i = 0; i <= max; i++) {
            code = (code + blCount[i]) << 1;
            nextCode[i] = code;
        }

        return nextCode;
    }

    private static class DecodingMemory {
        private final byte[] memory;
        private final int mask;
        private int wHead;
        private boolean wrappedAround;

        private DecodingMemory() {
            this(16);
        }

        private DecodingMemory(int bits) {
            memory = new byte[1 << bits];
            mask = memory.length - 1;
        }

        byte add(byte b) {
            memory[wHead] = b;
            wHead = incCounter(wHead);
            return b;
        }

        void add(byte[] b, int off, int len) {
            for (int i = off; i < off + len; i++) {
                add(b[i]);
            }
        }

        void recordToBuffer(int distance, int length, byte[] buff) {
            if (distance > memory.length) {
                throw new IllegalStateException("Illegal distance parameter: " + distance);
            }
            int start = (wHead - distance) & mask;
            if (!wrappedAround && start >= wHead) {
                throw new IllegalStateException("Attempt to read beyond memory: dist=" + distance);
            }
            for (int i = 0, pos = start; i < length; i++, pos = incCounter(pos)) {
                buff[i] = add(memory[pos]);
            }
        }

        private int incCounter(int counter) {
            final int newCounter = (counter + 1) & mask;
            if (!wrappedAround && newCounter < counter) {
                wrappedAround = true;
            }
            return newCounter;
        }
    }

    private long readBits(int numBits) throws IOException {
        return readBits(reader, numBits);
    }

    private static long readBits(BitInputStream reader, int numBits) throws IOException {
        long r = reader.readBits(numBits);
        if (r == -1) {
            throw new EOFException("Truncated Deflate64 Stream");
        }
        return r;
    }
}
