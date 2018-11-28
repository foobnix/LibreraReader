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

import java.util.zip.ZipException;

/**
 * Exception thrown when attempting to write data that requires Zip64
 * support to an archive and {@link ZipArchiveOutputStream#setUseZip64
 * UseZip64} has been set to {@link Zip64Mode#Never Never}.
 * @since 1.3
 */
public class Zip64RequiredException extends ZipException {

    private static final long serialVersionUID = 20110809L;

    /**
     * Helper to format "entry too big" messages.
     */
    static String getEntryTooBigMessage(final ZipArchiveEntry ze) {
        return ze.getName() + "'s size exceeds the limit of 4GByte.";
    }

    static final String ARCHIVE_TOO_BIG_MESSAGE =
        "archive's size exceeds the limit of 4GByte.";

    static final String TOO_MANY_ENTRIES_MESSAGE =
        "archive contains more than 65535 entries.";

    public Zip64RequiredException(final String reason) {
        super(reason);
    }
}
