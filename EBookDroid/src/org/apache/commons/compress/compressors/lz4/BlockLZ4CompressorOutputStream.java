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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.lz77support.LZ77Compressor;
import org.apache.commons.compress.compressors.lz77support.Parameters;
import org.apache.commons.compress.utils.ByteUtils;

/**
 * CompressorOutputStream for the LZ4 block format.
 *
 * @see <a href="http://lz4.github.io/lz4/lz4_Block_format.html">LZ4 Block Format Description</a>
 * @since 1.14
 * @NotThreadSafe
 */
public class BlockLZ4CompressorOutputStream extends CompressorOutputStream {

    private static final int MIN_BACK_REFERENCE_LENGTH = 4;
    private static final int MIN_OFFSET_OF_LAST_BACK_REFERENCE = 12;

    /*

      The LZ4 block format has a few properties that make it less
      straight-forward than one would hope:

      * literal blocks and back-references must come in pairs (except
        for the very last literal block), so consecutive literal
        blocks created by the compressor must be merged into a single
        block.

      * the start of a literal/back-reference pair contains the length
        of the back-reference (at least some part of it) so we can't
        start writing the literal before we know how long the next
        back-reference is going to be.

      * there are special rules for the final blocks

        > There are specific parsing rules to respect in order to remain
        > compatible with assumptions made by the decoder :
        >
        >     1. The last 5 bytes are always literals
        >
        >     2. The last match must start at least 12 bytes before end of
        >        block. Consequently, a block with less than 13 bytes cannot be
        >        compressed.

        which means any back-reference may need to get rewritten as a
        literal block unless we know the next block is at least of
        length 5 and the sum of this block's length and offset and the
        next block's length is at least twelve.

    */

    private final LZ77Compressor compressor;
    private final OutputStream os;

    // used in one-arg write method
    private final byte[] oneByte = new byte[1];

    private boolean finished = false;

    private Deque<Pair> pairs = new LinkedList<>();
    // keeps track of the last window-size bytes (64k) in order to be
    // able to expand back-references when needed
    private Deque<byte[]> expandedBlocks = new LinkedList<>();

    /**
     * Creates a new LZ4 output stream.
     *
     * @param os
     *            An OutputStream to read compressed data from
     *
     * @throws IOException if reading fails
     */
    public BlockLZ4CompressorOutputStream(final OutputStream os) throws IOException {
        this(os, createParameterBuilder().build());
    }

    /**
     * Creates a new LZ4 output stream.
     *
     * @param os
     *            An OutputStream to read compressed data from
     * @param params
     *            The parameters to use for LZ77 compression.
     *
     * @throws IOException if reading fails
     */
    public BlockLZ4CompressorOutputStream(final OutputStream os, Parameters params) throws IOException {
        this.os = os;
        compressor = new LZ77Compressor(params,
            new LZ77Compressor.Callback() {
                @Override
                public void accept(LZ77Compressor.Block block) throws IOException {
                    switch (block.getType()) {
                    case LITERAL:
                        addLiteralBlock((LZ77Compressor.LiteralBlock) block);
                        break;
                    case BACK_REFERENCE:
                        addBackReference((LZ77Compressor.BackReference) block);
                        break;
                    case EOD:
                        writeFinalLiteralBlock();
                        break;
                    }
                }
            });
    }

    @Override
    public void write(int b) throws IOException {
        oneByte[0] = (byte) (b & 0xff);
        write(oneByte);
    }

    @Override
    public void write(byte[] data, int off, int len) throws IOException {
        compressor.compress(data, off, len);
    }

    @Override
    public void close() throws IOException {
        try {
            finish();
        } finally {
            os.close();
        }
    }

    /**
     * Compresses all remaining data and writes it to the stream,
     * doesn't close the underlying stream.
     * @throws IOException if an error occurs
     */
    public void finish() throws IOException {
        if (!finished) {
            compressor.finish();
            finished = true;
        }
    }

