package com.foobnix.ext.chm;

import java.io.IOException;

/**
 * LZX decompression as a CHM file uses it, after chmlib's lzx.c (Stuart Caie's decoder from
 * cabextract, LGPL).
 * <p>
 * A CHM keeps its compressed files in one LZX stream cut into blocks of 32 KB of output. The
 * stream is reset every few blocks, and each block's compressed bytes start at a byte offset the
 * reset table gives, so a block is decoded by one {@link #decompress} call that starts a fresh bit
 * reader at that offset, while the window, the repeated offsets and the Huffman trees carry on from
 * the block before - until the next {@link #reset()}.
 * <p>
 * The bit buffer is 32 bits wide and filled 16 bits at a time, as in the C code; every shift is
 * unsigned.
 */
final class ChmLzx {

    private static final int MIN_MATCH = 2;
    private static final int NUM_CHARS = 256;
    private static final int BLOCKTYPE_INVALID = 0;
    private static final int BLOCKTYPE_VERBATIM = 1;
    private static final int BLOCKTYPE_ALIGNED = 2;
    private static final int BLOCKTYPE_UNCOMPRESSED = 3;
    private static final int NUM_PRIMARY_LENGTHS = 7;
    private static final int NUM_SECONDARY_LENGTHS = 249;
    private static final int LENTABLE_SAFETY = 64;

    private static final int PRETREE_MAXSYMBOLS = 20;
    private static final int PRETREE_TABLEBITS = 6;
    private static final int MAINTREE_MAXSYMBOLS = NUM_CHARS + 50 * 8;
    private static final int MAINTREE_TABLEBITS = 12;
    private static final int LENGTH_MAXSYMBOLS = NUM_SECONDARY_LENGTHS + 1;
    private static final int LENGTH_TABLEBITS = 12;
    private static final int ALIGNED_MAXSYMBOLS = 8;
    private static final int ALIGNED_TABLEBITS = 7;

    private static final int[] EXTRA_BITS = new int[52];
    private static final int[] POSITION_BASE = new int[51];

    static {
        for (int i = 0, j = 0; i <= 50; i += 2) {
            EXTRA_BITS[i] = EXTRA_BITS[i + 1] = j;
            if (i != 0 && j < 17) {
                j++;
            }
        }
        for (int i = 0, j = 0; i <= 50; i++) {
            POSITION_BASE[i] = j;
            j += 1 << EXTRA_BITS[i];
        }
    }

    private final byte[] window;
    private final int windowSize;
    private final int mainElements;

    private int r0, r1, r2;
    private boolean headerRead;
    private int blockType;
    private int blockLength;
    private int blockRemaining;
    private int framesRead;
    private int intelFileSize;
    private int intelCurPos;
    private boolean intelStarted;
    private int windowPosn;

    private final int[] pretreeTable = new int[(1 << PRETREE_TABLEBITS) + (PRETREE_MAXSYMBOLS << 1)];
    private final int[] pretreeLen = new int[PRETREE_MAXSYMBOLS + LENTABLE_SAFETY];
    private final int[] maintreeTable = new int[(1 << MAINTREE_TABLEBITS) + (MAINTREE_MAXSYMBOLS << 1)];
    private final int[] maintreeLen = new int[MAINTREE_MAXSYMBOLS + LENTABLE_SAFETY];
    private final int[] lengthTable = new int[(1 << LENGTH_TABLEBITS) + (LENGTH_MAXSYMBOLS << 1)];
    private final int[] lengthLen = new int[LENGTH_MAXSYMBOLS + LENTABLE_SAFETY];
    private final int[] alignedTable = new int[(1 << ALIGNED_TABLEBITS) + (ALIGNED_MAXSYMBOLS << 1)];
    private final int[] alignedLen = new int[ALIGNED_MAXSYMBOLS + LENTABLE_SAFETY];

    // The bit reader of the current call
    private byte[] in;
    private int inPos;
    private int inEnd;
    private int bitBuf;
    private int bitsLeft;

