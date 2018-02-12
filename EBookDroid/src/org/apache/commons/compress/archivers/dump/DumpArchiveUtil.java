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
package org.apache.commons.compress.archivers.dump;

import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.utils.ByteUtils;

/**
 * Various utilities for dump archives.
 */
class DumpArchiveUtil {
    /**
     * Private constructor to prevent instantiation.
     */
    private DumpArchiveUtil() {
    }

    /**
     * Calculate checksum for buffer.
     *
     * @param buffer buffer containing tape segment header
     * @returns checksum
     */
    public static int calculateChecksum(final byte[] buffer) {
        int calc = 0;

        for (int i = 0; i < 256; i++) {
            calc += DumpArchiveUtil.convert32(buffer, 4 * i);
        }

        return DumpArchiveConstants.CHECKSUM -
        (calc - DumpArchiveUtil.convert32(buffer, 28));
    }

    /**
     * Verify that the buffer contains a tape segment header.
     *
     * @param buffer
     */
    public static final boolean verify(final byte[] buffer) {
        // verify magic. for now only accept NFS_MAGIC.
        final int magic = convert32(buffer, 24);

        if (magic != DumpArchiveConstants.NFS_MAGIC) {
            return false;
        }

        //verify checksum...
        final int checksum = convert32(buffer, 28);

        return checksum == calculateChecksum(buffer);
    }

    /**
     * Get the ino associated with this buffer.
     *
     * @param buffer
     */
    public static final int getIno(final byte[] buffer) {
        return convert32(buffer, 20);
    }

    /**
     * Read 8-byte integer from buffer.
     *
     * @param buffer
     * @param offset
     * @return the 8-byte entry as a long
     */
    public static final long convert64(final byte[] buffer, final int offset) {
        return ByteUtils.fromLittleEndian(buffer, offset, 8);
    }

    /**
     * Read 4-byte integer from buffer.
     *
     * @param buffer
     * @param offset
     * @return the 4-byte entry as an int
     */
    public static final int convert32(final byte[] buffer, final int offset) {
        return (int) ByteUtils.fromLittleEndian(buffer, offset, 4);
    }

    /**
     * Read 2-byte integer from buffer.
     *
     * @param buffer
     * @param offset
     * @return the 2-byte entry as an int
     */
    public static final int convert16(final byte[] buffer, final int offset) {
        return (int) ByteUtils.fromLittleEndian(buffer, offset, 2);
    }

    /**
     * Decodes a byte array to a string.
     */
    static String decode(final ZipEncoding encoding, final byte[] b, final int offset, final int len)
        throws IOException {
        return encoding.decode(Arrays.copyOfRange(b, offset, offset + len));
    }
}
