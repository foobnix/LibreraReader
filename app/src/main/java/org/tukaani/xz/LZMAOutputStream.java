/*
 * LZMAOutputStream
 *
 * Authors: Lasse Collin <lasse.collin@tukaani.org>
 *          Igor Pavlov <http://7-zip.org/>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */

package org.tukaani.xz;

import java.io.OutputStream;
import java.io.IOException;
import org.tukaani.xz.lz.LZEncoder;
import org.tukaani.xz.rangecoder.RangeEncoderToStream;
import org.tukaani.xz.lzma.LZMAEncoder;

/**
 * Compresses into the legacy .lzma file format or into a raw LZMA stream.
 *
 * @since 1.6
 */
public class LZMAOutputStream extends FinishableOutputStream {
    private OutputStream out;

    private final ArrayCache arrayCache;

    private LZEncoder lz;
    private final RangeEncoderToStream rc;
    private LZMAEncoder lzma;

    private final int props;
    private final boolean useEndMarker;
    private final long expectedUncompressedSize;
    private long currentUncompressedSize = 0;

    private boolean finished = false;
    private IOException exception = null;

    private final byte[] tempBuf = new byte[1];

    private LZMAOutputStream(OutputStream out, LZMA2Options options,
                             boolean useHeader, boolean useEndMarker,
                             long expectedUncompressedSize,
                             ArrayCache arrayCache)
            throws IOException {
        if (out == null)
            throw new NullPointerException();

        // -1 indicates unknown and >= 0 are for known sizes.
        if (expectedUncompressedSize < -1)
            throw new IllegalArgumentException(
                    "Invalid expected input size (less than -1)");

        this.useEndMarker = useEndMarker;
        this.expectedUncompressedSize = expectedUncompressedSize;

        this.arrayCache = arrayCache;

        this.out = out;
        rc = new RangeEncoderToStream(out);

        int dictSize = options.getDictSize();
        lzma = LZMAEncoder.getInstance(rc,
                options.getLc(), options.getLp(), options.getPb(),
                options.getMode(),
                dictSize, 0, options.getNiceLen(),
                options.getMatchFinder(), options.getDepthLimit(),
                arrayCache);

        lz = lzma.getLZEncoder();

        byte[] presetDict = options.getPresetDict();
        if (presetDict != null && presetDict.length > 0) {
            if (useHeader)
                throw new UnsupportedOptionsException(
                        "Preset dictionary cannot be used in .lzma files "
                        + "(try a raw LZMA stream instead)");

            lz.setPresetDict(dictSize, presetDict);
        }

        props = (options.getPb() * 5 + options.getLp()) * 9 + options.getLc();

        if (useHeader) {
            // Props byte stores lc, lp, and pb.
            out.write(props);

            // Dictionary size is stored as a 32-bit unsigned little endian
            // integer.
            for (int i = 0; i < 4; ++i) {
                out.write(dictSize & 0xFF);
                dictSize >>>= 8;
            }

            // Uncompressed size is stored as a 64-bit unsigned little endian
            // integer. The max value (-1 in two's complement) indicates
            // unknown size.
            for (int i = 0; i < 8; ++i)
                out.write((int)(expectedUncompressedSize >>> (8 * i)) & 0xFF);
        }
    }

    /**
     * Creates a new compressor for the legacy .lzma file format.
     * <p>
     * If the uncompressed size of the input data is known, it will be stored
     * in the .lzma header and no end of stream marker will be used. Otherwise
     * the header will indicate unknown uncompressed size and the end of stream
     * marker will be used.
     * <p>
     * Note that a preset dictionary cannot be used in .lzma files but
     * it can be used for raw LZMA streams.
     *
     * @param       out         output stream to which the compressed data
     *                          will be written
     *
     * @param       options     LZMA compression options; the same class
     *                          is used here as is for LZMA2
     *
     * @param       inputSize   uncompressed size of the data to be compressed;
     *                          use <code>-1</code> when unknown
     *
     * @throws      IOException may be thrown from <code>out</code>
     */
    public LZMAOutputStream(OutputStream out, LZMA2Options options,
                            long inputSize)
            throws IOException {
        this(out, options, inputSize, ArrayCache.getDefaultCache());
    }

    /**
     * Creates a new compressor for the legacy .lzma file format.
     * <p>
     * This is identical to
     * <code>LZMAOutputStream(OutputStream, LZMA2Options, long)</code>
     * except that this also takes the <code>arrayCache</code> argument.
     *
     * @param       out         output stream to which the compressed data
     *                          will be written
     *
     * @param       options     LZMA compression options; the same class
     *                          is used here as is for LZMA2
     *
     * @param       inputSize   uncompressed size of the data to be compressed;
     *                          use <code>-1</code> when unknown
     *
     * @param       arrayCache  cache to be used for allocating large arrays
     *
     * @throws      IOException may be thrown from <code>out</code>
     *
     * @since 1.7
     */
    public LZMAOutputStream(OutputStream out, LZMA2Options options,
                            long inputSize, ArrayCache arrayCache)
            throws IOException {
        this(out, options, true, inputSize == -1, inputSize, arrayCache);
    }

