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
 * PKCS#7 Encryption Recipient Certificate List (0x0019).
 *
 * <p>This field MAY contain information about each of the certificates used in
 * encryption processing and it can be used to identify who is allowed to
 * decrypt encrypted files. This field should only appear in the archive extra
 * data record. This field is not required and serves only to aid archive
 * modifications by preserving public encryption key data. Individual security
 * requirements may dictate that this data be omitted to deter information
 * exposure.</p>
 *
 * <p>Note: all fields stored in Intel low-byte/high-byte order.</p>
 *
 * <pre>
 *          Value     Size     Description
 *          -----     ----     -----------
 * (CStore) 0x0019    2 bytes  Tag for this "extra" block type
 *          TSize     2 bytes  Size of the store data
 *          Version   2 bytes  Format version number - must 0x0001 at this time
 *          CStore    (var)    PKCS#7 data blob
 * </pre>
 *
 * <p><b>See the section describing the Strong Encryption Specification for
 * details. Refer to the section in this document entitled
 * "Incorporating PKWARE Proprietary Technology into Your Product" for more
 * information.</b></p>
 *
 * @NotThreadSafe
 * @since 1.11
 */
public class X0019_EncryptionRecipientCertificateList extends PKWareExtraHeader {

    public X0019_EncryptionRecipientCertificateList() {
        super(new ZipShort(0x0019));
    }

}
