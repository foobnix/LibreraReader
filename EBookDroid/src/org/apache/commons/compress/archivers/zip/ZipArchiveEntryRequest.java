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
package org.apache.commons.compress.archivers.zip;

import org.apache.commons.compress.parallel.InputStreamSupplier;

import java.io.InputStream;

/**
 * A Thread-safe representation of a ZipArchiveEntry that is used to add entries to parallel archives.
 *
 * @since 1.10
 */
public class ZipArchiveEntryRequest {
    /*
     The zipArchiveEntry is not thread safe, and cannot be safely accessed by the getters of this class.
     It is safely accessible during the construction part of this class and also after the
     thread pools have been shut down.
     */
    private final ZipArchiveEntry zipArchiveEntry;
    private final InputStreamSupplier payloadSupplier;
    private final int method;


    private ZipArchiveEntryRequest(final ZipArchiveEntry zipArchiveEntry, final InputStreamSupplier payloadSupplier) {
        // this constructor has "safe" access to all member variables on zipArchiveEntry
        this.zipArchiveEntry = zipArchiveEntry;
        this.payloadSupplier = payloadSupplier;
        this.method = zipArchiveEntry.getMethod();
    }

    /**
     * Create a ZipArchiveEntryRequest
     * @param zipArchiveEntry The entry to use
     * @param payloadSupplier The payload that will be added to the zip entry.
     * @return The newly created request
     */
    public static ZipArchiveEntryRequest createZipArchiveEntryRequest(final ZipArchiveEntry zipArchiveEntry, final InputStreamSupplier payloadSupplier) {
        return new ZipArchiveEntryRequest(zipArchiveEntry, payloadSupplier);
    }

    /**
     * The paylaod that will be added to this zip entry
     * @return The input stream.
     */
    public InputStream getPayloadStream() {
        return payloadSupplier.get();
    }

    /**
     * The compression method to use
     * @return The compression method to use
     */
    public int getMethod(){
       return method;
    }


    /**
     * Gets the underlying entry. Do not use this methods from threads that did not create the instance itself !
     * @return the zipeArchiveEntry that is basis for this request
     */
    ZipArchiveEntry getZipArchiveEntry() {
        return zipArchiveEntry;
    }
}
