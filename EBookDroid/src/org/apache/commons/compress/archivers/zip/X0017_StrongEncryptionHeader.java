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

/**
 * Strong Encryption Header (0x0017).
 *
 * <p>Certificate-based encryption:</p>
 *
 * <pre>
 * Value     Size     Description
 * -----     ----     -----------
 * 0x0017    2 bytes  Tag for this "extra" block type
 * TSize     2 bytes  Size of data that follows
 * Format    2 bytes  Format definition for this record
 * AlgID     2 bytes  Encryption algorithm identifier
 * Bitlen    2 bytes  Bit length of encryption key (32-448 bits)
 * Flags     2 bytes  Processing flags
 * RCount    4 bytes  Number of recipients.
 * HashAlg   2 bytes  Hash algorithm identifier
 * HSize     2 bytes  Hash size
 * SRList    (var)    Simple list of recipients hashed public keys
 *
 * Flags -   This defines the processing flags.
 * </pre>
 *
 *           <ul>
 *           <li>0x0007 - reserved for future use
 *           <li>0x000F - reserved for future use
 *           <li>0x0100 - Indicates non-OAEP key wrapping was used.  If this
 *                        this field is set, the version needed to extract must
 *                        be at least 61.  This means OAEP key wrapping is not
 *                        used when generating a Master Session Key using
 *                        ErdData.
 *           <li>0x4000 - ErdData must be decrypted using 3DES-168, otherwise use the
 *                        same algorithm used for encrypting the file contents.
 *           <li>0x8000 - reserved for future use
 *           </ul>
 *
 * <pre>
 * RCount - This defines the number intended recipients whose
 *          public keys were used for encryption.  This identifies
 *          the number of elements in the SRList.
 *
 *          see also: reserved1
 *
 * HashAlg - This defines the hash algorithm used to calculate
 *           the public key hash of each public key used
 *           for encryption. This field currently supports
 *           only the following value for SHA-1
 *
 *           0x8004 - SHA1
 *
 * HSize -   This defines the size of a hashed public key.
 *
 * SRList -  This is a variable length list of the hashed
 *           public keys for each intended recipient.  Each
 *           element in this list is HSize.  The total size of
 *           SRList is determined using RCount * HSize.
 * </pre>
 *
 * <p>Password-based Extra Field 0x0017 in central header only.</p>
 *
 * <pre>
 * Value     Size     Description
 * -----     ----     -----------
 * 0x0017    2 bytes  Tag for this "extra" block type
 * TSize     2 bytes  Size of data that follows
 * Format    2 bytes  Format definition for this record
 * AlgID     2 bytes  Encryption algorithm identifier
 * Bitlen    2 bytes  Bit length of encryption key (32-448 bits)
 * Flags     2 bytes  Processing flags
 * (more?)
 * </pre>
 *
 * <p><b>Format</b> - the data format identifier for this record. The only value
 * allowed at this time is the integer value 2.</p>
 *
 * <p>Password-based Extra Field 0x0017 preceding compressed file data.</p>
 *
 * <pre>
 * Value     Size     Description
 * -----     ----     -----------
 * 0x0017    2 bytes  Tag for this "extra" block type
 * IVSize    2 bytes  Size of initialization vector (IV)
 * IVData    IVSize   Initialization vector for this file
 * Size      4 bytes  Size of remaining decryption header data
 * Format    2 bytes  Format definition for this record
 * AlgID     2 bytes  Encryption algorithm identifier
 * Bitlen    2 bytes  Bit length of encryption key (32-448 bits)
 * Flags     2 bytes  Processing flags
 * ErdSize   2 bytes  Size of Encrypted Random Data
 * ErdData   ErdSize  Encrypted Random Data
 * Reserved1 4 bytes  Reserved certificate processing data
 * Reserved2 (var)    Reserved for certificate processing data
 * VSize     2 bytes  Size of password validation data
 * VData     VSize-4  Password validation data
 * VCRC32    4 bytes  Standard ZIP CRC32 of password validation data
 *
 * IVData - The size of the IV should match the algorithm block size.
 *          The IVData can be completely random data.  If the size of
 *          the randomly generated data does not match the block size
 *          it should be complemented with zero's or truncated as
 *          necessary.  If IVSize is 0,then IV = CRC32 + Uncompressed
 *          File Size (as a 64 bit little-endian, unsigned integer value).
 *
 * Format -  the data format identifier for this record.  The only
 *           value allowed at this time is the integer value 2.
 *
 * ErdData - Encrypted random data is used to store random data that
 *           is used to generate a file session key for encrypting
 *           each file.  SHA1 is used to calculate hash data used to
 *           derive keys.  File session keys are derived from a master
 *           session key generated from the user-supplied password.
 *           If the Flags field in the decryption header contains
 *           the value 0x4000, then the ErdData field must be
 *           decrypted using 3DES. If the value 0x4000 is not set,
 *           then the ErdData field must be decrypted using AlgId.
 *
 * Reserved1 - Reserved for certificate processing, if value is
 *           zero, then Reserved2 data is absent.  See the explanation
 *           under the Certificate Processing Method for details on
 *           this data structure.
 *
 * Reserved2 - If present, the size of the Reserved2 data structure
 *           is located by skipping the first 4 bytes of this field
 *           and using the next 2 bytes as the remaining size.  See
 *           the explanation under the Certificate Processing Method
 *           for details on this data structure.
 *
 * VSize - This size value will always include the 4 bytes of the
 *         VCRC32 data and will be greater than 4 bytes.
 *
 * VData - Random data for password validation.  This data is VSize
 *         in length and VSize must be a multiple of the encryption
 *         block size.  VCRC32 is a checksum value of VData.
 *         VData and VCRC32 are stored encrypted and start the
 *         stream of encrypted data for a file.
 * </pre>
 *
 * <p>Reserved1 - Certificate Decryption Header Reserved1 Data:</p>
 *
 * <pre>
 * Value     Size     Description
 * -----     ----     -----------
 * RCount    4 bytes  Number of recipients.
 * </pre>
 *
 * <p>RCount - This defines the number intended recipients whose public keys were
 * used for encryption. This defines the number of elements in the REList field
 * defined below.</p>
 *
 * <p>Reserved2 - Certificate Decryption Header Reserved2 Data Structures:</p>
 *
 * <pre>
 * Value     Size     Description
 * -----     ----     -----------
 * HashAlg   2 bytes  Hash algorithm identifier
 * HSize     2 bytes  Hash size
 * REList    (var)    List of recipient data elements
 *
 * HashAlg - This defines the hash algorithm used to calculate
 *           the public key hash of each public key used
 *           for encryption. This field currently supports
 *           only the following value for SHA-1
 *
 *               0x8004 - SHA1
 *
 * HSize -   This defines the size of a hashed public key
 *           defined in REHData.
 *
 * REList -  This is a variable length of list of recipient data.
 *           Each element in this list consists of a Recipient
 *           Element data structure as follows:
 * </pre>
 *
 * <p>Recipient Element (REList) Data Structure:</p>
 *
 * <pre>
 * Value     Size     Description
 * -----     ----     -----------
 * RESize    2 bytes  Size of REHData + REKData
 * REHData   HSize    Hash of recipients public key
 * REKData   (var)    Simple key blob
 *
 *
 * RESize -  This defines the size of an individual REList
 *           element.  This value is the combined size of the
 *           REHData field + REKData field.  REHData is defined by
 *           HSize.  REKData is variable and can be calculated
 *           for each REList element using RESize and HSize.
 *
 * REHData - Hashed public key for this recipient.
 *
 * REKData - Simple Key Blob.  The format of this data structure
 *           is identical to that defined in the Microsoft
 *           CryptoAPI and generated using the CryptExportKey()
 *           function.  The version of the Simple Key Blob
 *           supported at this time is 0x02 as defined by
 *           Microsoft.
 *
 *           For more details see https://msdn.microsoft.com/en-us/library/aa920051.aspx
 * </pre>
 *
 * <p><b>Flags</b> - Processing flags needed for decryption</p>
 *
 * <ul>
 * <li>0x0001 - Password is required to decrypt</li>
 * <li>0x0002 - Certificates only</li>
 * <li>0x0003 - Password or certificate required to decrypt</li>
 * <li>0x0007 - reserved for future use
 * <li>0x000F - reserved for future use
 * <li>0x0100 - indicates non-OAEP key wrapping was used. If this field is set
 * the version needed to extract must be at least 61. This means OAEP key
 * wrapping is not used when generating a Master Session Key using ErdData.
 * <li>0x4000 - ErdData must be decrypted using 3DES-168, otherwise use the same
 * algorithm used for encrypting the file contents.
 * <li>0x8000 - reserved for future use.
 * </ul>
 *
 * <p><b>See the section describing the Strong Encryption Specification for
 * details. Refer to the section in this document entitled
 * "Incorporating PKWARE Proprietary Technology into Your Product" for more
 * information.</b></p>
 *
 * @NotThreadSafe
 * @since 1.11
 */
