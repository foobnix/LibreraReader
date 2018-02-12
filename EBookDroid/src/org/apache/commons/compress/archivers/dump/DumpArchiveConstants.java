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

/**
 * Various constants associated with dump archives.
 */
public final class DumpArchiveConstants {
    public static final int TP_SIZE = 1024;
    public static final int NTREC = 10;
    public static final int HIGH_DENSITY_NTREC = 32;
    public static final int OFS_MAGIC = 60011;
    public static final int NFS_MAGIC = 60012;
    public static final int FS_UFS2_MAGIC = 0x19540119;
    public static final int CHECKSUM = 84446;
    public static final int LBLSIZE = 16;
    public static final int NAMELEN = 64;

    /* do not instantiate */
    private DumpArchiveConstants() {
    }

    /**
     * The type of tape segment.
     */
    public enum SEGMENT_TYPE {
        TAPE(1),
        INODE(2),
        BITS(3),
        ADDR(4),
        END(5),
        CLRI(6);

        int code;

        SEGMENT_TYPE(final int code) {
            this.code = code;
        }

        public static SEGMENT_TYPE find(final int code) {
            for (final SEGMENT_TYPE t : values()) {
                if (t.code == code) {
                    return t;
                }
            }

            return null;
        }
    }

    /**
     * The type of compression.
     */
    public enum COMPRESSION_TYPE {
        ZLIB(0),
        BZLIB(1),
        LZO(2);

        int code;

        COMPRESSION_TYPE(final int code) {
            this.code = code;
        }

        public static COMPRESSION_TYPE find(final int code) {
            for (final COMPRESSION_TYPE t : values()) {
                if (t.code == code) {
                    return t;
                }
            }

            return null;
        }
    }
}
