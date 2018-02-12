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

package org.apache.commons.compress.utils;

import java.util.Collections;
import java.util.HashSet;

/**
 * Set utilities
 *
 * @since 1.13
 */
public class Sets {

    private Sets() {
        // Do not instantiate
    }

    /**
     * Creates a new HashSet filled with the given elements
     *
     * @param elements
     *            the elements to fill the new set
     * @param <E> type of elements contained in new set
     * @return A new HasSet
     */
    public static <E> HashSet<E> newHashSet(@SuppressWarnings("unchecked") E... elements) {
        final HashSet<E> set = new HashSet<>(elements.length);
        Collections.addAll(set, elements);
        return set;
    }
}
