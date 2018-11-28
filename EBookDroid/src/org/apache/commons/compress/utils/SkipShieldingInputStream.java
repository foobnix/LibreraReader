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
package org.apache.commons.compress.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper that overwrites {@link #skip} and delegates to {@link #read} instead.
 *
 * <p>Some implementations of {@link InputStream} implement {@link
 * InputStream#skip} in a way that throws an exception if the stream
 * is not seekable - {@link System#in System.in} is known to behave
 * that way. For such a stream it is impossible to invoke skip at all
 * and you have to read from the stream (and discard the data read)
 * instead. Skipping is potentially much faster than reading so we do
 * want to invoke {@code skip} when possible. We provide this class so
 * you can wrap your own {@link InputStream} in it if you encounter
 * problems with {@code skip} throwing an excpetion.</p>
 *
 * @since 1.17
 */
public class SkipShieldingInputStream extends FilterInputStream {
    private static final int SKIP_BUFFER_SIZE = 8192;
    // we can use a shared buffer as the content is discarded anyway
    private static final byte[] SKIP_BUFFER = new byte[SKIP_BUFFER_SIZE];
    public SkipShieldingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public long skip(long n) throws IOException {
        return n < 0 ? 0 : read(SKIP_BUFFER, 0, (int) Math.min(n, SKIP_BUFFER_SIZE));
    }
}
