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
 * X.509 Certificate ID and Signature for central directory (0x0016).
 *
 * <p>This field contains the information about which certificate in the PKCS#7
 * store was used to sign the central directory structure. When the Central
 * Directory Encryption feature is enabled for a ZIP file, this record will
 * appear in the Archive Extra Data Record, otherwise it will appear in the
 * first central directory record.</p>
 *
 * <p>Note: all fields stored in Intel low-byte/high-byte order.</p>
 *
 * <pre>
 *         Value     Size     Description
 *         -----     ----     -----------
 * (CDID)  0x0016    2 bytes  Tag for this "extra" block type
 *         TSize     2 bytes  Size of data that follows
 *         RCount    4 bytes  Number of recipients. (inferred)
 *         HashAlg   2 bytes  Hash algorithm identifier. (inferred)
 *         TData     TSize    Data
 * </pre>
 *
 * @NotThreadSafe
 * @since 1.11
 */
public class X0016_CertificateIdForCentralDirectory extends PKWareExtraHeader {

    public X0016_CertificateIdForCentralDirectory() {
        super(new ZipShort(0x0016));
    }

    private int rcount;
    private HashAlgorithm hashAlg;

    /**
     * Get record count.
     * @return the record count
     */
    public int getRecordCount() {
        return rcount;
    }

    /**
     * Get hash algorithm.
     * @return the hash algorithm
     */
    public HashAlgorithm getHashAlgorithm() {
        return hashAlg;
    }

    @Override
    public void parseFromCentralDirectoryData(final byte[] data, final int offset, final int length) {
        this.rcount = ZipShort.getValue(data, offset);
        this.hashAlg = HashAlgorithm.getAlgorithmByCode(ZipShort.getValue(data, offset + 2));
    }
}