public class X0017_StrongEncryptionHeader extends PKWareExtraHeader {

    public X0017_StrongEncryptionHeader() {
        super(new ZipShort(0x0017));
    }

    private int format; // TODO written but not read
    private EncryptionAlgorithm algId;
    private int bitlen; // TODO written but not read
    private int flags; // TODO written but not read
    private long rcount;
    private HashAlgorithm hashAlg;
    private int hashSize;

    // encryption data
    private byte ivData[];
    private byte erdData[];

    // encryption key
    private byte recipientKeyHash[];
    private byte keyBlob[];

    // password verification data
    private byte vData[];
    private byte vCRC32[];

    /**
     * Get record count.
     * @return the record count
     */
    public long getRecordCount() {
        return rcount;
    }

    /**
     * Get hash algorithm.
     * @return the hash algorithm
     */
    public HashAlgorithm getHashAlgorithm() {
        return hashAlg;
    }

    /**
     * Get encryption algorithm.
     * @return the encryption algorithm
     */
    public EncryptionAlgorithm getEncryptionAlgorithm() {
        return algId;
    }

    /**
     * Parse central directory format.
     *
     * @param data the buffer to read data from
     * @param offset offset into buffer to read data
     * @param length the length of data
     */
    public void parseCentralDirectoryFormat(final byte[] data, final int offset, final int length) {
        this.format = ZipShort.getValue(data, offset);
        this.algId = EncryptionAlgorithm.getAlgorithmByCode(ZipShort.getValue(data, offset + 2));
        this.bitlen = ZipShort.getValue(data, offset + 4);
        this.flags = ZipShort.getValue(data, offset + 6);
        this.rcount = ZipLong.getValue(data, offset + 8);

        if (rcount > 0) {
            this.hashAlg = HashAlgorithm.getAlgorithmByCode(ZipShort.getValue(data, offset + 12));
            this.hashSize = ZipShort.getValue(data, offset + 14);
            // srlist... hashed public keys
            for (long i = 0; i < this.rcount; i++) {
                for (int j = 0; j < this.hashSize; j++) {
                    //  ZipUtil.signedByteToUnsignedInt(data[offset + 16 + (i * this.hashSize) + j]));
                }
            }
        }
    }

