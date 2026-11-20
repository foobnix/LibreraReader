package com.foobnix.ext.lit;

import java.io.IOException;

/**
 * LZX decompression, ported from libmspack's lzxd.c (Stuart Caie, LGPL 2.1) as calibre uses it
 * for Microsoft Reader (.lit) books: every window-sized chunk of a section is decoded from a fresh
 * state, so one call takes the compressed bytes of one chunk and returns its bytes.
 * <p>
 * The bit buffer is 32 bits wide, as in the C code, with unsigned shifts.
 */
final class LzxDecoder {

    private static final int MIN_MATCH = 2;
    private static final int NUM_CHARS = 256;
    private static final int BLOCKTYPE_INVALID = 0;
    private static final int BLOCKTYPE_VERBATIM = 1;
    private static final int BLOCKTYPE_ALIGNED = 2;
    private static final int BLOCKTYPE_UNCOMPRESSED = 3;
    private static final int NUM_PRIMARY_LENGTHS = 7;
    private static final int NUM_SECONDARY_LENGTHS = 249;
    private static final int FRAME_SIZE = 32768;
    private static final int LENTABLE_SAFETY = 64;

    private static final int PRETREE_MAXSYMBOLS = 20;
    private static final int PRETREE_TABLEBITS = 6;
    private static final int MAINTREE_MAXSYMBOLS = NUM_CHARS + 50 * 8;
    private static final int MAINTREE_TABLEBITS = 12;
    private static final int LENGTH_MAXSYMBOLS = NUM_SECONDARY_LENGTHS + 1;
    private static final int LENGTH_TABLEBITS = 12;
    private static final int ALIGNED_MAXSYMBOLS = 8;
    private static final int ALIGNED_TABLEBITS = 7;

    private static final int[] POSITION_BASE = new int[51];
    private static final int[] EXTRA_BITS = new int[51];

    static {
        for (int i = 0, j = 0; i < 50; i += 2) {
            EXTRA_BITS[i] = j;
            EXTRA_BITS[i + 1] = j;
            if (i != 0 && j < 17) {
                j++;
            }
        }
        EXTRA_BITS[50] = 17;
        for (int i = 0, j = 0; i < 51; i++) {
            POSITION_BASE[i] = j;
            j += 1 << EXTRA_BITS[i];
        }
    }

    static final class LzxException extends IOException {
        LzxException(String message) {
            super(message);
        }
    }

    private final int[] pretreeLen = new int[PRETREE_MAXSYMBOLS + LENTABLE_SAFETY];
    private final int[] maintreeLen = new int[MAINTREE_MAXSYMBOLS + LENTABLE_SAFETY];
    private final int[] lengthLen = new int[LENGTH_MAXSYMBOLS + LENTABLE_SAFETY];
    private final int[] alignedLen = new int[ALIGNED_MAXSYMBOLS + LENTABLE_SAFETY];

    private final int[] pretreeTable = new int[(1 << PRETREE_TABLEBITS) + (PRETREE_MAXSYMBOLS * 2)];
    private final int[] maintreeTable = new int[(1 << MAINTREE_TABLEBITS) + (MAINTREE_MAXSYMBOLS * 2)];
    private final int[] lengthTable = new int[(1 << LENGTH_TABLEBITS) + (LENGTH_MAXSYMBOLS * 2)];
    private final int[] alignedTable = new int[(1 << ALIGNED_TABLEBITS) + (ALIGNED_MAXSYMBOLS * 2)];

    private final int windowSize;
    private final int posnSlots;
    private final byte[] window;

    // input: the chunk followed by the two zero bytes libmspack fakes at the end of the stream
    private byte[] in;
    private int ip;
    private int iend;
    private int bitBuffer;
    private int bitsLeft;

    LzxDecoder(int windowBits) throws LzxException {
        if (windowBits < 15 || windowBits > 21) {
            throw new LzxException("Invalid LZX window " + windowBits);
        }
        windowSize = 1 << windowBits;
        posnSlots = windowBits == 21 ? 50 : (windowBits == 20 ? 42 : (windowBits << 1));
        window = new byte[windowSize];
    }

