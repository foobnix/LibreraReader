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
 * Info-ZIP Unicode Path Extra Field (0x7075):
 *
 * <p>Stores the UTF-8 version of the file name field as stored in the
 * local header and central directory header.</p>
 *
 * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">PKWARE
 * APPNOTE.TXT, section 4.6.9</a>
 *
 * @NotThreadSafe super-class is not thread-safe
 */
public class UnicodePathExtraField extends AbstractUnicodeExtraField {

    public static final ZipShort UPATH_ID = new ZipShort(0x7075);

    public UnicodePathExtraField () {
    }

    /**
     * Assemble as unicode path extension from the name given as
     * text as well as the encoded bytes actually written to the archive.
     *
     * @param text The file name
     * @param bytes the bytes actually written to the archive
     * @param off The offset of the encoded filename in <code>bytes</code>.
     * @param len The length of the encoded filename or comment in
     * <code>bytes</code>.
     */
    public UnicodePathExtraField(final String text, final byte[] bytes, final int off, final int len) {
        super(text, bytes, off, len);
    }

    /**
     * Assemble as unicode path extension from the name given as
     * text as well as the encoded bytes actually written to the archive.
     *
     * @param name The file name
     * @param bytes the bytes actually written to the archive
     */
    public UnicodePathExtraField(final String name, final byte[] bytes) {
        super(name, bytes);
    }

    @Override
    public ZipShort getHeaderId() {
        return UPATH_ID;
    }
}
