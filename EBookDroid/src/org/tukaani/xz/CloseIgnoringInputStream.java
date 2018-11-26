/*
 * CloseIgnoringInputStream
 *
 * Author: Lasse Collin <lasse.collin@tukaani.org>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */

package org.tukaani.xz;

import java.io.InputStream;
import java.io.FilterInputStream;

/**
 * An {@code InputStream} wrapper whose {@code close()} does nothing.
 * This is useful with raw decompressors if you want to call
 * {@code close()} to release memory allocated from an {@link ArrayCache}
 * but don't want to close the underlying {@code InputStream}.
 * For example:
 * <p><blockquote><pre>
 * InputStream rawdec = new LZMA2InputStream(
 *         new CloseIgnoringInputStream(myInputStream),
 *         myDictSize, null, myArrayCache);
 * doSomething(rawdec);
 * rawdec.close(); // This doesn't close myInputStream.
 * </pre></blockquote>
 * <p>
 * With {@link XZInputStream}, {@link SingleXZInputStream}, and
 * {@link SeekableXZInputStream} you can use their {@code close(boolean)}
 * method to avoid closing the underlying {@code InputStream}; with
 * those classes {@code CloseIgnoringInputStream} isn't needed.
 *
 * @since 1.7
 */
public class CloseIgnoringInputStream extends FilterInputStream {
    /**
     * Creates a new {@code CloseIgnoringInputStream}.
     */
    public CloseIgnoringInputStream(InputStream in) {
        super(in);
    }

    /**
     * This does nothing (doesn't call {@code in.close()}).
     */
    public void close() {}
}
