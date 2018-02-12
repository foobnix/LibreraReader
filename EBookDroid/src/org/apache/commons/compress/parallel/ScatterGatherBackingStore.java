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
package org.apache.commons.compress.parallel;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>Store intermediate payload in a scatter-gather scenario.
 * Multiple threads write their payload to a backing store, which can
 * subsequently be reversed to an {@link InputStream} to be used as input in the
 * gather phase.</p>
 *
 * <p>It is the responsibility of the allocator of an instance of this class
 * to close this. Closing it should clear off any allocated structures
 * and preferably delete files.</p>
 *
 * @since 1.10
 */
public interface ScatterGatherBackingStore extends Closeable {

    /**
     * An input stream that contains the scattered payload
     *
     * @return An InputStream, should be closed by the caller of this method.
     * @throws IOException when something fails
     */
    InputStream getInputStream() throws IOException;

    /**
     * Writes a piece of payload.
     *
     * @param data the data to write
     * @param offset offset inside data to start writing from
     * @param length the amount of data to write
     * @throws IOException when something fails
     */
    void writeOut(byte[] data, int offset, int length) throws IOException;

    /**
     * Closes this backing store for further writing.
     * @throws IOException when something fails
     */
    void closeForWriting() throws IOException;
}
