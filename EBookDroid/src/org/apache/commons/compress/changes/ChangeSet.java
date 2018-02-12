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

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * ChangeSet collects and performs changes to an archive.
 * Putting delete changes in this ChangeSet from multiple threads can
 * cause conflicts.
 *
 * @NotThreadSafe
 */
public final class ChangeSet {

    private final Set<Change> changes = new LinkedHashSet<>();

    /**
     * Deletes the file with the filename from the archive.
     *
     * @param filename
     *            the filename of the file to delete
     */
    public void delete(final String filename) {
        addDeletion(new Change(filename, Change.TYPE_DELETE));
    }

    /**
     * Deletes the directory tree from the archive.
     *
     * @param dirName
     *            the name of the directory tree to delete
     */
    public void deleteDir(final String dirName) {
        addDeletion(new Change(dirName, Change.TYPE_DELETE_DIR));
    }

    /**
     * Adds a new archive entry to the archive.
     *
     * @param pEntry
     *            the entry to add
     * @param pInput
     *            the datastream to add
     */
    public void add(final ArchiveEntry pEntry, final InputStream pInput) {
        this.add(pEntry, pInput, true);
    }

    /**
     * Adds a new archive entry to the archive.
     * If replace is set to true, this change will replace all other additions
     * done in this ChangeSet and all existing entries in the original stream.
     *
     * @param pEntry
     *            the entry to add
     * @param pInput
     *            the datastream to add
     * @param replace
     *            indicates the this change should replace existing entries
     */
    public void add(final ArchiveEntry pEntry, final InputStream pInput, final boolean replace) {
        addAddition(new Change(pEntry, pInput, replace));
    }

    /**
     * Adds an addition change.
     *
     * @param pChange
     *            the change which should result in an addition
     */
    private void addAddition(final Change pChange) {
        if (Change.TYPE_ADD != pChange.type() ||
            pChange.getInput() == null) {
            return;
        }

        if (!changes.isEmpty()) {
            for (final Iterator<Change> it = changes.iterator(); it.hasNext();) {
                final Change change = it.next();
                if (change.type() == Change.TYPE_ADD
                        && change.getEntry() != null) {
                    final ArchiveEntry entry = change.getEntry();

                    if(entry.equals(pChange.getEntry())) {
                        if(pChange.isReplaceMode()) {
                            it.remove();
                            changes.add(pChange);
                            return;
                        }
                        // do not add this change
                        return;
                    }
                }
            }
        }
        changes.add(pChange);
    }

    /**
     * Adds an delete change.
     *
     * @param pChange
     *            the change which should result in a deletion
     */
    private void addDeletion(final Change pChange) {
        if ((Change.TYPE_DELETE != pChange.type() &&
            Change.TYPE_DELETE_DIR != pChange.type()) ||
            pChange.targetFile() == null) {
            return;
        }
        final String source = pChange.targetFile();

        if (source != null && !changes.isEmpty()) {
            for (final Iterator<Change> it = changes.iterator(); it.hasNext();) {
                final Change change = it.next();
                if (change.type() == Change.TYPE_ADD
                        && change.getEntry() != null) {
                    final String target = change.getEntry().getName();

                    if (target == null) {
                        continue;
                    }

                    if (Change.TYPE_DELETE == pChange.type() && source.equals(target) ||
                            (Change.TYPE_DELETE_DIR == pChange.type() && target.matches(source + "/.*"))) {
                        it.remove();
                    }
                }
            }
        }
        changes.add(pChange);
    }

    /**
     * Returns the list of changes as a copy. Changes on this set
     * are not reflected on this ChangeSet and vice versa.
     * @return the changes as a copy
     */
    Set<Change> getChanges() {
        return new LinkedHashSet<>(changes);
    }
}
