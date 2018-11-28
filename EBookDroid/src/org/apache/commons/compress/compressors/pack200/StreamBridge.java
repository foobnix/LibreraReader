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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides an InputStream to read all data written to this
 * OutputStream.
 *
 * @ThreadSafe
 * @since 1.3
 */
abstract class StreamBridge extends FilterOutputStream {
    private InputStream input;
    private final Object inputLock = new Object();

    protected StreamBridge(final OutputStream out) {
        super(out);
    }

    protected StreamBridge() {
        this(null);
    }

    /**
     * Provides the input view.
     */
    InputStream getInput() throws IOException {
        synchronized (inputLock) {
            if (input == null) {
                input = getInputView();
            }
        }
        return input;
    }

    /**
     * Creates the input view.
     */
    abstract InputStream getInputView() throws IOException;

    /**
     * Closes input and output and releases all associated resources.
     */
    void stop() throws IOException {
        close();
        synchronized (inputLock) {
            if (input != null) {
                input.close();
                input = null;
            }
        }
    }
}