    /**
     * @param windowBits 15 to 21, the log2 of the window size
     */
    ChmLzx(final int windowBits) throws IOException {
        if (windowBits < 15 || windowBits > 21) {
            throw new IOException("LZX window bits " + windowBits);
        }
        windowSize = 1 << windowBits;
        window = new byte[windowSize];
        final int posnSlots = windowBits == 20 ? 42 : windowBits == 21 ? 50 : windowBits << 1;
        mainElements = NUM_CHARS + (posnSlots << 3);
        reset();
    }

    void reset() {
        r0 = r1 = r2 = 1;
        headerRead = false;
        framesRead = 0;
        blockRemaining = 0;
        blockType = BLOCKTYPE_INVALID;
        intelCurPos = 0;
        intelStarted = false;
        windowPosn = 0;
        for (int i = 0; i < maintreeLen.length; i++) {
            maintreeLen[i] = 0;
        }
        for (int i = 0; i < lengthLen.length; i++) {
            lengthLen[i] = 0;
        }
    }

    // ------------------------------------------------------------------ bits

    private void initBitstream() {
        bitBuf = 0;
        bitsLeft = 0;
    }

    /** The bytes past the end read as zeros, as the padded C buffer does; a stream far past it is broken. */
    private int inByte(final int pos) throws IOException {
        if (pos < inEnd) {
            return in[pos] & 0xFF;
        }
        if (pos > inEnd + 32) {
            throw new IOException("LZX read past the end of the block");
        }
        return 0;
    }

    private void ensureBits(final int n) throws IOException {
        while (bitsLeft < n) {
            final int word = (inByte(inPos + 1) << 8) | inByte(inPos);
            bitBuf |= word << (32 - 16 - bitsLeft);
            bitsLeft += 16;
            inPos += 2;
        }
    }

    private int peekBits(final int n) {
        return bitBuf >>> (32 - n);
    }

    private void removeBits(final int n) {
        bitBuf <<= n;
        bitsLeft -= n;
    }

    private int readBits(final int n) throws IOException {
        ensureBits(n);
        final int v = peekBits(n);
        removeBits(n);
        return v;
    }

    private int readHuffSym(final int[] table, final int[] lens, final int maxSymbols, final int tableBits) throws IOException {
        ensureBits(16);
        int i = table[peekBits(tableBits)];
        if (i >= maxSymbols) {
            int j = 1 << (32 - tableBits);
            do {
                j >>>= 1;
                i <<= 1;
                i |= (bitBuf & j) != 0 ? 1 : 0;
                if (j == 0) {
                    throw new IOException("LZX bad Huffman code");
                }
                if (i >= table.length) {
                    throw new IOException("LZX Huffman code out of the table");
                }
            } while ((i = table[i]) >= maxSymbols);
        }
        removeBits(lens[i]);
        return i;
    }

    // ------------------------------------------------------------------ tables

    private static void makeDecodeTable(final int nsyms, final int nbits, final int[] length, final int[] table) throws IOException {
        int bitNum = 1;
        int pos = 0;
        int tableMask = 1 << nbits;
        int bitMask = tableMask >>> 1;
        int nextSymbol = bitMask;

        while (bitNum <= nbits) {
            for (int sym = 0; sym < nsyms; sym++) {
                if (length[sym] == bitNum) {
                    int leaf = pos;
                    if ((pos += bitMask) > tableMask) {
                        throw new IOException("LZX table overrun");
                    }
                    for (int fill = bitMask; fill > 0; fill--) {
                        table[leaf++] = sym;
                    }
                }
            }
            bitMask >>>= 1;
            bitNum++;
        }

        if (pos != tableMask) {
            for (int sym = pos; sym < tableMask; sym++) {
                table[sym] = 0;
            }
            // Room for codes to grow by up to 16 more bits; long arithmetic keeps it unsigned
            long lpos = ((long) pos) << 16;
            final long ltableMask = ((long) tableMask) << 16;
            int lbitMask = 1 << 15;
            while (bitNum <= 16) {
                for (int sym = 0; sym < nsyms; sym++) {
                    if (length[sym] == bitNum) {
                        int leaf = (int) (lpos >>> 16);
                        for (int fill = 0; fill < bitNum - nbits; fill++) {
                            if (table[leaf] == 0) {
                                if ((nextSymbol << 1) + 1 >= table.length) {
                                    throw new IOException("LZX table overflow");
                                }
                                table[nextSymbol << 1] = 0;
                                table[(nextSymbol << 1) + 1] = 0;
                                table[leaf] = nextSymbol++;
                            }
                            leaf = table[leaf] << 1;
                            if (((lpos >>> (15 - fill)) & 1) != 0) {
                                leaf++;
                            }
                        }
                        table[leaf] = sym;
                        if ((lpos += lbitMask) > ltableMask) {
                            throw new IOException("LZX table overflow");
                        }
                    }
                }
                lbitMask >>>= 1;
                bitNum++;
            }
            if (lpos == ltableMask) {
                return;
            }
        } else {
            return;
        }
        // Either a broken table or every length is 0
        for (int sym = 0; sym < nsyms; sym++) {
            if (length[sym] != 0) {
                throw new IOException("LZX bad table");
            }
        }
    }

