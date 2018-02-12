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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Iterates all services for a given class through the standard
 * {@link ServiceLoader} mechanism.
 *
 * @param <E>
 *            The service to load
 * @since 1.13
 */
public class ServiceLoaderIterator<E> implements Iterator<E> {

    private E nextServiceLoader;
    private final Class<E> service;
    private final Iterator<E> serviceLoaderIterator;

    public ServiceLoaderIterator(final Class<E> service) {
        this(service, ClassLoader.getSystemClassLoader());
    }

    public ServiceLoaderIterator(final Class<E> service, final ClassLoader classLoader) {
        this.service = service;
        final ServiceLoader<E> serviceLoader = ServiceLoader.load(service, classLoader);
        serviceLoaderIterator = serviceLoader.iterator();
        nextServiceLoader = null;
    }

    private boolean getNextServiceLoader() {
        while (nextServiceLoader == null) {
            try {
                if (!serviceLoaderIterator.hasNext()) {
                    return false;
                }
                nextServiceLoader = serviceLoaderIterator.next();
            } catch (final ServiceConfigurationError e) {
                if (e.getCause() instanceof SecurityException) {
                    // Ignore security exceptions
                    // TODO Log?
                    continue;
                }
                throw e;
            }
        }
        return true;
    }

    @Override
    public boolean hasNext() {
        return getNextServiceLoader();
    }

    @Override
    public E next() {
        if (!getNextServiceLoader()) {
            throw new NoSuchElementException("No more elements for service " + service.getName());
        }
        final E tempNext = nextServiceLoader;
        nextServiceLoader = null;
        return tempNext;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("service=" + service.getName());
    }

}