    /**
     * Creates a new compressor for raw LZMA (also known as LZMA1) stream.
     * <p>
     * Raw LZMA streams can be encoded with or without end of stream marker.
     * When decompressing the stream, one must know if the end marker was used
     * and tell it to the decompressor. If the end marker wasn't used, the
     * decompressor will also need to know the uncompressed size.
     *
     * @param       out         output stream to which the compressed data
     *                          will be written
     *
     * @param       options     LZMA compression options; the same class
     *                          is used here as is for LZMA2
     *
     * @param       useEndMarker
     *                          if end of stream marker should be written
     *
     * @throws      IOException may be thrown from <code>out</code>
     */
    public LZMAOutputStream(OutputStream out, LZMA2Options options,
                            boolean useEndMarker) throws IOException {
        this(out, options, useEndMarker, ArrayCache.getDefaultCache());
    }

    /**
     * Creates a new compressor for raw LZMA (also known as LZMA1) stream.
     * <p>
     * This is identical to
     * <code>LZMAOutputStream(OutputStream, LZMA2Options, boolean)</code>
     * except that this also takes the <code>arrayCache</code> argument.
     *
     * @param       out         output stream to which the compressed data
     *                          will be written
     *
     * @param       options     LZMA compression options; the same class
     *                          is used here as is for LZMA2
     *
     * @param       useEndMarker
     *                          if end of stream marker should be written
     *
     * @param       arrayCache  cache to be used for allocating large arrays
     *
     * @throws      IOException may be thrown from <code>out</code>
     *
     * @since 1.7
     */
    public LZMAOutputStream(OutputStream out, LZMA2Options options,
                            boolean useEndMarker, ArrayCache arrayCache)
            throws IOException {
        this(out, options, false, useEndMarker, -1, arrayCache);
    }

    /**
     * Returns the LZMA lc/lp/pb properties encoded into a single byte.
     * This might be useful when handling file formats other than .lzma
     * that use the same encoding for the LZMA properties as .lzma does.
     */
    public int getProps() {
        return props;
    }

    /**
     * Gets the amount of uncompressed data written to the stream.
     * This is useful when creating raw LZMA streams without
     * the end of stream marker.
     */
    public long getUncompressedSize() {
        return currentUncompressedSize;
    }

    public void write(int b) throws IOException {
        tempBuf[0] = (byte)b;
        write(tempBuf, 0, 1);
    }

    public void write(byte[] buf, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len < 0 || off + len > buf.length)
            throw new IndexOutOfBoundsException();

        if (exception != null)
            throw exception;

        if (finished)
            throw new XZIOException("Stream finished or closed");

        if (expectedUncompressedSize != -1
                && expectedUncompressedSize - currentUncompressedSize < len)
            throw new XZIOException("Expected uncompressed input size ("
                    + expectedUncompressedSize + " bytes) was exceeded");

        currentUncompressedSize += len;

        try {
            while (len > 0) {
                int used = lz.fillWindow(buf, off, len);
                off += used;
                len -= used;
                lzma.encodeForLZMA1();
            }
        } catch (IOException e) {
            exception = e;
            throw e;
        }
    }

    /**
     * Flushing isn't supported and will throw XZIOException.
     */
    public void flush() throws IOException {
        throw new XZIOException("LZMAOutputStream does not support flushing");
    }

    /**
     * Finishes the stream without closing the underlying OutputStream.
     */
    public void finish() throws IOException {
        if (!finished) {
            if (exception != null)
                throw exception;

            try {
                if (expectedUncompressedSize != -1
                        && expectedUncompressedSize != currentUncompressedSize)
                    throw new XZIOException("Expected uncompressed size ("
                            + expectedUncompressedSize + ") doesn't equal "
                            + "the number of bytes written to the stream ("
                            + currentUncompressedSize + ")");

                lz.setFinishing();
                lzma.encodeForLZMA1();

                if (useEndMarker)
                    lzma.encodeLZMA1EndMarker();

                rc.finish();
            } catch (IOException e) {
                exception = e;
                throw e;
            }

            finished = true;

            lzma.putArraysToCache(arrayCache);
            lzma = null;
            lz = null;
        }
    }

    /**
     * Finishes the stream and closes the underlying OutputStream.
     */
    public void close() throws IOException {
        if (out != null) {
            try {
                finish();
            } catch (IOException e) {}

            try {
                out.close();
            } catch (IOException e) {
                if (exception == null)
                    exception = e;
            }

            out = null;
        }

        if (exception != null)
            throw exception;
    }
}
