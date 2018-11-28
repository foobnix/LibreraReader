/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.commons.compress.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Charsets required of every implementation of the Java platform.
 *
 * From the Java documentation <a href="https://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">Standard
 * charsets</a>:
 * <p>
 * <cite>Every implementation of the Java platform is required to support the following character encodings. Consult the
 * release documentation for your implementation to see if any other encodings are supported. Consult the release
 * documentation for your implementation to see if any other encodings are supported. </cite>
 * </p>
 *
 * <dl>
 * <dt><code>US-ASCII</code></dt>
 * <dd>Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set.</dd>
 * <dt><code>ISO-8859-1</code></dt>
 * <dd>ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.</dd>
 * <dt><code>UTF-8</code></dt>
 * <dd>Eight-bit Unicode Transformation Format.</dd>
 * <dt><code>UTF-16BE</code></dt>
 * <dd>Sixteen-bit Unicode Transformation Format, big-endian byte order.</dd>
 * <dt><code>UTF-16LE</code></dt>
 * <dd>Sixteen-bit Unicode Transformation Format, little-endian byte order.</dd>
 * <dt><code>UTF-16</code></dt>
 * <dd>Sixteen-bit Unicode Transformation Format, byte order specified by a mandatory initial byte-order mark (either order
 * accepted on input, big-endian used on output.)</dd>
 * </dl>
 *
 * <p>This class best belongs in the Commons Lang or IO project. Even if a similar class is defined in another Commons
 * component, it is not foreseen that Commons Compress would be made to depend on another Commons component.</p>
 *
 * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
 * @see StandardCharsets
 * @since 1.4
 */
public class Charsets {

    //
    // This class should only contain Charset instances for required encodings. This guarantees that it will load correctly and
    // without delay on all Java platforms.
    //

    /**
     * Returns the given Charset or the default Charset if the given Charset is null.
     *
     * @param charset
     *            A charset or null.
     * @return the given Charset or the default Charset if the given Charset is null
     */
    public static Charset toCharset(final Charset charset) {
        return charset == null ? Charset.defaultCharset() : charset;
    }

    /**
     * Returns a Charset for the named charset. If the name is null, return the default Charset.
     *
     * @param charset
     *            The name of the requested charset, may be null.
     * @return a Charset for the named charset
     * @throws java.nio.charset.UnsupportedCharsetException
     *             If the named charset is unavailable
     * @throws java.nio.charset.IllegalCharsetNameException
     *             If the given charset name is illegal
     */
    public static Charset toCharset(final String charset) {
        return charset == null ? Charset.defaultCharset() : Charset.forName(charset);
    }

    /**
     * CharsetNamesISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     * @deprecated replaced by {@link StandardCharsets} in Java 7
     */
    public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

    /**
     * <p>
     * Seven-bit ASCII, also known as ISO646-US, also known as the Basic Latin block of the Unicode character set.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     * @deprecated replaced by {@link StandardCharsets} in Java 7
     */
    public static final Charset US_ASCII = StandardCharsets.US_ASCII;

    /**
     * <p>
     * Sixteen-bit Unicode Transformation Format, The byte order specified by a mandatory initial byte-order mark
     * (either order accepted on input, big-endian used on output)
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     * @deprecated replaced by {@link StandardCharsets} in Java 7
     */
    public static final Charset UTF_16 = StandardCharsets.UTF_16;

    /**
     * <p>
     * Sixteen-bit Unicode Transformation Format, big-endian byte order.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     * @deprecated replaced by {@link StandardCharsets} in Java 7
     */
    public static final Charset UTF_16BE = StandardCharsets.UTF_16BE;

    /**
     * <p>
     * Sixteen-bit Unicode Transformation Format, little-endian byte order.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     * @deprecated replaced by {@link StandardCharsets} in Java 7
     */
    public static final Charset UTF_16LE = StandardCharsets.UTF_16LE;

    /**
     * <p>
     * Eight-bit Unicode Transformation Format.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     * @deprecated replaced by {@link StandardCharsets} in Java 7
     */
    public static final Charset UTF_8 = StandardCharsets.UTF_8;
}