    /**
     * Adds some initial data to fill the window with.
     *
     * @param data the data to fill the window with.
     * @param off offset of real data into the array
     * @param len amount of data
     * @throws IllegalStateException if the stream has already started to write data
     * @see LZ77Compressor#prefill
     */
    public void prefill(byte[] data, int off, int len) {
        if (len > 0) {
            byte[] b = Arrays.copyOfRange(data, off, off + len);
            compressor.prefill(b);
            recordLiteral(b);
        }
    }

    private void addLiteralBlock(LZ77Compressor.LiteralBlock block) throws IOException {
        Pair last = writeBlocksAndReturnUnfinishedPair(block.getLength());
        recordLiteral(last.addLiteral(block));
        clearUnusedBlocksAndPairs();
    }

    private void addBackReference(LZ77Compressor.BackReference block) throws IOException {
        Pair last = writeBlocksAndReturnUnfinishedPair(block.getLength());
        last.setBackReference(block);
        recordBackReference(block);
        clearUnusedBlocksAndPairs();
    }

    private Pair writeBlocksAndReturnUnfinishedPair(int length) throws IOException {
        writeWritablePairs(length);
        Pair last = pairs.peekLast();
        if (last == null || last.hasBackReference()) {
            last = new Pair();
            pairs.addLast(last);
        }
        return last;
    }

    private void recordLiteral(byte[] b) {
        expandedBlocks.addFirst(b);
    }

    private void clearUnusedBlocksAndPairs() {
        clearUnusedBlocks();
        clearUnusedPairs();
    }

    private void clearUnusedBlocks() {
        int blockLengths = 0;
        int blocksToKeep = 0;
        for (byte[] b : expandedBlocks) {
            blocksToKeep++;
            blockLengths += b.length;
            if (blockLengths >= BlockLZ4CompressorInputStream.WINDOW_SIZE) {
                break;
            }
        }
        final int size = expandedBlocks.size();
        for (int i = blocksToKeep; i < size; i++) {
            expandedBlocks.removeLast();
        }
    }

    private void recordBackReference(LZ77Compressor.BackReference block) {
        expandedBlocks.addFirst(expand(block.getOffset(), block.getLength()));
    }

    private byte[] expand(final int offset, final int length) {
        byte[] expanded = new byte[length];
        if (offset == 1) { // surprisingly common special case
            byte[] block = expandedBlocks.peekFirst();
            byte b = block[block.length - 1];
            if (b != 0) { // the fresh array contains 0s anyway
                Arrays.fill(expanded, b);
            }
        } else {
            expandFromList(expanded, offset, length);
        }
        return expanded;
    }

    private void expandFromList(final byte[] expanded, int offset, int length) {
        int offsetRemaining = offset;
        int lengthRemaining = length;
        int writeOffset = 0;
        while (lengthRemaining > 0) {
            // find block that contains offsetRemaining
            byte[] block = null;
            int copyLen, copyOffset;
            if (offsetRemaining > 0) {
                int blockOffset = 0;
                for (byte[] b : expandedBlocks) {
                    if (b.length + blockOffset >= offsetRemaining) {
                        block = b;
                        break;
                    }
                    blockOffset += b.length;
                }
                if (block == null) {
                    // should not be possible
                    throw new IllegalStateException("failed to find a block containing offset " + offset);
                }
                copyOffset = blockOffset + block.length - offsetRemaining;
                copyLen = Math.min(lengthRemaining, block.length - copyOffset);
            } else {
                // offsetRemaining is negative or 0 and points into the expanded bytes
                block = expanded;
                copyOffset = -offsetRemaining;
                copyLen = Math.min(lengthRemaining, writeOffset + offsetRemaining);
            }
            System.arraycopy(block, copyOffset, expanded, writeOffset, copyLen);
            offsetRemaining -= copyLen;
            lengthRemaining -= copyLen;
            writeOffset += copyLen;
        }
    }

