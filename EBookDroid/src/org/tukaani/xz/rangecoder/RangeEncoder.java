/*
 * RangeEncoder
 *
 * Authors: Lasse Collin <lasse.collin@tukaani.org>
 *          Igor Pavlov <http://7-zip.org/>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */

package org.tukaani.xz.rangecoder;

import java.io.IOException;

public abstract class RangeEncoder extends RangeCoder {
    private static final int MOVE_REDUCING_BITS = 4;
    private static final int BIT_PRICE_SHIFT_BITS = 4;

    private static final int[] prices
            = new int[BIT_MODEL_TOTAL >>> MOVE_REDUCING_BITS];

    private long low;
    private int range;

    // NOTE: int is OK for LZMA2 because a compressed chunk
    // is not more than 64 KiB, but with LZMA1 there is no chunking
    // so in theory cacheSize can grow very big. To be very safe,
    // use long instead of int since this code is used for LZMA1 too.
    long cacheSize;
    private byte cache;

    static {
        for (int i = (1 << MOVE_REDUCING_BITS) / 2; i < BIT_MODEL_TOTAL;
                i += (1 << MOVE_REDUCING_BITS)) {
            int w = i;
            int bitCount = 0;

            for (int j = 0; j < BIT_PRICE_SHIFT_BITS; ++j) {
                w *= w;
                bitCount <<= 1;

                while ((w & 0xFFFF0000) != 0) {
                    w >>>= 1;
                    ++bitCount;
                }
            }

            prices[i >> MOVE_REDUCING_BITS]
                    = (BIT_MODEL_TOTAL_BITS << BIT_PRICE_SHIFT_BITS)
                      - 15 - bitCount;
        }
    }

    public void reset() {
        low = 0;
        range = 0xFFFFFFFF;
        cache = 0x00;
        cacheSize = 1;
    }

    public int getPendingSize() {
        // This function is only needed by users of RangeEncoderToBuffer,
        // but providing a must-be-never-called version here makes
        // LZMAEncoder simpler.
        throw new Error();
    }

    public int finish() throws IOException {
        for (int i = 0; i < 5; ++i)
            shiftLow();

        // RangeEncoderToBuffer.finish() needs a return value to tell
        // how big the finished buffer is. RangeEncoderToStream has no
        // buffer and thus no return value is needed. Here we use a dummy
        // value which can be overriden in RangeEncoderToBuffer.finish().
        return -1;
    }

    abstract void writeByte(int b) throws IOException;

    private void shiftLow() throws IOException {
        int lowHi = (int)(low >>> 32);

        if (lowHi != 0 || low < 0xFF000000L) {
            int temp = cache;

            do {
                writeByte(temp + lowHi);
                temp = 0xFF;
            } while (--cacheSize != 0);

            cache = (byte)(low >>> 24);
        }

        ++cacheSize;
        low = (low & 0x00FFFFFF) << 8;
    }

    public void encodeBit(short[] probs, int index, int bit)
            throws IOException {
        int prob = probs[index];
        int bound = (range >>> BIT_MODEL_TOTAL_BITS) * prob;

        // NOTE: Any non-zero value for bit is taken as 1.
        if (bit == 0) {
            range = bound;
            probs[index] = (short)(
                    prob + ((BIT_MODEL_TOTAL - prob) >>> MOVE_BITS));
        } else {
            low += bound & 0xFFFFFFFFL;
            range -= bound;
            probs[index] = (short)(prob - (prob >>> MOVE_BITS));
        }

        if ((range & TOP_MASK) == 0) {
            range <<= SHIFT_BITS;
            shiftLow();
        }
    }

    public static int getBitPrice(int prob, int bit) {
        // NOTE: Unlike in encodeBit(), here bit must be 0 or 1.
        assert bit == 0 || bit == 1;
        return prices[(prob ^ ((-bit) & (BIT_MODEL_TOTAL - 1)))
                      >>> MOVE_REDUCING_BITS];
    }

    public void encodeBitTree(short[] probs, int symbol) throws IOException {
        int index = 1;
        int mask = probs.length;

        do {
            mask >>>= 1;
            int bit = symbol & mask;
            encodeBit(probs, index, bit);

            index <<= 1;
            if (bit != 0)
                index |= 1;

        } while (mask != 1);
    }

    public static int getBitTreePrice(short[] probs, int symbol) {
        int price = 0;
        symbol |= probs.length;

        do {
            int bit = symbol & 1;
            symbol >>>= 1;
            price += getBitPrice(probs[symbol], bit);
        } while (symbol != 1);

        return price;
    }

    public void encodeReverseBitTree(short[] probs, int symbol)
            throws IOException {
        int index = 1;
        symbol |= probs.length;

        do {
            int bit = symbol & 1;
            symbol >>>= 1;
            encodeBit(probs, index, bit);
            index = (index << 1) | bit;
        } while (symbol != 1);
    }

    public static int getReverseBitTreePrice(short[] probs, int symbol) {
        int price = 0;
        int index = 1;
        symbol |= probs.length;

        do {
            int bit = symbol & 1;
            symbol >>>= 1;
            price += getBitPrice(probs[index], bit);
            index = (index << 1) | bit;
        } while (symbol != 1);

        return price;
    }

    public void encodeDirectBits(int value, int count) throws IOException {
        do {
            range >>>= 1;
            low += range & (0 - ((value >>> --count) & 1));

            if ((range & TOP_MASK) == 0) {
                range <<= SHIFT_BITS;
                shiftLow();
            }
        } while (count != 0);
    }

    public static int getDirectBitsPrice(int count) {
        return count << BIT_PRICE_SHIFT_BITS;
    }
}
