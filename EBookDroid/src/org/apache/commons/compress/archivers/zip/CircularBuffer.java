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
 * Circular byte buffer.
 *
 * @author Emmanuel Bourg
 * @since 1.7
 */
class CircularBuffer {

    /** Size of the buffer */
    private final int size;

    /** The buffer */
    private final byte[] buffer;

    /** Index of the next data to be read from the buffer */
    private int readIndex;

    /** Index of the next data written in the buffer */
    private int writeIndex;

    CircularBuffer(final int size) {
        this.size = size;
        buffer = new byte[size];
    }

    /**
     * Tells if a new byte can be read from the buffer.
     */
    public boolean available() {
        return readIndex != writeIndex;
    }

    /**
     * Writes a byte to the buffer.
     */
    public void put(final int value) {
        buffer[writeIndex] = (byte) value;
        writeIndex = (writeIndex + 1) % size;
    }

    /**
     * Reads a byte from the buffer.
     */
    public int get() {
        if (available()) {
            final int value = buffer[readIndex];
            readIndex = (readIndex + 1) % size;
            return value & 0xFF;
        }
        return -1;
    }

    /**
     * Copy a previous interval in the buffer to the current position.
     *
     * @param distance the distance from the current write position
     * @param length   the number of bytes to copy
     */
    public void copy(final int distance, final int length) {
        final int pos1 = writeIndex - distance;
        final int pos2 = pos1 + length;
        for (int i = pos1; i < pos2; i++) {
            buffer[writeIndex] = buffer[(i + size) % size];
            writeIndex = (writeIndex + 1) % size;
        }
    }
}
