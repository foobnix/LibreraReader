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
package org.apache.commons.compress.compressors.lzma;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.compress.compressors.FileNameUtil;

/**
 * Utility code for the lzma compression format.
 * @ThreadSafe
 * @since 1.10
 */
public class LZMAUtils {

    private static final FileNameUtil fileNameUtil;

    /**
     * LZMA Header Magic Bytes begin a LZMA file.
     */
    private static final byte[] HEADER_MAGIC = {
        (byte) 0x5D, 0, 0
    };

    enum CachedAvailability {
        DONT_CACHE, CACHED_AVAILABLE, CACHED_UNAVAILABLE
    }

    private static volatile CachedAvailability cachedLZMAAvailability;

    static {
        final Map<String, String> uncompressSuffix = new HashMap<>();
        uncompressSuffix.put(".lzma", "");
        uncompressSuffix.put("-lzma", "");
        fileNameUtil = new FileNameUtil(uncompressSuffix, ".lzma");
        cachedLZMAAvailability = CachedAvailability.DONT_CACHE;
        try {
            Class.forName("org.osgi.framework.BundleEvent");
        } catch (final Exception ex) {
            setCacheLZMAAvailablity(true);
        }
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private LZMAUtils() {
    }

    /**
     * Checks if the signature matches what is expected for a .lzma file.
     *
     * @param   signature     the bytes to check
     * @param   length        the number of bytes to check
     * @return  true if signature matches the .lzma magic bytes, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < HEADER_MAGIC.length) {
            return false;
        }

        for (int i = 0; i < HEADER_MAGIC.length; ++i) {
            if (signature[i] != HEADER_MAGIC[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Are the classes required to support LZMA compression available?
     * @return true if the classes required to support LZMA
     * compression are available
     */
    public static boolean isLZMACompressionAvailable() {
        final CachedAvailability cachedResult = cachedLZMAAvailability;
        if (cachedResult != CachedAvailability.DONT_CACHE) {
            return cachedResult == CachedAvailability.CACHED_AVAILABLE;
        }
        return internalIsLZMACompressionAvailable();
    }

    private static boolean internalIsLZMACompressionAvailable() {
        try {
            LZMACompressorInputStream.matches(null, 0);
            return true;
        } catch (final NoClassDefFoundError error) {
            return false;
        }
    }

    /**
     * Detects common lzma suffixes in the given filename.
     *
     * @param filename name of a file
     * @return {@code true} if the filename has a common lzma suffix,
     *         {@code false} otherwise
     */
    public static boolean isCompressedFilename(final String filename) {
        return fileNameUtil.isCompressedFilename(filename);
    }

    /**
     * Maps the given name of a lzma-compressed file to the name that
     * the file should have after uncompression.  Any filenames with
     * the generic ".lzma" suffix (or any other generic lzma suffix)
     * is mapped to a name without that suffix. If no lzma suffix is
     * detected, then the filename is returned unmapped.
     *
     * @param filename name of a file
     * @return name of the corresponding uncompressed file
     */
    public static String getUncompressedFilename(final String filename) {
        return fileNameUtil.getUncompressedFilename(filename);
    }

    /**
     * Maps the given filename to the name that the file should have after
     * compression with lzma.
     *
     * @param filename name of a file
     * @return name of the corresponding compressed file
     */
    public static String getCompressedFilename(final String filename) {
        return fileNameUtil.getCompressedFilename(filename);
    }

    /**
     * Whether to cache the result of the LZMA check.
     *
     * <p>This defaults to {@code false} in an OSGi environment and {@code true} otherwise.</p>
     * @param doCache whether to cache the result
     */
    public static void setCacheLZMAAvailablity(final boolean doCache) {
        if (!doCache) {
            cachedLZMAAvailability = CachedAvailability.DONT_CACHE;
        } else if (cachedLZMAAvailability == CachedAvailability.DONT_CACHE) {
            final boolean hasLzma = internalIsLZMACompressionAvailable();
            cachedLZMAAvailability = hasLzma ? CachedAvailability.CACHED_AVAILABLE // NOSONAR
                : CachedAvailability.CACHED_UNAVAILABLE;
        }
    }

    // only exists to support unit tests
    static CachedAvailability getCachedLZMAAvailability() {
        return cachedLZMAAvailability;
    }
}
