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
package org.apache.commons.compress.changes;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the results of an performed ChangeSet operation.
 */
public class ChangeSetResults {
    private final List<String> addedFromChangeSet = new ArrayList<>();
    private final List<String> addedFromStream = new ArrayList<>();
    private final List<String> deleted = new ArrayList<>();

    /**
     * Adds the filename of a recently deleted file to the result list.
     * @param fileName the file which has been deleted
     */
    void deleted(final String fileName) {
        deleted.add(fileName);
    }

    /**
     * Adds the name of a file to the result list which has been
     * copied from the source stream to the target stream.
     * @param fileName the file name which has been added from the original stream
     */
    void addedFromStream(final String fileName) {
        addedFromStream.add(fileName);
    }

    /**
     * Adds the name of a file to the result list which has been
     * copied from the changeset to the target stream
     * @param fileName the name of the file
     */
    void addedFromChangeSet(final String fileName) {
        addedFromChangeSet.add(fileName);
    }

    /**
     * Returns a list of filenames which has been added from the changeset
     * @return the list of filenames
     */
    public List<String> getAddedFromChangeSet() {
        return addedFromChangeSet;
    }

    /**
     * Returns a list of filenames which has been added from the original stream
     * @return the list of filenames
     */
    public List<String> getAddedFromStream() {
        return addedFromStream;
    }

    /**
     * Returns a list of filenames which has been deleted
     * @return the list of filenames
     */
    public List<String> getDeleted() {
        return deleted;
    }

    /**
     * Checks if an filename already has been added to the result list
     * @param filename the filename to check
     * @return true, if this filename already has been added
     */
    boolean hasBeenAdded(final String filename) {
        return addedFromChangeSet.contains(filename) || addedFromStream.contains(filename);
    }
}
