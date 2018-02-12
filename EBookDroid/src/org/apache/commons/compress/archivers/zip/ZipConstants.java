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
 * Various constants used throughout the package.
 *
 * @since 1.3
 */
final class ZipConstants {
    /** Masks last eight bits */
    static final int BYTE_MASK = 0xFF;

    /** length of a ZipShort in bytes */
    static final int SHORT = 2;

    /** length of a ZipLong in bytes */
    static final int WORD = 4;

    /** length of a ZipEightByteInteger in bytes */
    static final int DWORD = 8;

    /** Initial ZIP specification version */
    static final int INITIAL_VERSION = 10;

    /**
     * ZIP specification version that introduced DEFLATE compression method.
     * @since 1.15
     */
    static final int DEFLATE_MIN_VERSION = 20;

    /** ZIP specification version that introduced data descriptor method */
    static final int DATA_DESCRIPTOR_MIN_VERSION = 20;

    /** ZIP specification version that introduced ZIP64 */
    static final int ZIP64_MIN_VERSION = 45;

    /**
     * Value stored in two-byte size and similar fields if ZIP64
     * extensions are used.
     */
    static final int ZIP64_MAGIC_SHORT = 0xFFFF;

    /**
     * Value stored in four-byte size and similar fields if ZIP64
     * extensions are used.
     */
    static final long ZIP64_MAGIC = 0xFFFFFFFFL;

    private ZipConstants() { }

}
