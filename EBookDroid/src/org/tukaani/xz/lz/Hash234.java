/*
 * 2-, 3-, and 4-byte hashing
 *
 * Authors: Lasse Collin <lasse.collin@tukaani.org>
 *          Igor Pavlov <http://7-zip.org/>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */

package org.tukaani.xz.lz;

import org.tukaani.xz.ArrayCache;

final class Hash234 extends CRC32Hash {
    private static final int HASH_2_SIZE = 1 << 10;
    private static final int HASH_2_MASK = HASH_2_SIZE - 1;

    private static final int HASH_3_SIZE = 1 << 16;
    private static final int HASH_3_MASK = HASH_3_SIZE - 1;

    private final int hash4Mask;

    private final int[] hash2Table;
    private final int[] hash3Table;
    private final int[] hash4Table;
    private final int hash4Size;

    private int hash2Value = 0;
    private int hash3Value = 0;
    private int hash4Value = 0;

    static int getHash4Size(int dictSize) {
        int h = dictSize - 1;
        h |= h >>> 1;
        h |= h >>> 2;
        h |= h >>> 4;
        h |= h >>> 8;
        h >>>= 1;
        h |= 0xFFFF;
        if (h > (1 << 24))
            h >>>= 1;

        return h + 1;
    }

    static int getMemoryUsage(int dictSize) {
        // Sizes of the hash arrays + a little extra
        return (HASH_2_SIZE + HASH_3_SIZE + getHash4Size(dictSize))
               / (1024 / 4) + 4;
    }

    Hash234(int dictSize, ArrayCache arrayCache) {
        hash2Table = arrayCache.getIntArray(HASH_2_SIZE, true);
        hash3Table = arrayCache.getIntArray(HASH_3_SIZE, true);

        hash4Size = getHash4Size(dictSize);
        hash4Table = arrayCache.getIntArray(hash4Size, true);
        hash4Mask = hash4Size - 1;
    }

    void putArraysToCache(ArrayCache arrayCache) {
        arrayCache.putArray(hash4Table);
        arrayCache.putArray(hash3Table);
        arrayCache.putArray(hash2Table);
    }

    void calcHashes(byte[] buf, int off) {
        int temp = crcTable[buf[off] & 0xFF] ^ (buf[off + 1] & 0xFF);
        hash2Value = temp & HASH_2_MASK;

        temp ^= (buf[off + 2] & 0xFF) << 8;
        hash3Value = temp & HASH_3_MASK;

        temp ^= crcTable[buf[off + 3] & 0xFF] << 5;
        hash4Value = temp & hash4Mask;
    }

    int getHash2Pos() {
        return hash2Table[hash2Value];
    }

    int getHash3Pos() {
        return hash3Table[hash3Value];
    }

    int getHash4Pos() {
        return hash4Table[hash4Value];
    }

    void updateTables(int pos) {
        hash2Table[hash2Value] = pos;
        hash3Table[hash3Value] = pos;
        hash4Table[hash4Value] = pos;
    }

    void normalize(int normalizeOffset) {
        LZEncoder.normalize(hash2Table, HASH_2_SIZE, normalizeOffset);
        LZEncoder.normalize(hash3Table, HASH_3_SIZE, normalizeOffset);
        LZEncoder.normalize(hash4Table, hash4Size, normalizeOffset);
    }
}
