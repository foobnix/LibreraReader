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
 * Wrapper for extra field data that doesn't conform to the recommended format of header-tag + size + data.
 *
 * <p>The header-id is artificial (and not listed as a known ID in <a
 * href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">APPNOTE.TXT</a>).  Since it isn't used anywhere
 * except to satisfy the ZipExtraField contract it shouldn't matter anyway.</p>
 *
 * @since 1.1
 * @NotThreadSafe
 */
public final class UnparseableExtraFieldData implements ZipExtraField {
    private static final ZipShort HEADER_ID = new ZipShort(0xACC1);

    private byte[] localFileData;
    private byte[] centralDirectoryData;

    /**
     * The Header-ID.
     *
     * @return a completely arbitrary value that should be ignored.
     */
    @Override
    public ZipShort getHeaderId() {
        return HEADER_ID;
    }

    /**
     * Length of the complete extra field in the local file data.
     *
     * @return The LocalFileDataLength value
     */
    @Override
    public ZipShort getLocalFileDataLength() {
        return new ZipShort(localFileData == null ? 0 : localFileData.length);
    }

    /**
     * Length of the complete extra field in the central directory.
     *
     * @return The CentralDirectoryLength value
     */
    @Override
    public ZipShort getCentralDirectoryLength() {
        return centralDirectoryData == null
            ? getLocalFileDataLength()
            : new ZipShort(centralDirectoryData.length);
    }

    /**
     * The actual data to put into local file data.
     *
     * @return The LocalFileDataData value
     */
    @Override
    public byte[] getLocalFileDataData() {
        return ZipUtil.copy(localFileData);
    }

    /**
     * The actual data to put into central directory.
     *
     * @return The CentralDirectoryData value
     */
    @Override
    public byte[] getCentralDirectoryData() {
        return centralDirectoryData == null
            ? getLocalFileDataData() : ZipUtil.copy(centralDirectoryData);
    }

    /**
     * Populate data from this array as if it was in local file data.
     *
     * @param buffer the buffer to read data from
     * @param offset offset into buffer to read data
     * @param length the length of data
     */
    @Override
    public void parseFromLocalFileData(final byte[] buffer, final int offset, final int length) {
        localFileData = new byte[length];
        System.arraycopy(buffer, offset, localFileData, 0, length);
    }

    /**
     * Populate data from this array as if it was in central directory data.
     *
     * @param buffer the buffer to read data from
     * @param offset offset into buffer to read data
     * @param length the length of data
     */
    @Override
    public void parseFromCentralDirectoryData(final byte[] buffer, final int offset,
                                              final int length) {
        centralDirectoryData = new byte[length];
        System.arraycopy(buffer, offset, centralDirectoryData, 0, length);
        if (localFileData == null) {
            parseFromLocalFileData(buffer, offset, length);
        }
    }

}