    private void clearUnusedPairs() {
        int pairLengths = 0;
        int pairsToKeep = 0;
        for (Iterator<Pair> it = pairs.descendingIterator(); it.hasNext(); ) {
            Pair p = it.next();
            pairsToKeep++;
            pairLengths += p.length();
            if (pairLengths >= BlockLZ4CompressorInputStream.WINDOW_SIZE) {
                break;
            }
        }
        final int size = pairs.size();
        for (int i = pairsToKeep; i < size; i++) {
            Pair p = pairs.peekFirst();
            if (p.hasBeenWritten()) {
                pairs.removeFirst();
            } else {
                break;
            }
        }
    }

    private void writeFinalLiteralBlock() throws IOException {
        rewriteLastPairs();
        for (Pair p : pairs) {
            if (!p.hasBeenWritten()) {
                p.writeTo(os);
            }
        }
        pairs.clear();
    }

    private void writeWritablePairs(int lengthOfBlocksAfterLastPair) throws IOException {
        int unwrittenLength = lengthOfBlocksAfterLastPair;
        for (Iterator<Pair> it = pairs.descendingIterator(); it.hasNext(); ) {
            Pair p = it.next();
            if (p.hasBeenWritten()) {
                break;
            }
            unwrittenLength += p.length();
        }
        for (Pair p : pairs) {
            if (p.hasBeenWritten()) {
                continue;
            }
            unwrittenLength -= p.length();
            if (p.canBeWritten(unwrittenLength)) {
                p.writeTo(os);
            } else {
                break;
            }
        }
    }

    private void rewriteLastPairs() {
        LinkedList<Pair> lastPairs = new LinkedList<>();
        LinkedList<Integer> pairLength = new LinkedList<>();
        int offset = 0;
        for (Iterator<Pair> it = pairs.descendingIterator(); it.hasNext(); ) {
            Pair p = it.next();
            if (p.hasBeenWritten()) {
                break;
            }
            int len = p.length();
            pairLength.addFirst(len);
            lastPairs.addFirst(p);
            offset += len;
            if (offset >= MIN_OFFSET_OF_LAST_BACK_REFERENCE) {
                break;
            }
        }
        for (Pair p : lastPairs) {
            pairs.remove(p);
        }
        // lastPairs may contain between one and four Pairs:
        // * the last pair may be a one byte literal
        // * all other Pairs contain a back-reference which must be four bytes long at minimum
        // we could merge them all into a single literal block but
        // this may harm compression. For example compressing
        // "bla.tar" from our tests yields a last block containing a
        // back-reference of length > 2k and we'd end up with a last
        // literal of that size rather than a 2k back-reference and a
        // 12 byte literal at the end.

        // Instead we merge all but the first of lastPairs into a new
        // literal-only Pair "replacement" and look at the
        // back-reference in the first of lastPairs and see if we can
        // split it. We can split it if it is longer than 16 -
        // replacement.length (i.e. the minimal length of four is kept
        // while making sure the last literal is at least twelve bytes
        // long). If we can't split it, we expand the first of the pairs
        // as well.

        // this is not optimal, we could get better compression
        // results with more complex approaches as the last literal
        // only needs to be five bytes long if the previous
        // back-reference has an offset big enough

        final int lastPairsSize = lastPairs.size();
        int toExpand = 0;
        for (int i = 1; i < lastPairsSize; i++) {
            toExpand += pairLength.get(i);
        }
        Pair replacement = new Pair();
        if (toExpand > 0) {
            replacement.prependLiteral(expand(toExpand, toExpand));
        }
        Pair splitCandidate = lastPairs.get(0);
        int stillNeeded = MIN_OFFSET_OF_LAST_BACK_REFERENCE - toExpand;
        int brLen = splitCandidate.hasBackReference() ? splitCandidate.backReferenceLength() : 0;
        if (splitCandidate.hasBackReference() && brLen >= MIN_BACK_REFERENCE_LENGTH + stillNeeded) {
            replacement.prependLiteral(expand(toExpand + stillNeeded, stillNeeded));
            pairs.add(splitCandidate.splitWithNewBackReferenceLengthOf(brLen - stillNeeded));
        } else {
            if (splitCandidate.hasBackReference()) {
                replacement.prependLiteral(expand(toExpand + brLen, brLen));
            }
            splitCandidate.prependTo(replacement);
        }
        pairs.add(replacement);
    }

