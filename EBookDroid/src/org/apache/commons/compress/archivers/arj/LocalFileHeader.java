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
package org.apache.commons.compress.archivers.arj;

import java.util.Arrays;

class LocalFileHeader {
    int archiverVersionNumber;
    int minVersionToExtract;
    int hostOS;
    int arjFlags;
    int method;
    int fileType;
    int reserved;
    int dateTimeModified;
    long compressedSize;
    long originalSize;
    long originalCrc32;
    int fileSpecPosition;
    int fileAccessMode;
    int firstChapter;
    int lastChapter;

    int extendedFilePosition;
    int dateTimeAccessed;
    int dateTimeCreated;
    int originalSizeEvenForVolumes;

    String name;
    String comment;

    byte[][] extendedHeaders = null;

    static class Flags {
        static final int GARBLED = 0x01;
        static final int VOLUME = 0x04;
        static final int EXTFILE = 0x08;
        static final int PATHSYM = 0x10;
        static final int BACKUP = 0x20;
    }

    static class FileTypes {
        static final int BINARY = 0;
        static final int SEVEN_BIT_TEXT = 1;
        static final int DIRECTORY = 3;
        static final int VOLUME_LABEL = 4;
        static final int CHAPTER_LABEL = 5;
    }

    static class Methods {
        static final int STORED = 0;
        static final int COMPRESSED_MOST = 1;
        static final int COMPRESSED_FASTEST = 4;
        static final int NO_DATA_NO_CRC = 8;
        static final int NO_DATA = 9;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("LocalFileHeader [archiverVersionNumber=");
        builder.append(archiverVersionNumber);
        builder.append(", minVersionToExtract=");
        builder.append(minVersionToExtract);
        builder.append(", hostOS=");
        builder.append(hostOS);
        builder.append(", arjFlags=");
        builder.append(arjFlags);
        builder.append(", method=");
        builder.append(method);
        builder.append(", fileType=");
        builder.append(fileType);
        builder.append(", reserved=");
        builder.append(reserved);
        builder.append(", dateTimeModified=");
        builder.append(dateTimeModified);
        builder.append(", compressedSize=");
        builder.append(compressedSize);
        builder.append(", originalSize=");
        builder.append(originalSize);
        builder.append(", originalCrc32=");
        builder.append(originalCrc32);
        builder.append(", fileSpecPosition=");
        builder.append(fileSpecPosition);
        builder.append(", fileAccessMode=");
        builder.append(fileAccessMode);
        builder.append(", firstChapter=");
        builder.append(firstChapter);
        builder.append(", lastChapter=");
        builder.append(lastChapter);
        builder.append(", extendedFilePosition=");
        builder.append(extendedFilePosition);
        builder.append(", dateTimeAccessed=");
        builder.append(dateTimeAccessed);
        builder.append(", dateTimeCreated=");
        builder.append(dateTimeCreated);
        builder.append(", originalSizeEvenForVolumes=");
        builder.append(originalSizeEvenForVolumes);
        builder.append(", name=");
        builder.append(name);
        builder.append(", comment=");
        builder.append(comment);
        builder.append(", extendedHeaders=");
        builder.append(Arrays.toString(extendedHeaders));
        builder.append("]");
        return builder.toString();
    }
}
