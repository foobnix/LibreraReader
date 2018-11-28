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
 * Provides information about a scatter compression run.
 *
 * @since 1.10
 */
public class ScatterStatistics {
    private final long compressionElapsed;
    private final long mergingElapsed;

    ScatterStatistics(final long compressionElapsed, final long mergingElapsed) {
        this.compressionElapsed = compressionElapsed;
        this.mergingElapsed = mergingElapsed;
    }

    /**
     * The number of milliseconds elapsed in the parallel compression phase
     * @return The number of milliseconds elapsed
     */
    public long getCompressionElapsed() {
        return compressionElapsed;
    }

    /**
     * The number of milliseconds elapsed in merging the results of the parallel compression, the IO phase
     * @return The number of milliseconds elapsed
     */
    public long getMergingElapsed() {
        return mergingElapsed;
    }

    @Override
    public String toString() {
        return "compressionElapsed=" + compressionElapsed + "ms, mergingElapsed=" + mergingElapsed + "ms";
    }

}