    /**
     * Decodes one chunk, from a fresh state, into {@code out[outOff .. outOff + outLen)}.
     */
    void decompress(byte[] src, int srcOff, int srcLen, byte[] out, int outOff, int outLen) throws LzxException {
        in = new byte[srcLen + 4];
        System.arraycopy(src, srcOff, in, 0, srcLen);
        ip = 0;
        iend = srcLen + 2;
        bitBuffer = 0;
        bitsLeft = 0;
        try {
            run(out, outOff, outLen);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new LzxException("Corrupt LZX data");
        } finally {
            in = null;
        }
    }

    private void ensureBits(int nbits) throws LzxException {
        while (bitsLeft < nbits) {
            if (ip >= iend) {
                throw new LzxException("Out of LZX input");
            }
            bitBuffer |= (((in[ip + 1] & 0xFF) << 8) | (in[ip] & 0xFF)) << (16 - bitsLeft);
            bitsLeft += 16;
            ip += 2;
        }
    }

    private int peekBits(int nbits) {
        return bitBuffer >>> (32 - nbits);
    }

    private void removeBits(int nbits) {
        bitBuffer <<= nbits;
        bitsLeft -= nbits;
    }

    private int readBits(int nbits) throws LzxException {
        if (nbits == 0) {
            return 0;
        }
        ensureBits(nbits);
        final int v = peekBits(nbits);
        removeBits(nbits);
        return v;
    }

    private int readHuffSym(int[] table, int[] lens, int maxSymbols, int tableBits) throws LzxException {
        ensureBits(16);
        int sym = table[peekBits(tableBits)];
        if (sym >= maxSymbols) {
            int i = 1 << (32 - tableBits);
            do {
                i >>>= 1;
                if (i == 0) {
                    throw new LzxException("Out of bits in huffman decode");
                }
                sym <<= 1;
                sym |= (bitBuffer & i) != 0 ? 1 : 0;
                sym = table[sym];
            } while (sym >= maxSymbols);
        }
        removeBits(lens[sym]);
        return sym;
    }

    private static void makeDecodeTable(int nsyms, int nbits, int[] length, int[] table) throws LzxException {
        int pos = 0;
        int tableMask = 1 << nbits;
        int bitMask = tableMask >>> 1;
        int nextSymbol = bitMask;

        for (int bitNum = 1; bitNum <= nbits; bitNum++) {
            for (int sym = 0; sym < nsyms; sym++) {
                if (length[sym] != bitNum) {
                    continue;
                }
                int leaf = pos;
                if ((pos += bitMask) > tableMask) {
                    throw new LzxException("Huffman table overrun");
                }
                for (int fill = bitMask; fill-- > 0; ) {
                    table[leaf++] = sym;
                }
            }
            bitMask >>>= 1;
        }

        if (pos == tableMask) {
            return;
        }

        for (int sym = pos; sym < tableMask; sym++) {
            table[sym] = 0xFFFF;
        }

        pos <<= 16;
        tableMask <<= 16;
        bitMask = 1 << 15;

        for (int bitNum = nbits + 1; bitNum <= 16; bitNum++) {
            for (int sym = 0; sym < nsyms; sym++) {
                if (length[sym] != bitNum) {
                    continue;
                }
                int leaf = pos >>> 16;
                for (int fill = 0; fill < bitNum - nbits; fill++) {
                    if (table[leaf] == 0xFFFF) {
                        table[nextSymbol << 1] = 0xFFFF;
                        table[(nextSymbol << 1) + 1] = 0xFFFF;
                        table[leaf] = nextSymbol++;
                    }
                    leaf = table[leaf] << 1;
                    if (((pos >>> (15 - fill)) & 1) != 0) {
                        leaf++;
                    }
                }
                table[leaf] = sym;
                if ((pos += bitMask) > tableMask) {
                    throw new LzxException("Huffman table overflow");
                }
            }
            bitMask >>>= 1;
        }

        if (pos == tableMask) {
            return;
        }
        for (int sym = 0; sym < nsyms; sym++) {
            if (length[sym] != 0) {
                throw new LzxException("Bad huffman table");
            }
        }
    }

