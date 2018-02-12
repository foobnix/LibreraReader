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
package org.apache.commons.compress.archivers.zip;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all PKWare strong crypto extra headers.
 *
 * <p>This base class acts as a marker so you know you can ignore all
 * extra fields that extend this class if you are not interested in
 * the meta data of PKWare strong encryption.</p>
 *
 * <b>Algorithm IDs</b> - integer identifier of the encryption algorithm from
 * the following range
 *
 * <ul>
 * <li>0x6601 - DES</li>
 * <li>0x6602 - RC2 (version needed to extract &lt; 5.2)</li>
 * <li>0x6603 - 3DES 168</li>
 * <li>0x6609 - 3DES 112</li>
 * <li>0x660E - AES 128</li>
 * <li>0x660F - AES 192</li>
 * <li>0x6610 - AES 256</li>
 * <li>0x6702 - RC2 (version needed to extract &gt;= 5.2)</li>
 * <li>0x6720 - Blowfish</li>
 * <li>0x6721 - Twofish</li>
 * <li>0x6801 - RC4</li>
 * <li>0xFFFF - Unknown algorithm</li>
 * </ul>
 *
 * <b>Hash Algorithms</b> - integer identifier of the hash algorithm from the
 * following range
 *
 * <ul>
 * <li>0x0000 - none</li>
 * <li>0x0001 - CRC32</li>
 * <li>0x8003 - MD5</li>
 * <li>0x8004 - SHA1</li>
 * <li>0x8007 - RIPEMD160</li>
 * <li>0x800C - SHA256</li>
 * <li>0x800D - SHA384</li>
 * <li>0x800E - SHA512</li>
 * </ul>
 *
 * @since 1.11
 */
public abstract class PKWareExtraHeader implements ZipExtraField {

    private final ZipShort headerId;
    /**
     * Extra field data in local file data - without Header-ID or length
     * specifier.
     */
    private byte[] localData;
    /**
     * Extra field data in central directory - without Header-ID or length
     * specifier.
     */
    private byte[] centralData;

    protected PKWareExtraHeader(final ZipShort headerId) {
        this.headerId = headerId;
    }

    /**
     * Get the header id.
     *
     * @return the header id
     */
    @Override
    public ZipShort getHeaderId() {
        return headerId;
    }

    /**
     * Set the extra field data in the local file data - without Header-ID or
     * length specifier.
     *
     * @param data
     *            the field data to use
     */
    public void setLocalFileDataData(final byte[] data) {
        localData = ZipUtil.copy(data);
    }

    /**
     * Get the length of the local data.
     *
     * @return the length of the local data
     */
    @Override
    public ZipShort getLocalFileDataLength() {
        return new ZipShort(localData != null ? localData.length : 0);
    }

    /**
     * Get the local data.
     *
     * @return the local data
     */
    @Override
    public byte[] getLocalFileDataData() {
        return ZipUtil.copy(localData);
    }

    /**
     * Set the extra field data in central directory.
     *
     * @param data
     *            the data to use
     */
    public void setCentralDirectoryData(final byte[] data) {
        centralData = ZipUtil.copy(data);
    }

    /**
     * Get the central data length. If there is no central data, get the local
     * file data length.
     *
     * @return the central data length
     */
    @Override
    public ZipShort getCentralDirectoryLength() {
        if (centralData != null) {
            return new ZipShort(centralData.length);
        }
        return getLocalFileDataLength();
    }

    /**
     * Get the central data.
     *
     * @return the central data if present, else return the local file data
     */
    @Override
    public byte[] getCentralDirectoryData() {
        if (centralData != null) {
            return ZipUtil.copy(centralData);
        }
        return getLocalFileDataData();
    }

    /**
     * @param data
     *            the array of bytes.
     * @param offset
     *            the source location in the data array.
     * @param length
     *            the number of bytes to use in the data array.
     * @see ZipExtraField#parseFromLocalFileData(byte[], int, int)
     */
    @Override
    public void parseFromLocalFileData(final byte[] data, final int offset, final int length) {
        final byte[] tmp = new byte[length];
        System.arraycopy(data, offset, tmp, 0, length);
        setLocalFileDataData(tmp);
    }

    /**
     * @param data
     *            the array of bytes.
     * @param offset
     *            the source location in the data array.
     * @param length
     *            the number of bytes to use in the data array.
     * @see ZipExtraField#parseFromCentralDirectoryData(byte[], int, int)
     */
    @Override
    public void parseFromCentralDirectoryData(final byte[] data, final int offset, final int length) {
        final byte[] tmp = new byte[length];
        System.arraycopy(data, offset, tmp, 0, length);
        setCentralDirectoryData(tmp);
        if (localData == null) {
            setLocalFileDataData(tmp);
        }
    }

    /**
     * Encryption algorithm.
     *
     * @since 1.11
     */
    public enum EncryptionAlgorithm {
        DES(0x6601),
        RC2pre52(0x6602),
        TripleDES168(0x6603),
        TripleDES192(0x6609),
        AES128(0x660E),
        AES192(0x660F),
        AES256(0x6610),
        RC2(0x6702),
        RC4(0x6801),
        UNKNOWN(0xFFFF);

        private final int code;

        private static final Map<Integer, EncryptionAlgorithm> codeToEnum;

        static {
            final Map<Integer, EncryptionAlgorithm> cte = new HashMap<>();
            for (final EncryptionAlgorithm method : values()) {
                cte.put(method.getCode(), method);
            }
            codeToEnum = Collections.unmodifiableMap(cte);
        }

        /**
         * private constructor for enum style class.
         */
        EncryptionAlgorithm(final int code) {
            this.code = code;
        }

        /**
         * the algorithm id.
         *
         * @return the PKWare AlgorithmId
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the EncryptionAlgorithm for the given code or null if the
         * method is not known.
         * @param code the code of the algorithm
         * @return the EncryptionAlgorithm for the given code or null
         * if the method is not known
         */
        public static EncryptionAlgorithm getAlgorithmByCode(final int code) {
            return codeToEnum.get(code);
        }
    }

    /**
     * Hash Algorithm
     *
     * @since 1.11
     */
    public enum HashAlgorithm {
        NONE(0),
        CRC32(1),
        MD5(0x8003),
        SHA1(0x8004),
        RIPEND160(0x8007),
        SHA256(0x800C),
        SHA384(0x800D),
        SHA512(0x800E);

        private final int code;

        private static final Map<Integer, HashAlgorithm> codeToEnum;

        static {
            final Map<Integer, HashAlgorithm> cte = new HashMap<>();
            for (final HashAlgorithm method : values()) {
                cte.put(method.getCode(), method);
            }
            codeToEnum = Collections.unmodifiableMap(cte);
        }

        /**
         * private constructor for enum style class.
         */
        HashAlgorithm(final int code) {
            this.code = code;
        }

        /**
         * the hash algorithm ID.
         *
         * @return the PKWare hashAlg
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the HashAlgorithm for the given code or null if the method is
         * not known.
         * @param code the code of the algorithm
         * @return the HashAlgorithm for the given code or null
         * if the method is not known
         */
        public static HashAlgorithm getAlgorithmByCode(final int code) {
            return codeToEnum.get(code);
        }
    }
}