    /**
     * Returns a builder correctly configured for the LZ4 algorithm.
     * @return a builder correctly configured for the LZ4 algorithm
     */
    public static Parameters.Builder createParameterBuilder() {
        int maxLen = BlockLZ4CompressorInputStream.WINDOW_SIZE - 1;
        return Parameters.builder(BlockLZ4CompressorInputStream.WINDOW_SIZE)
            .withMinBackReferenceLength(MIN_BACK_REFERENCE_LENGTH)
            .withMaxBackReferenceLength(maxLen)
            .withMaxOffset(maxLen)
            .withMaxLiteralLength(maxLen);
    }

    final static class Pair {
        private final Deque<byte[]> literals = new LinkedList<>();
        private int brOffset, brLength;
        private boolean written;

        private void prependLiteral(byte[] data) {
            literals.addFirst(data);
        }
        byte[] addLiteral(LZ77Compressor.LiteralBlock block) {
            byte[] copy = Arrays.copyOfRange(block.getData(), block.getOffset(),
                block.getOffset() + block.getLength());
            literals.add(copy);
            return copy;
        }
        void setBackReference(LZ77Compressor.BackReference block) {
            if (hasBackReference()) {
                throw new IllegalStateException();
            }
            brOffset = block.getOffset();
            brLength = block.getLength();
        }
        boolean hasBackReference() {
            return brOffset > 0;
        }
        boolean canBeWritten(int lengthOfBlocksAfterThisPair) {
            return hasBackReference()
                && lengthOfBlocksAfterThisPair >= MIN_OFFSET_OF_LAST_BACK_REFERENCE + MIN_BACK_REFERENCE_LENGTH;
        }
        int length() {
            return literalLength() + brLength;
        }
        private boolean hasBeenWritten() {
            return written;
        }
        void writeTo(OutputStream out) throws IOException {
            int litLength = literalLength();
            out.write(lengths(litLength, brLength));
            if (litLength >= BlockLZ4CompressorInputStream.BACK_REFERENCE_SIZE_MASK) {
                writeLength(litLength - BlockLZ4CompressorInputStream.BACK_REFERENCE_SIZE_MASK, out);
            }
            for (byte[] b : literals) {
                out.write(b);
            }
            if (hasBackReference()) {
                ByteUtils.toLittleEndian(out, brOffset, 2);
                if (brLength - MIN_BACK_REFERENCE_LENGTH >= BlockLZ4CompressorInputStream.BACK_REFERENCE_SIZE_MASK) {
                    writeLength(brLength - MIN_BACK_REFERENCE_LENGTH
                        - BlockLZ4CompressorInputStream.BACK_REFERENCE_SIZE_MASK, out);
                }
            }
            written = true;
        }
        private int literalLength() {
            int length = 0;
            for (byte[] b : literals) {
                length += b.length;
            }
            return length;
        }
        private static int lengths(int litLength, int brLength) {
            int l = litLength < 15 ? litLength : 15;
            int br = brLength < 4 ? 0 : (brLength < 19 ? brLength - 4 : 15);
            return (l << BlockLZ4CompressorInputStream.SIZE_BITS) | br;
        }
        private static void writeLength(int length, OutputStream out) throws IOException {
            while (length >= 255) {
                out.write(255);
                length -= 255;
            }
            out.write(length);
        }
        private int backReferenceLength() {
            return brLength;
        }
        private void prependTo(Pair other) {
            Iterator<byte[]> listBackwards = literals.descendingIterator();
            while (listBackwards.hasNext()) {
                other.prependLiteral(listBackwards.next());
            }
        }
        private Pair splitWithNewBackReferenceLengthOf(int newBackReferenceLength) {
            Pair p = new Pair();
            p.literals.addAll(literals);
            p.brOffset = brOffset;
            p.brLength = newBackReferenceLength;
            return p;
        }
    }
}
