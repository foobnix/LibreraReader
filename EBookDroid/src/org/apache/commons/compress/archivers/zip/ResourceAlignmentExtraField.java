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


import java.util.zip.ZipException;

/**
 * An extra field who's sole purpose is to align and pad the local file header
 * so that the entry's data starts at a certain position.
 *
 * <p>The padding content of the padding is ignored and not retained
 * when reading a padding field.</p>
 *
 * <p>This enables Commons Compress to create "aligned" archives
 * similar to Android's zipalign command line tool.</p>
 *
 * @since 1.14
 * @see "https://developer.android.com/studio/command-line/zipalign.html"
 * @see ZipArchiveEntry#setAlignment
 */
public class ResourceAlignmentExtraField implements ZipExtraField {

    /**
     * Extra field id used for storing alignment and padding.
     */
    public static final ZipShort ID = new ZipShort(0xa11e);

    public static final int BASE_SIZE = 2;

    private static final int ALLOW_METHOD_MESSAGE_CHANGE_FLAG = 0x8000;

    private short alignment;

    private boolean allowMethodChange;

    private int padding = 0;

    public ResourceAlignmentExtraField() {
    }

    public ResourceAlignmentExtraField(int alignment) {
        this(alignment, false);
    }

    public ResourceAlignmentExtraField(int alignment, boolean allowMethodChange) {
        this(alignment, allowMethodChange, 0);
    }

    public ResourceAlignmentExtraField(int alignment, boolean allowMethodChange, int padding) {
        if (alignment < 0 || alignment > 0x7fff) {
            throw new IllegalArgumentException("Alignment must be between 0 and 0x7fff, was: " + alignment);
        }
        this.alignment = (short) alignment;
        this.allowMethodChange = allowMethodChange;
        this.padding = padding;
    }

    /**
     * Gets requested alignment.
     *
     * @return
     *      requested alignment.
     */
    public short getAlignment() {
        return alignment;
    }

    /**
     * Indicates whether method change is allowed when re-compressing the zip file.
     *
     * @return
     *      true if method change is allowed, false otherwise.
     */
    public boolean allowMethodChange() {
        return allowMethodChange;
    }

    @Override
    public ZipShort getHeaderId() {
        return ID;
    }

    @Override
    public ZipShort getLocalFileDataLength() {
        return new ZipShort(BASE_SIZE + padding);
    }

    @Override
    public ZipShort getCentralDirectoryLength() {
        return new ZipShort(BASE_SIZE);
    }

    @Override
    public byte[] getLocalFileDataData() {
        byte[] content = new byte[BASE_SIZE + padding];
        ZipShort.putShort(alignment | (allowMethodChange ? ALLOW_METHOD_MESSAGE_CHANGE_FLAG : 0),
                          content, 0);
        return content;
    }

    @Override
    public byte[] getCentralDirectoryData() {
        return ZipShort.getBytes(alignment | (allowMethodChange ? ALLOW_METHOD_MESSAGE_CHANGE_FLAG : 0));
    }

    @Override
    public void parseFromLocalFileData(byte[] buffer, int offset, int length) throws ZipException {
        parseFromCentralDirectoryData(buffer, offset, length);
        this.padding = length - BASE_SIZE;
    }

    @Override
    public void parseFromCentralDirectoryData(byte[] buffer, int offset, int length) throws ZipException {
        if (length < BASE_SIZE) {
            throw new ZipException("Too short content for ResourceAlignmentExtraField (0xa11e): " + length);
        }
        int alignmentValue = ZipShort.getValue(buffer, offset);
        this.alignment = (short) (alignmentValue & (ALLOW_METHOD_MESSAGE_CHANGE_FLAG - 1));
        this.allowMethodChange = (alignmentValue & ALLOW_METHOD_MESSAGE_CHANGE_FLAG) != 0;
    }
}
