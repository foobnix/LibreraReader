/*
 * RangeEncoderToBuffer
 *
 * Authors: Lasse Collin <lasse.collin@tukaani.org>
 *          Igor Pavlov <http://7-zip.org/>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */

package org.tukaani.xz.rangecoder;

import java.io.OutputStream;
import java.io.IOException;
import org.tukaani.xz.ArrayCache;

public final class RangeEncoderToBuffer extends RangeEncoder {
    private final byte[] buf;
    private int bufPos;

    public RangeEncoderToBuffer(int bufSize, ArrayCache arrayCache) {
        buf = arrayCache.getByteArray(bufSize, false);
        reset();
    }

    public void putArraysToCache(ArrayCache arrayCache) {
        arrayCache.putArray(buf);
    }

    public void reset() {
        super.reset();
        bufPos = 0;
    }

    public int getPendingSize() {
        // With LZMA2 it is known that cacheSize fits into an int.
        return bufPos + (int)cacheSize + 5 - 1;
    }

    public int finish() {
        // super.finish() cannot throw an IOException because writeByte()
        // provided in this file cannot throw an IOException.
        try {
            super.finish();
        } catch (IOException e) {
            throw new Error();
        }

        return bufPos;
    }

    public void write(OutputStream out) throws IOException {
        out.write(buf, 0, bufPos);
    }

    void writeByte(int b) {
        buf[bufPos++] = (byte)b;
    }
}
