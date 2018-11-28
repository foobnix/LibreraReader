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

package org.apache.commons.compress.compressors.pack200;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

/**
 * Utility methods for Pack200.
 *
 * @ThreadSafe
 * @since 1.3
 */
public class Pack200Utils {
    private Pack200Utils() { }

    /**
     * Normalizes a JAR archive in-place so it can be safely signed
     * and packed.
     *
     * <p>As stated in <a
     * href="https://download.oracle.com/javase/1.5.0/docs/api/java/util/jar/Pack200.Packer.html">Pack200.Packer's</a>
     * javadocs applying a Pack200 compression to a JAR archive will
     * in general make its sigantures invalid.  In order to prepare a
     * JAR for signing it should be "normalized" by packing and
     * unpacking it.  This is what this method does.</p>
     *
     * <p>Note this methods implicitly sets the segment length to
     * -1.</p>
     *
     * @param jar the JAR archive to normalize
     * @throws IOException if reading or writing fails
     */
    public static void normalize(final File jar)
        throws IOException {
        normalize(jar, jar, null);
    }

    /**
     * Normalizes a JAR archive in-place so it can be safely signed
     * and packed.
     *
     * <p>As stated in <a
     * href="https://download.oracle.com/javase/1.5.0/docs/api/java/util/jar/Pack200.Packer.html">Pack200.Packer's</a>
     * javadocs applying a Pack200 compression to a JAR archive will
     * in general make its sigantures invalid.  In order to prepare a
     * JAR for signing it should be "normalized" by packing and
     * unpacking it.  This is what this method does.</p>
     *
     * @param jar the JAR archive to normalize
     * @param props properties to set for the pack operation.  This
     * method will implicitly set the segment limit to -1.
     * @throws IOException if reading or writing fails
     */
    public static void normalize(final File jar, final Map<String, String> props)
        throws IOException {
        normalize(jar, jar, props);
    }

    /**
     * Normalizes a JAR archive so it can be safely signed and packed.
     *
     * <p>As stated in <a
     * href="https://download.oracle.com/javase/1.5.0/docs/api/java/util/jar/Pack200.Packer.html">Pack200.Packer's</a>
     * javadocs applying a Pack200 compression to a JAR archive will
     * in general make its sigantures invalid.  In order to prepare a
     * JAR for signing it should be "normalized" by packing and
     * unpacking it.  This is what this method does.</p>
     *
     * <p>This method does not replace the existing archive but creates
     * a new one.</p>
     *
     * <p>Note this methods implicitly sets the segment length to
     * -1.</p>
     *
     * @param from the JAR archive to normalize
     * @param to the normalized archive
     * @throws IOException if reading or writing fails
     */
    public static void normalize(final File from, final File to)
        throws IOException {
        normalize(from, to, null);
    }

    /**
     * Normalizes a JAR archive so it can be safely signed and packed.
     *
     * <p>As stated in <a
     * href="https://download.oracle.com/javase/1.5.0/docs/api/java/util/jar/Pack200.Packer.html">Pack200.Packer's</a>
     * javadocs applying a Pack200 compression to a JAR archive will
     * in general make its sigantures invalid.  In order to prepare a
     * JAR for signing it should be "normalized" by packing and
     * unpacking it.  This is what this method does.</p>
     *
     * <p>This method does not replace the existing archive but creates
     * a new one.</p>
     *
     * @param from the JAR archive to normalize
     * @param to the normalized archive
     * @param props properties to set for the pack operation.  This
     * method will implicitly set the segment limit to -1.
     * @throws IOException if reading or writing fails
     */
    public static void normalize(final File from, final File to, Map<String, String> props)
            throws IOException {
        if (props == null) {
            props = new HashMap<>();
        }
        props.put(Pack200.Packer.SEGMENT_LIMIT, "-1");
        final File tempFile = File.createTempFile("commons-compress", "pack200normalize");
        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                    JarFile jarFile = new JarFile(from)) {
                final Pack200.Packer packer = Pack200.newPacker();
                packer.properties().putAll(props);
                packer.pack(jarFile, fos);
            }
            final Pack200.Unpacker unpacker = Pack200.newUnpacker();
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(to))) {
                unpacker.unpack(tempFile, jos);
            }
        } finally {
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }
}
