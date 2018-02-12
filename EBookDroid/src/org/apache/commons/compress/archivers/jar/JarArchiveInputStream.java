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
package org.apache.commons.compress.archivers.jar;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

/**
 * Implements an input stream that can read entries from jar files.
 *
 * @NotThreadSafe
 */
public class JarArchiveInputStream extends ZipArchiveInputStream {

    /**
     * Creates an instance from the input stream using the default encoding.
     *
     * @param inputStream the input stream to wrap
     */
    public JarArchiveInputStream( final InputStream inputStream ) {
        super(inputStream);
    }

    /**
     * Creates an instance from the input stream using the specified encoding.
     *
     * @param inputStream the input stream to wrap
     * @param encoding the encoding to use
     * @since 1.10
     */
    public JarArchiveInputStream( final InputStream inputStream, final String encoding ) {
        super(inputStream, encoding);
    }

    public JarArchiveEntry getNextJarEntry() throws IOException {
        final ZipArchiveEntry entry = getNextZipEntry();
        return entry == null ? null : new JarArchiveEntry(entry);
    }

    @Override
    public ArchiveEntry getNextEntry() throws IOException {
        return getNextJarEntry();
    }

    /**
     * Checks if the signature matches what is expected for a jar file
     * (in this case it is the same as for a zip file).
     *
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this stream is a jar archive stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length ) {
        return ZipArchiveInputStream.matches(signature, length);
    }
}
