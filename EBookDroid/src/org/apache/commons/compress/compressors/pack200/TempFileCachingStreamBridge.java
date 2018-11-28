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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * StreamSwitcher that caches all data written to the output side in
 * a temporary file.
 * @since 1.3
 */
class TempFileCachingStreamBridge extends StreamBridge {
    private final File f;

    TempFileCachingStreamBridge() throws IOException {
        f = File.createTempFile("commons-compress", "packtemp");
        f.deleteOnExit();
        out = Files.newOutputStream(f.toPath());
    }

    @Override
    InputStream getInputView() throws IOException {
        out.close();
        return new FilterInputStream(Files.newInputStream(f.toPath())) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    f.delete();
                }
            }
        };
    }
}
