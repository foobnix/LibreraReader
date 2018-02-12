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
 * Constants from stat.h on Unix systems.
 */
// CheckStyle:InterfaceIsTypeCheck OFF - backward compatible
public interface UnixStat {

    /**
     * Bits used for permissions (and sticky bit)
     */
    int PERM_MASK = 07777;
    /**
     * Bits used to indicate the filesystem object type.
     * @since 1.14
     */
    int FILE_TYPE_FLAG = 0170000;
    /**
     * Indicates symbolic links.
     */
    int LINK_FLAG = 0120000;
    /**
     * Indicates plain files.
     */
    int FILE_FLAG = 0100000;
    /**
     * Indicates directories.
     */
    int DIR_FLAG = 040000;

    // ----------------------------------------------------------
    // somewhat arbitrary choices that are quite common for shared
    // installations
    // -----------------------------------------------------------

    /**
     * Default permissions for symbolic links.
     */
    int DEFAULT_LINK_PERM = 0777;

    /**
     * Default permissions for directories.
     */
    int DEFAULT_DIR_PERM = 0755;

    /**
     * Default permissions for plain files.
     */
    int DEFAULT_FILE_PERM = 0644;
}
