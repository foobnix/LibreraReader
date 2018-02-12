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

import java.security.cert.Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

/**
 *
 * @NotThreadSafe (parent is not thread-safe)
 */
public class JarArchiveEntry extends ZipArchiveEntry {

    // These are always null - see https://issues.apache.org/jira/browse/COMPRESS-18 for discussion
    private final Attributes manifestAttributes = null;
    private final Certificate[] certificates = null;

    public JarArchiveEntry(final ZipEntry entry) throws ZipException {
        super(entry);
    }

    public JarArchiveEntry(final String name) {
        super(name);
    }

    public JarArchiveEntry(final ZipArchiveEntry entry) throws ZipException {
        super(entry);
    }

    public JarArchiveEntry(final JarEntry entry) throws ZipException {
        super(entry);

    }

    /**
     * This method is not implemented and won't ever be.
     * The JVM equivalent has a different name {@link java.util.jar.JarEntry#getAttributes()}
     *
     * @deprecated since 1.5, do not use; always returns null
     * @return Always returns null.
     */
    @Deprecated
    public Attributes getManifestAttributes() {
        return manifestAttributes;
    }

    /**
     * Return a copy of the list of certificates or null if there are none.
     *
     * @return Always returns null in the current implementation
     *
     * @deprecated since 1.5, not currently implemented
     */
    @Deprecated
    public Certificate[] getCertificates() {
        if (certificates != null) { // never true currently // NOSONAR
            final Certificate[] certs = new Certificate[certificates.length];
            System.arraycopy(certificates, 0, certs, 0, certs.length);
            return certs;
        }
        /*
         * Note, the method
         * Certificate[] java.util.jar.JarEntry.getCertificates()
         * also returns null or the list of certificates (but not copied)
         */
        return null;
    }

}