    private void readLengths(final int[] lens, final int first, final int last) throws IOException {
        for (int x = 0; x < 20; x++) {
            pretreeLen[x] = readBits(4);
        }
        makeDecodeTable(PRETREE_MAXSYMBOLS, PRETREE_TABLEBITS, pretreeLen, pretreeTable);

        int x = first;
        while (x < last) {
            int z = readHuffSym(pretreeTable, pretreeLen, PRETREE_MAXSYMBOLS, PRETREE_TABLEBITS);
            if (z == 17) {
                int y = readBits(4) + 4;
                checkRun(lens, x, y);
                while (y-- > 0) {
                    lens[x++] = 0;
                }
            } else if (z == 18) {
                int y = readBits(5) + 20;
                checkRun(lens, x, y);
                while (y-- > 0) {
                    lens[x++] = 0;
                }
            } else if (z == 19) {
                int y = readBits(1) + 4;
                z = readHuffSym(pretreeTable, pretreeLen, PRETREE_MAXSYMBOLS, PRETREE_TABLEBITS);
                checkRun(lens, x, y);
                z = lens[x] - z;
                if (z < 0) {
                    z += 17;
                }
                while (y-- > 0) {
                    lens[x++] = z;
                }
            } else {
                z = lens[x] - z;
                if (z < 0) {
                    z += 17;
                }
                lens[x++] = z;
            }
        }
    }

    private static void checkRun(final int[] lens, final int x, final int run) throws IOException {
        if (x + run > lens.length) {
            throw new IOException("LZX length run past the table");
        }
    }

    // ------------------------------------------------------------------ decoding

    /**
     * Decodes one block.
     *
     * @param src    holds the block's compressed bytes at {@code [srcOff, srcOff + srcLen)}
     * @param out    receives {@code outLen} bytes at {@code outOff}
     * @param outLen 32 KB but for the last block of the stream, and never more than the window
     */
    void decompress(final byte[] src, final int srcOff, final int srcLen, final byte[] out, final int outOff, final int outLen) throws IOException {
        begin(src, srcOff, srcLen);
        frame(out, outOff, outLen);
    }

    /** Starts reading compressed bytes at {@code [srcOff, srcOff + srcLen)} of {@code src}. */
    void begin(final byte[] src, final int srcOff, final int srcLen) {
        in = src;
        inPos = srcOff;
        inEnd = srcOff + srcLen;
        initBitstream();
    }

    /** Where the next frame starts in the input, once a frame has been decoded. */
    int position() {
        return inPos - (bitsLeft >> 3);
    }

