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

/**
 * Parser/encoder for the "general purpose bit" field in ZIP's local
 * file and central directory headers.
 *
 * @since 1.1
 * @NotThreadSafe
 */
public final class GeneralPurposeBit implements Cloneable {

    /**
     * Indicates that the file is encrypted.
     */
    private static final int ENCRYPTION_FLAG = 1 << 0;

    /**
     * Indicates the size of the sliding dictionary used by the compression method 6 (imploding).
     * <ul>
     *   <li>0: 4096 bytes</li>
     *   <li>1: 8192 bytes</li>
     * </ul>
     */
    private static final int SLIDING_DICTIONARY_SIZE_FLAG = 1 << 1;

    /**
     * Indicates the number of Shannon-Fano trees used by the compression method 6 (imploding).
     * <ul>
     *   <li>0: 2 trees (lengths, distances)</li>
     *   <li>1: 3 trees (literals, lengths, distances)</li>
     * </ul>
     */
    private static final int NUMBER_OF_SHANNON_FANO_TREES_FLAG = 1 << 2;

    /**
     * Indicates that a data descriptor stored after the file contents
     * will hold CRC and size information.
     */
    private static final int DATA_DESCRIPTOR_FLAG = 1 << 3;

    /**
     * Indicates strong encryption.
     */
    private static final int STRONG_ENCRYPTION_FLAG = 1 << 6;

    /**
     * Indicates that filenames are written in UTF-8.
     *
     * <p>The only reason this is public is that {@link
     * ZipArchiveOutputStream#EFS_FLAG} was public in Apache Commons
     * Compress 1.0 and we needed a substitute for it.</p>
     */
    public static final int UFT8_NAMES_FLAG = 1 << 11;

    private boolean languageEncodingFlag = false;
    private boolean dataDescriptorFlag = false;
    private boolean encryptionFlag = false;
    private boolean strongEncryptionFlag = false;
    private int slidingDictionarySize;
    private int numberOfShannonFanoTrees;

    public GeneralPurposeBit() {
    }

    /**
     * whether the current entry uses UTF8 for file name and comment.
     * @return whether the current entry uses UTF8 for file name and comment.
     */
    public boolean usesUTF8ForNames() {
        return languageEncodingFlag;
    }

    /**
     * whether the current entry will use UTF8 for file name and comment.
     * @param b whether the current entry will use UTF8 for file name and comment.
     */
    public void useUTF8ForNames(final boolean b) {
        languageEncodingFlag = b;
    }

    /**
     * whether the current entry uses the data descriptor to store CRC
     * and size information.
     * @return whether the current entry uses the data descriptor to store CRC
     * and size information
     */
    public boolean usesDataDescriptor() {
        return dataDescriptorFlag;
    }

    /**
     * whether the current entry will use the data descriptor to store
     * CRC and size information.
     * @param b whether the current entry will use the data descriptor to store
     * CRC and size information
     */
    public void useDataDescriptor(final boolean b) {
        dataDescriptorFlag = b;
    }

    /**
     * whether the current entry is encrypted.
     * @return whether the current entry is encrypted
     */
    public boolean usesEncryption() {
        return encryptionFlag;
    }

    /**
     * whether the current entry will be encrypted.
     * @param b whether the current entry will be encrypted
     */
    public void useEncryption(final boolean b) {
        encryptionFlag = b;
    }

    /**
     * whether the current entry is encrypted using strong encryption.
     * @return whether the current entry is encrypted using strong encryption
     */
    public boolean usesStrongEncryption() {
        return encryptionFlag && strongEncryptionFlag;
    }

    /**
     * whether the current entry will be encrypted  using strong encryption.
     * @param b whether the current entry will be encrypted  using strong encryption
     */
    public void useStrongEncryption(final boolean b) {
        strongEncryptionFlag = b;
        if (b) {
            useEncryption(true);
        }
    }

    /**
     * Returns the sliding dictionary size used by the compression method 6 (imploding).
     */
    int getSlidingDictionarySize() {
        return slidingDictionarySize;
    }

    /**
     * Returns the number of trees used by the compression method 6 (imploding).
     */
    int getNumberOfShannonFanoTrees() {
        return numberOfShannonFanoTrees;
    }

    /**
     * Encodes the set bits in a form suitable for ZIP archives.
     * @return the encoded general purpose bits
     */
    public byte[] encode() {
        final byte[] result = new byte[2];
        encode(result, 0);
        return result;
    }


    /**
     * Encodes the set bits in a form suitable for ZIP archives.
     *
     * @param buf the output buffer
     * @param  offset
     *         The offset within the output buffer of the first byte to be written.
     *         must be non-negative and no larger than <tt>buf.length-2</tt>
     */
    public void encode(final byte[] buf, final int offset) {
                ZipShort.putShort((dataDescriptorFlag ? DATA_DESCRIPTOR_FLAG : 0)
                        |
                        (languageEncodingFlag ? UFT8_NAMES_FLAG : 0)
                        |
                        (encryptionFlag ? ENCRYPTION_FLAG : 0)
                        |
                        (strongEncryptionFlag ? STRONG_ENCRYPTION_FLAG : 0)
                        , buf, offset);
    }

    /**
     * Parses the supported flags from the given archive data.
     *
     * @param data local file header or a central directory entry.
     * @param offset offset at which the general purpose bit starts
     * @return parsed flags
     */
    public static GeneralPurposeBit parse(final byte[] data, final int offset) {
        final int generalPurposeFlag = ZipShort.getValue(data, offset);
        final GeneralPurposeBit b = new GeneralPurposeBit();
        b.useDataDescriptor((generalPurposeFlag & DATA_DESCRIPTOR_FLAG) != 0);
        b.useUTF8ForNames((generalPurposeFlag & UFT8_NAMES_FLAG) != 0);
        b.useStrongEncryption((generalPurposeFlag & STRONG_ENCRYPTION_FLAG) != 0);
        b.useEncryption((generalPurposeFlag & ENCRYPTION_FLAG) != 0);
        b.slidingDictionarySize = (generalPurposeFlag & SLIDING_DICTIONARY_SIZE_FLAG) != 0 ? 8192 : 4096;
        b.numberOfShannonFanoTrees = (generalPurposeFlag & NUMBER_OF_SHANNON_FANO_TREES_FLAG) != 0 ? 3 : 2;
        return b;
    }

    @Override
    public int hashCode() {
        return 3 * (7 * (13 * (17 * (encryptionFlag ? 1 : 0)
                               + (strongEncryptionFlag ? 1 : 0))
                         + (languageEncodingFlag ? 1 : 0))
                    + (dataDescriptorFlag ? 1 : 0));
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GeneralPurposeBit)) {
            return false;
        }
        final GeneralPurposeBit g = (GeneralPurposeBit) o;
        return g.encryptionFlag == encryptionFlag
            && g.strongEncryptionFlag == strongEncryptionFlag
            && g.languageEncodingFlag == languageEncodingFlag
            && g.dataDescriptorFlag == dataDescriptorFlag;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException ex) {
            // impossible
            throw new RuntimeException("GeneralPurposeBit is not Cloneable?", ex); //NOSONAR
        }
    }
}