    private void readLengths(int[] lens, int first, int last) throws LzxException {
        for (int x = 0; x < 20; x++) {
            pretreeLen[x] = readBits(4);
        }
        makeDecodeTable(PRETREE_MAXSYMBOLS, PRETREE_TABLEBITS, pretreeLen, pretreeTable);

        for (int x = first; x < last; ) {
            int z = readHuffSym(pretreeTable, pretreeLen, PRETREE_MAXSYMBOLS, PRETREE_TABLEBITS);
            if (z == 17) {
                int y = readBits(4) + 4;
                while (y-- > 0) {
                    lens[x++] = 0;
                }
            } else if (z == 18) {
                int y = readBits(5) + 20;
                while (y-- > 0) {
                    lens[x++] = 0;
                }
            } else if (z == 19) {
                int y = readBits(1) + 4;
                z = readHuffSym(pretreeTable, pretreeLen, PRETREE_MAXSYMBOLS, PRETREE_TABLEBITS);
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

    private void run(byte[] out, int outOff, int length) throws LzxException {
        int r0 = 1, r1 = 1, r2 = 1;
        boolean headerRead = false;
        int blockRemaining = 0;
        int blockLength = 0;
        int blockType = BLOCKTYPE_INVALID;
        int intelFilesize = 0;
        int intelCurpos = 0;
        boolean intelStarted = false;
        int windowPosn = 0;
        int framePosn = 0;
        int frame = 0;
        int offset = 0;

        for (int i = 0; i < MAINTREE_MAXSYMBOLS; i++) {
            maintreeLen[i] = 0;
        }
        for (int i = 0; i < LENGTH_MAXSYMBOLS; i++) {
            lengthLen[i] = 0;
        }

        final int endFrame = length / FRAME_SIZE + 1;
        while (frame < endFrame && offset < length) {
            if (!headerRead) {
                int i = readBits(1);
                int j = 0;
                if (i != 0) {
                    i = readBits(16);
                    j = readBits(16);
                }
                intelFilesize = (i << 16) | j;
                headerRead = true;
            }

            int frameSize = FRAME_SIZE;
            if (length - offset < frameSize) {
                frameSize = length - offset;
            }

            int bytesTodo = framePosn + frameSize - windowPosn;
            while (bytesTodo > 0) {
                if (blockRemaining == 0) {
                    if (blockType == BLOCKTYPE_UNCOMPRESSED && (blockLength & 1) != 0) {
                        if (ip >= iend) {
                            throw new LzxException("Out of LZX input");
                        }
                        ip++;
                    }
                    blockType = readBits(3);
                    int i = readBits(16);
                    int j = readBits(8);
                    blockRemaining = blockLength = (i << 8) | j;

                    switch (blockType) {
                        case BLOCKTYPE_ALIGNED:
                            for (i = 0; i < 8; i++) {
                                alignedLen[i] = readBits(3);
                            }
                            makeDecodeTable(ALIGNED_MAXSYMBOLS, ALIGNED_TABLEBITS, alignedLen, alignedTable);
                            // fall through: the rest of the header is a verbatim one
                        case BLOCKTYPE_VERBATIM:
                            readLengths(maintreeLen, 0, 256);
                            readLengths(maintreeLen, 256, NUM_CHARS + (posnSlots << 3));
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
                                ip -= 2;
                            }
                            bitsLeft = 0;
                            bitBuffer = 0;
                            final int[] buf = new int[12];
                            for (i = 0; i < 12; i++) {
                                if (ip >= iend) {
                                    throw new LzxException("Out of LZX input");
                                }
                                buf[i] = in[ip++] & 0xFF;
                            }
                            r0 = buf[0] | (buf[1] << 8) | (buf[2] << 16) | (buf[3] << 24);
                            r1 = buf[4] | (buf[5] << 8) | (buf[6] << 16) | (buf[7] << 24);
                            r2 = buf[8] | (buf[9] << 8) | (buf[10] << 16) | (buf[11] << 24);
                            break;
                        default:
                            throw new LzxException("Bad LZX block type " + blockType);
                    }
                }

                int thisRun = blockRemaining;
                if (thisRun > bytesTodo) {
                    thisRun = bytesTodo;
                }
                bytesTodo -= thisRun;
                blockRemaining -= thisRun;

                if (blockType == BLOCKTYPE_VERBATIM || blockType == BLOCKTYPE_ALIGNED) {
                    final boolean aligned = blockType == BLOCKTYPE_ALIGNED;
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
                        switch (matchOffset) {
                            case 0:
                                matchOffset = r0;
                                break;
                            case 1:
                                matchOffset = r1;
                                r1 = r0;
                                r0 = matchOffset;
                                break;
                            case 2:
                                matchOffset = r2;
                                r2 = r0;
                                r0 = matchOffset;
                                break;
                            default:
                                if (!aligned && matchOffset == 3) {
                                    matchOffset = 1;
                                } else if (!aligned) {
                                    final int extra = EXTRA_BITS[matchOffset];
                                    matchOffset = POSITION_BASE[matchOffset] - 2 + readBits(extra);
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
                        }

                        if (windowPosn + matchLength > windowSize) {
                            throw new LzxException("Match ran over window wrap");
                        }

                        int dest = windowPosn;
                        int count = matchLength;
                        if (matchOffset > windowPosn || matchOffset < 0) {
                            int j = matchOffset - windowPosn;
                            if (j > windowSize || j < 0) {
                                throw new LzxException("Match offset beyond window boundaries");
                            }
                            int src = windowSize - j;
                            if (j < count) {
                                count -= j;
                                while (j-- > 0) {
                                    window[dest++] = window[src++];
                                }
                                src = 0;
                            }
                            while (count-- > 0) {
                                window[dest++] = window[src++];
                            }
                        } else {
                            int src = dest - matchOffset;
                            while (count-- > 0) {
                                window[dest++] = window[src++];
                            }
                        }
                        thisRun -= matchLength;
                        windowPosn += matchLength;
                    }
                } else if (blockType == BLOCKTYPE_UNCOMPRESSED) {
                    int dest = windowPosn;
                    windowPosn += thisRun;
                    while (thisRun > 0) {
                        int avail = iend - ip;
                        if (avail <= 0) {
                            throw new LzxException("Out of LZX input");
                        }
                        if (avail > thisRun) {
                            avail = thisRun;
                        }
                        System.arraycopy(in, ip, window, dest, avail);
                        dest += avail;
                        ip += avail;
                        thisRun -= avail;
                    }
                } else {
                    throw new LzxException("Bad LZX block type " + blockType);
                }

                if (thisRun < 0) {
                    if (-thisRun > blockRemaining) {
                        throw new LzxException("Overrun past end of block");
                    }
                    blockRemaining -= -thisRun;
                }
            }

            // re-align the input bitstream
            if (bitsLeft > 0) {
                ensureBits(16);
            }
            if ((bitsLeft & 15) != 0) {
                removeBits(bitsLeft & 15);
            }

            final int at = outOff + offset;
            System.arraycopy(window, framePosn, out, at, frameSize);
            if (intelStarted && intelFilesize != 0 && frame <= 32768 && frameSize > 10) {
                int curpos = intelCurpos;
                int p = at;
                final int end = at + frameSize - 10;
                while (p < end) {
                    if ((out[p++] & 0xFF) != 0xE8) {
                        curpos++;
                        continue;
                    }
                    final int absOff = (out[p] & 0xFF) | ((out[p + 1] & 0xFF) << 8) | ((out[p + 2] & 0xFF) << 16) | ((out[p + 3] & 0xFF) << 24);
                    if (absOff >= -curpos && absOff < intelFilesize) {
                        final int relOff = absOff >= 0 ? absOff - curpos : absOff + intelFilesize;
                        out[p] = (byte) relOff;
                        out[p + 1] = (byte) (relOff >>> 8);
                        out[p + 2] = (byte) (relOff >>> 16);
                        out[p + 3] = (byte) (relOff >>> 24);
                    }
                    p += 4;
                    curpos += 5;
                }
                intelCurpos += frameSize;
            } else if (intelFilesize != 0) {
                intelCurpos += frameSize;
            }

            offset += frameSize;
            framePosn += frameSize;
            frame++;

            if (windowPosn == windowSize) {
                windowPosn = 0;
            }
            if (framePosn == windowSize) {
                framePosn = 0;
            }
        }

        if (offset < length) {
            throw new LzxException("LZX bytes left to output");
        }
    }
}