    /**
     * Decodes the next frame of the input {@link #begin} gave, and leaves the bit reader on the
     * 16-bit boundary the next frame starts at.
     */
    void frame(final byte[] out, final int outOff, final int outLen) throws IOException {
        if (outLen > windowSize) {
            throw new IOException("LZX block larger than the window");
        }

        if (!headerRead) {
            int i = 0;
            int j = 0;
            if (readBits(1) != 0) {
                i = readBits(16);
                j = readBits(16);
            }
            intelFileSize = (i << 16) | j;
            headerRead = true;
        }

        int togo = outLen;
        while (togo > 0) {
            if (blockRemaining == 0) {
                if (blockType == BLOCKTYPE_UNCOMPRESSED) {
                    if ((blockLength & 1) != 0) {
                        inPos++;
                    }
                    initBitstream();
                }
                blockType = readBits(3);
                final int i = readBits(16);
                final int j = readBits(8);
                blockRemaining = blockLength = (i << 8) | j;

                switch (blockType) {
                    case BLOCKTYPE_ALIGNED:
                        for (int k = 0; k < 8; k++) {
                            alignedLen[k] = readBits(3);
                        }
                        makeDecodeTable(ALIGNED_MAXSYMBOLS, ALIGNED_TABLEBITS, alignedLen, alignedTable);
                        // fall through: the rest of the header is the verbatim one
                    case BLOCKTYPE_VERBATIM:
                        readLengths(maintreeLen, 0, 256);
                        readLengths(maintreeLen, 256, mainElements);
                        makeDecodeTable(MAINTREE_MAXSYMBOLS, MAINTREE_TABLEBITS, maintreeLen, maintreeTable);
                        if (maintreeLen[0xE8] != 0) {
                            intelStarted = true;
                        }
                        readLengths(lengthLen, 0, NUM_SECONDARY_LENGTHS);
                        makeDecodeTable(LENGTH_MAXSYMBOLS, LENGTH_TABLEBITS, lengthLen, lengthTable);
                        break;
                    case BLOCKTYPE_UNCOMPRESSED:
                        intelStarted = true;
                        ensureBits(16);
                        if (bitsLeft > 16) {
                            inPos -= 2;
                        }
                        if (inPos + 12 > inEnd) {
                            throw new IOException("LZX uncompressed header past the end");
                        }
                        r0 = le32(in, inPos);
                        r1 = le32(in, inPos + 4);
                        r2 = le32(in, inPos + 8);
                        inPos += 12;
                        break;
                    default:
                        throw new IOException("LZX block type " + blockType);
                }
            }

            if (inPos > inEnd) {
                if (inPos > inEnd + 2 || bitsLeft < 16) {
                    throw new IOException("LZX ran out of input");
                }
            }

            int thisRun;
            while ((thisRun = blockRemaining) > 0 && togo > 0) {
                if (thisRun > togo) {
                    thisRun = togo;
                }
                togo -= thisRun;
                blockRemaining -= thisRun;

                windowPosn &= windowSize - 1;
                if (windowPosn + thisRun > windowSize) {
                    throw new IOException("LZX run past the window");
                }

                switch (blockType) {
                    case BLOCKTYPE_VERBATIM:
                    case BLOCKTYPE_ALIGNED:
                        thisRun = decodeRun(thisRun, blockType == BLOCKTYPE_ALIGNED);
                        // A match may run on past this run, which the next one makes up for
                        if (thisRun < 0) {
                            if (-thisRun > blockRemaining) {
                                throw new IOException("LZX match past the block");
                            }
                            blockRemaining -= -thisRun;
                            togo -= -thisRun;
                        }
                        break;
                    case BLOCKTYPE_UNCOMPRESSED:
                        if (inPos + thisRun > inEnd) {
                            throw new IOException("LZX uncompressed run past the end");
                        }
                        System.arraycopy(in, inPos, window, windowPosn, thisRun);
                        inPos += thisRun;
                        windowPosn += thisRun;
                        break;
                    default:
                        throw new IOException("LZX bad block");
                }
            }
        }

        if (togo != 0) {
            throw new IOException("LZX decoded more than the block");
        }
        // Frames start on a 16-bit boundary of the stream
        if (bitsLeft > 0) {
            ensureBits(16);
        }
        if ((bitsLeft & 15) != 0) {
            removeBits(bitsLeft & 15);
        }
        int start = (windowPosn == 0 ? windowSize : windowPosn) - outLen;
        System.arraycopy(window, start, out, outOff, outLen);

        if (framesRead++ < 32768 && intelFileSize != 0) {
            if (outLen <= 6 || !intelStarted) {
                intelCurPos += outLen;
            } else {
                intelE8(out, outOff, outLen);
            }
        }
    }

