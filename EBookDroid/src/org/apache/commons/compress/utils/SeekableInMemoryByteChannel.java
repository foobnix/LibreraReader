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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link SeekableByteChannel} implementation that wraps a byte[].
 *
 * <p>When this channel is used for writing an internal buffer grows to accommodate
 * incoming data. A natural size limit is the value of {@link Integer#MAX_VALUE}.
 * Internal buffer can be accessed via {@link SeekableInMemoryByteChannel#array()}.</p>
 *
 * @since 1.13
 * @NotThreadSafe
 */
public class SeekableInMemoryByteChannel implements SeekableByteChannel {

    private static final int NAIVE_RESIZE_LIMIT = Integer.MAX_VALUE >> 1;

    private byte[] data;
    private final AtomicBoolean closed = new AtomicBoolean();
    private int position, size;

    /**
     * Constructor taking a byte array.
     *
     * <p>This constructor is intended to be used with pre-allocated buffer or when
     * reading from a given byte array.</p>
     *
     * @param data input data or pre-allocated array.
     */
    public SeekableInMemoryByteChannel(byte[] data) {
        this.data = data;
        size = data.length;
    }

    /**
     * Parameterless constructor - allocates internal buffer by itself.
     */
    public SeekableInMemoryByteChannel() {
        this(new byte[0]);
    }

    /**
     * Constructor taking a size of storage to be allocated.
     *
     * <p>Creates a channel and allocates internal storage of a given size.</p>
     *
     * @param size size of internal buffer to allocate, in bytes.
     */
    public SeekableInMemoryByteChannel(int size) {
        this(new byte[size]);
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        ensureOpen();
        if (newPosition < 0L || newPosition > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Position has to be in range 0.. " + Integer.MAX_VALUE);
        }
        position = (int) newPosition;
        return this;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long newSize) {
        if (size > newSize) {
            size = (int) newSize;
        }
        repositionIfNecessary();
        return this;
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        ensureOpen();
        repositionIfNecessary();
        int wanted = buf.remaining();
        int possible = size - position;
        if (possible <= 0) {
            return -1;
        }
        if (wanted > possible) {
            wanted = possible;
        }
        buf.put(data, position, wanted);
        position += wanted;
        return wanted;
    }

    @Override
    public void close() {
        closed.set(true);
    }

    @Override
    public boolean isOpen() {
        return !closed.get();
    }

    @Override
    public int write(ByteBuffer b) throws IOException {
        ensureOpen();
        int wanted = b.remaining();
        int possibleWithoutResize = size - position;
        if (wanted > possibleWithoutResize) {
            int newSize = position + wanted;
            if (newSize < 0) { // overflow
                resize(Integer.MAX_VALUE);
                wanted = Integer.MAX_VALUE - position;
            } else {
                resize(newSize);
            }
        }
        b.get(data, position, wanted);
        position += wanted;
        if (size < position) {
            size = position;
        }
        return wanted;
    }

    /**
     * Obtains the array backing this channel.
     *
     * <p>NOTE:
     * The returned buffer is not aligned with containing data, use
     * {@link #size()} to obtain the size of data stored in the buffer.</p>
     *
     * @return internal byte array.
     */
    public byte[] array() {
        return data;
    }

    private void resize(int newLength) {
        int len = data.length;
        if (len <= 0) {
            len = 1;
        }
        if (newLength < NAIVE_RESIZE_LIMIT) {
            while (len < newLength) {
                len <<= 1;
            }
        } else { // avoid overflow
            len = newLength;
        }
        data = Arrays.copyOf(data, len);
    }

    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    private void repositionIfNecessary() {
        if (position > size) {
            position = size;
        }
    }

}
