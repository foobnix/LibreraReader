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
package org.apache.commons.compress.archivers.zip;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;

/**
 * List of known compression methods
 *
 * Many of these methods are currently not supported by commons compress
 *
 * @since 1.5
 */
public enum ZipMethod {

    /**
     * Compression method 0 for uncompressed entries.
     *
     * @see ZipEntry#STORED
     */
    STORED(ZipEntry.STORED),

    /**
     * UnShrinking.
     * dynamic Lempel-Ziv-Welch-Algorithm
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression
     *      method: (2 bytes)</a>
     */
    UNSHRINKING(1),

    /**
     * Reduced with compression factor 1.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression
     *      method: (2 bytes)</a>
     */
    EXPANDING_LEVEL_1(2),

    /**
     * Reduced with compression factor 2.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression
     *      method: (2 bytes)</a>
     */
    EXPANDING_LEVEL_2(3),

    /**
     * Reduced with compression factor 3.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression
     *      method: (2 bytes)</a>
     */
    EXPANDING_LEVEL_3(4),

    /**
     * Reduced with compression factor 4.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression
     *      method: (2 bytes)</a>
     */
    EXPANDING_LEVEL_4(5),

    /**
     * Imploding.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression
     *      method: (2 bytes)</a>
     */
    IMPLODING(6),

    /**
     * Tokenization.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression
     *      method: (2 bytes)</a>
     */
    TOKENIZATION(7),

    /**
     * Compression method 8 for compressed (deflated) entries.
     *
     * @see ZipEntry#DEFLATED
     */
    DEFLATED(ZipEntry.DEFLATED),

    /**
     * Compression Method 9 for enhanced deflate.
     *
     * @see <a href="https://www.winzip.com/wz54.htm">https://www.winzip.com/wz54.htm</a>
     */
    ENHANCED_DEFLATED(9),

    /**
     * PKWARE Data Compression Library Imploding.
     *
     * @see <a href="https://www.winzip.com/wz54.htm">https://www.winzip.com/wz54.htm</a>
     */
    PKWARE_IMPLODING(10),

    /**
     * Compression Method 12 for bzip2.
     *
     * @see <a href="https://www.winzip.com/wz54.htm">https://www.winzip.com/wz54.htm</a>
     */
    BZIP2(12),

    /**
     * Compression Method 14 for LZMA.
     *
     * @see <a href="https://www.7-zip.org/sdk.html">https://www.7-zip.org/sdk.html</a>
     * @see <a href="https://www.winzip.com/wz54.htm">https://www.winzip.com/wz54.htm</a>
     */
    LZMA(14),


    /**
     * Compression Method 95 for XZ.
     *
     * @see <a href="https://www.winzip.com/wz54.htm">https://www.winzip.com/wz54.htm</a>
     */
    XZ(95),

    /**
     * Compression Method 96 for Jpeg compression.
     *
     * @see <a href="https://www.winzip.com/wz54.htm">https://www.winzip.com/wz54.htm</a>
     */
    JPEG(96),

    /**
     * Compression Method 97 for WavPack.
     *
     * @see <a href="https://www.winzip.com/wz54.htm">https://www.winzip.com/wz54.htm</a>
     */
    WAVPACK(97),

    /**
     * Compression Method 98 for PPMd.
     *
     * @see <a href="https://www.winzip.com/wz54.htm">https://www.winzip.com/wz54.htm</a>
     */
    PPMD(98),


    /**
     * Compression Method 99 for AES encryption.
     *
     * @see <a href="https://www.winzip.com/wz54.htm">https://www.winzip.com/wz54.htm</a>
     */
    AES_ENCRYPTED(99),

    /**
     * Unknown compression method.
     */
    UNKNOWN();

    static final int UNKNOWN_CODE = -1;

    private final int code;

    private static final Map<Integer, ZipMethod> codeToEnum;

    static {
        final Map<Integer, ZipMethod> cte = new HashMap<>();
        for (final ZipMethod method : values()) {
            cte.put(method.getCode(), method);
        }
        codeToEnum = Collections.unmodifiableMap(cte);
    }

    private ZipMethod() {
        this(UNKNOWN_CODE);
    }

    /**
     * private constructor for enum style class.
     */
    ZipMethod(final int code) {
        this.code = code;
    }

    /**
     * the code of the compression method.
     *
     * @see ZipArchiveEntry#getMethod()
     *
     * @return an integer code for the method
     */
    public int getCode() {
        return code;
    }


    /**
     * returns the {@link ZipMethod} for the given code or null if the
     * method is not known.
     * @param code the code
     * @return the {@link ZipMethod} for the given code or null if the
     * method is not known.
     */
    public static ZipMethod getMethodByCode(final int code) {
        return codeToEnum.get(code);
    }
}
