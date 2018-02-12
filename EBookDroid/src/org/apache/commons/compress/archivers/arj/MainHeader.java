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

class MainHeader {
    int archiverVersionNumber;
    int minVersionToExtract;
    int hostOS;
    int arjFlags;
    int securityVersion;
    int fileType;
    int reserved;
    int dateTimeCreated;
    int dateTimeModified;
    long archiveSize;
    int securityEnvelopeFilePosition;
    int fileSpecPosition;
    int securityEnvelopeLength;
    int encryptionVersion;
    int lastChapter;
    int arjProtectionFactor;
    int arjFlags2;
    String name;
    String comment;
    byte[] extendedHeaderBytes = null;

    static class Flags {
        static final int GARBLED = 0x01;
        static final int OLD_SECURED_NEW_ANSI_PAGE = 0x02;
        static final int VOLUME = 0x04;
        static final int ARJPROT = 0x08;
        static final int PATHSYM = 0x10;
        static final int BACKUP = 0x20;
        static final int SECURED = 0x40;
        static final int ALTNAME = 0x80;
    }


    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MainHeader [archiverVersionNumber=");
        builder.append(archiverVersionNumber);
        builder.append(", minVersionToExtract=");
        builder.append(minVersionToExtract);
        builder.append(", hostOS=");
        builder.append(hostOS);
        builder.append(", arjFlags=");
        builder.append(arjFlags);
        builder.append(", securityVersion=");
        builder.append(securityVersion);
        builder.append(", fileType=");
        builder.append(fileType);
        builder.append(", reserved=");
        builder.append(reserved);
        builder.append(", dateTimeCreated=");
        builder.append(dateTimeCreated);
        builder.append(", dateTimeModified=");
        builder.append(dateTimeModified);
        builder.append(", archiveSize=");
        builder.append(archiveSize);
        builder.append(", securityEnvelopeFilePosition=");
        builder.append(securityEnvelopeFilePosition);
        builder.append(", fileSpecPosition=");
        builder.append(fileSpecPosition);
        builder.append(", securityEnvelopeLength=");
        builder.append(securityEnvelopeLength);
        builder.append(", encryptionVersion=");
        builder.append(encryptionVersion);
        builder.append(", lastChapter=");
        builder.append(lastChapter);
        builder.append(", arjProtectionFactor=");
        builder.append(arjProtectionFactor);
        builder.append(", arjFlags2=");
        builder.append(arjFlags2);
        builder.append(", name=");
        builder.append(name);
        builder.append(", comment=");
        builder.append(comment);
        builder.append(", extendedHeaderBytes=");
        builder.append(Arrays.toString(extendedHeaderBytes));
        builder.append("]");
        return builder.toString();
    }
}
