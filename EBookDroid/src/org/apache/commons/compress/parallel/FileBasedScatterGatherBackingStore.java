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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * ScatterGatherBackingStore that is backed by a file.
 *
 * @since 1.10
 */
public class FileBasedScatterGatherBackingStore implements ScatterGatherBackingStore {
    private final File target;
    private final OutputStream os;
    private boolean closed;

    public FileBasedScatterGatherBackingStore(final File target) throws FileNotFoundException {
        this.target = target;
        try {
            os = Files.newOutputStream(target.toPath());
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            // must convert exception to stay backwards compatible with Compress 1.10 to 1.13
            throw new RuntimeException(ex); // NOSONAR
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(target.toPath());
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void closeForWriting() throws IOException {
        if (!closed) {
            os.close();
            closed = true;
        }
    }

    @Override
    public void writeOut(final byte[] data, final int offset, final int length) throws IOException {
        os.write(data, offset, length);
    }

    @Override
    public void close() throws IOException {
        try {
            closeForWriting();
        } finally {
            target.delete();
        }
    }
}
