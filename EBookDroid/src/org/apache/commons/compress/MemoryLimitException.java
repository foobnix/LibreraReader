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
package org.apache.commons.compress;

import java.io.IOException;

/**
 * If a stream checks for estimated memory allocation, and the estimate
 * goes above the memory limit, this is thrown.  This can also be thrown
 * if a stream tries to allocate a byte array that is larger than
 * the allowable limit.
 *
 * @since 1.14
 */
public class MemoryLimitException extends IOException {

    private static final long serialVersionUID = 1L;

    //long instead of int to account for overflow for corrupt files
    private final long memoryNeededInKb;
    private final int memoryLimitInKb;

    public MemoryLimitException(long memoryNeededInKb, int memoryLimitInKb) {
        super(buildMessage(memoryNeededInKb, memoryLimitInKb));
        this.memoryNeededInKb = memoryNeededInKb;
        this.memoryLimitInKb = memoryLimitInKb;
    }

    public MemoryLimitException(long memoryNeededInKb, int memoryLimitInKb, Exception e) {
        super(buildMessage(memoryNeededInKb, memoryLimitInKb), e);
        this.memoryNeededInKb = memoryNeededInKb;
        this.memoryLimitInKb = memoryLimitInKb;
    }

    public long getMemoryNeededInKb() {
        return memoryNeededInKb;
    }

    public int getMemoryLimitInKb() {
        return memoryLimitInKb;
    }

    private static String buildMessage(long memoryNeededInKb, int memoryLimitInKb) {
        return memoryNeededInKb + " kb of memory would be needed; limit was "
                + memoryLimitInKb + " kb. " +
                "If the file is not corrupt, consider increasing the memory limit.";
    }
}
