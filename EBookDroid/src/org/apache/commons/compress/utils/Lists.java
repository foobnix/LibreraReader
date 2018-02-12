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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * List utilities
 *
 * @since 1.13
 */
public class Lists {

    /**
     * Creates a new {@link ArrayList}.
     *
     * @param <E> type of elements contained in new list
     * @return a new {@link ArrayList}
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    /**
     * Creates a new {@link ArrayList} filled with the contents of the given
     * {@code iterator}.
     *
     * @param iterator
     *            the source iterator
     * @param <E> type of elements contained in new list
     * @return a new {@link ArrayList}
     */
    public static <E> ArrayList<E> newArrayList(final Iterator<? extends E> iterator) {
        final ArrayList<E> list = newArrayList();
        Iterators.addAll(list, iterator);
        return list;
    }

    private Lists() {
        // do not instantiate
    }

}
