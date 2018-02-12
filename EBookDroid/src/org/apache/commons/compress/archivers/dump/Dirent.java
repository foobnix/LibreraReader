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
package org.apache.commons.compress.archivers.dump;

/**
 * Directory entry.
 */
class Dirent {
    private final int ino;
    private final int parentIno;
    private final int type;
    private final String name;

    /**
     * Constructor
     *
     * @param ino
     * @param parentIno
     * @param type
     * @param name
     */
    Dirent(final int ino, final int parentIno, final int type, final String name) {
        this.ino = ino;
        this.parentIno = parentIno;
        this.type = type;
        this.name = name;
    }

    /**
     * Get ino.
     * @return the i-node
     */
    int getIno() {
        return ino;
    }

    /**
     * Get ino of parent directory.
     * @return the parent i-node
     */
    int getParentIno() {
        return parentIno;
    }

    /**
     * Get entry type.
     * @return the entry type
     */
    int getType() {
        return type;
    }

    /**
     * Get name of directory entry.
     * @return the directory name
     */
    String getName() {
        return name;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("[%d]: %s", ino, name);
    }
}
