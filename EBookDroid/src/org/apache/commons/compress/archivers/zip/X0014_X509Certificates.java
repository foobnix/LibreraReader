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
 * PKCS#7 Store for X.509 Certificates (0x0014).
 *
 * <p>This field MUST contain information about each of the certificates files may
 * be signed with. When the Central Directory Encryption feature is enabled for
 * a ZIP file, this record will appear in the Archive Extra Data Record,
 * otherwise it will appear in the first central directory record and will be
 * ignored in any other record.</p>
 *
 * <p>Note: all fields stored in Intel low-byte/high-byte order.</p>
 *
 * <pre>
 *         Value     Size     Description
 *         -----     ----     -----------
 * (Store) 0x0014    2 bytes  Tag for this "extra" block type
 *         TSize     2 bytes  Size of the store data
 *         TData     TSize    Data about the store
 * </pre>
 *
 * @NotThreadSafe
 * @since 1.11
 */
public class X0014_X509Certificates extends PKWareExtraHeader {

    public X0014_X509Certificates() {
        super(new ZipShort(0x0014));
    }

}