    /**
     * Parse file header format.
     *
     * <p>(Password only?)</p>
     *
     * @param data the buffer to read data from
     * @param offset offset into buffer to read data
     * @param length the length of data
     */
    public void parseFileFormat(final byte[] data, final int offset, final int length) {
        final int ivSize = ZipShort.getValue(data, offset);
        this.ivData = new byte[ivSize];
        System.arraycopy(data, offset + 4, this.ivData, 0, ivSize);

        this.format = ZipShort.getValue(data, offset + ivSize + 6);
        this.algId = EncryptionAlgorithm.getAlgorithmByCode(ZipShort.getValue(data, offset + ivSize + 8));
        this.bitlen = ZipShort.getValue(data, offset + ivSize + 10);
        this.flags = ZipShort.getValue(data, offset + ivSize + 12);

        final int erdSize = ZipShort.getValue(data, offset + ivSize + 14);
        this.erdData = new byte[erdSize];
        System.arraycopy(data, offset + ivSize + 16, this.erdData, 0, erdSize);

        this.rcount = ZipLong.getValue(data, offset + ivSize + 16 + erdSize);
        System.out.println("rcount: " + rcount);
        if (rcount == 0) {
            final int vSize = ZipShort.getValue(data, offset + ivSize + 20 + erdSize);
            this.vData = new byte[vSize - 4];
            this.vCRC32 = new byte[4];
            System.arraycopy(data, offset + ivSize + 22 + erdSize , this.vData, 0, vSize - 4);
            System.arraycopy(data, offset + ivSize + 22 + erdSize + vSize - 4, vCRC32, 0, 4);
        } else {
            this.hashAlg = HashAlgorithm.getAlgorithmByCode(ZipShort.getValue(data, offset + ivSize + 20 + erdSize));
            this.hashSize = ZipShort.getValue(data, offset + ivSize + 22 + erdSize);
            final int resize = ZipShort.getValue(data, offset + ivSize + 24 + erdSize);
            this.recipientKeyHash = new byte[this.hashSize];
            this.keyBlob = new byte[resize - this.hashSize];
            System.arraycopy(data, offset + ivSize + 24 + erdSize, this.recipientKeyHash, 0, this.hashSize);
            System.arraycopy(data, offset + ivSize + 24 + erdSize + this.hashSize, this.keyBlob, 0, resize - this.hashSize);

            final int vSize = ZipShort.getValue(data, offset + ivSize + 26 + erdSize + resize);
            this.vData = new byte[vSize - 4];
            this.vCRC32 = new byte[4];
            System.arraycopy(data, offset + ivSize + 22 + erdSize + resize, this.vData, 0, vSize - 4);
            System.arraycopy(data, offset + ivSize + 22 + erdSize + resize + vSize - 4, vCRC32, 0, 4);
        }

        // validate values?
    }

    @Override
    public void parseFromLocalFileData(final byte[] data, final int offset, final int length) {
        super.parseFromLocalFileData(data, offset, length);
        parseFileFormat(data, offset, length);
    }

    @Override
    public void parseFromCentralDirectoryData(final byte[] data, final int offset, final int length) {
        super.parseFromCentralDirectoryData(data, offset, length);
        parseCentralDirectoryFormat(data, offset, length);
    }
}