    /**
     * Decodes {@code thisRun} bytes of a verbatim or aligned block into the window.
     *
     * @return 0, or minus the bytes the last match ran past the run
     */
    private int decodeRun(int thisRun, final boolean aligned) throws IOException {
        while (thisRun > 0) {
            int mainElement = readHuffSym(maintreeTable, maintreeLen, MAINTREE_MAXSYMBOLS, MAINTREE_TABLEBITS);
            if (mainElement < NUM_CHARS) {
                window[windowPosn++] = (byte) mainElement;
                thisRun--;
                continue;
            }
            mainElement -= NUM_CHARS;
            int matchLength = mainElement & NUM_PRIMARY_LENGTHS;
            if (matchLength == NUM_PRIMARY_LENGTHS) {
                matchLength += readHuffSym(lengthTable, lengthLen, LENGTH_MAXSYMBOLS, LENGTH_TABLEBITS);
            }
            matchLength += MIN_MATCH;

            int matchOffset = mainElement >>> 3;
            if (matchOffset > 2) {
                if (!aligned) {
                    if (matchOffset != 3) {
                        final int extra = EXTRA_BITS[matchOffset];
                        matchOffset = POSITION_BASE[matchOffset] - 2 + readBits(extra);
                    } else {
                        matchOffset = 1;
                    }
                } else {
                    int extra = EXTRA_BITS[matchOffset];
                    matchOffset = POSITION_BASE[matchOffset] - 2;
                    if (extra > 3) {
                        extra -= 3;
                        matchOffset += readBits(extra) << 3;
                        matchOffset += readHuffSym(alignedTable, alignedLen, ALIGNED_MAXSYMBOLS, ALIGNED_TABLEBITS);
                    } else if (extra == 3) {
                        matchOffset += readHuffSym(alignedTable, alignedLen, ALIGNED_MAXSYMBOLS, ALIGNED_TABLEBITS);
                    } else if (extra > 0) {
                        matchOffset += readBits(extra);
                    } else {
                        matchOffset = 1;
                    }
                }
                r2 = r1;
                r1 = r0;
                r0 = matchOffset;
            } else if (matchOffset == 0) {
                matchOffset = r0;
            } else if (matchOffset == 1) {
                matchOffset = r1;
                r1 = r0;
                r0 = matchOffset;
            } else {
                matchOffset = r2;
                r2 = r0;
                r0 = matchOffset;
            }
            if (matchOffset <= 0 || matchOffset > windowSize) {
                throw new IOException("LZX bad match offset");
            }
            if (windowPosn + matchLength > windowSize) {
                throw new IOException("LZX match past the window");
            }

            thisRun -= matchLength;
            int dest = windowPosn;
            int src;
            int length = matchLength;
            if (windowPosn >= matchOffset) {
                src = dest - matchOffset;
            } else {
                src = dest + (windowSize - matchOffset);
                int copyLength = matchOffset - windowPosn;
                if (copyLength < length) {
                    length -= copyLength;
                    while (copyLength-- > 0) {
                        window[dest++] = window[src++];
                    }
                    src = 0;
                }
            }
            while (length-- > 0) {
                window[dest++] = window[src++];
            }
            windowPosn += matchLength;
        }
        return thisRun;
    }

    private void intelE8(final byte[] out, final int off, final int len) {
        int data = off;
        final int dataEnd = off + len - 10;
        int curpos = intelCurPos;
        final int filesize = intelFileSize;
        intelCurPos = curpos + len;
        while (data < dataEnd) {
            if ((out[data++] & 0xFF) != 0xE8) {
                curpos++;
                continue;
            }
            final int absOff = le32(out, data);
            if (absOff >= -curpos && absOff < filesize) {
                final int relOff = absOff >= 0 ? absOff - curpos : absOff + filesize;
                out[data] = (byte) relOff;
                out[data + 1] = (byte) (relOff >>> 8);
                out[data + 2] = (byte) (relOff >>> 16);
                out[data + 3] = (byte) (relOff >>> 24);
            }
            data += 4;
            curpos += 5;
        }
    }

    private static int le32(final byte[] b, final int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
    }
}
