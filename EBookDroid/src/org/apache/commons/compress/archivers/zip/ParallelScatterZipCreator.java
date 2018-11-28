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

import org.apache.commons.compress.parallel.FileBasedScatterGatherBackingStore;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.parallel.ScatterGatherBackingStore;
import org.apache.commons.compress.parallel.ScatterGatherBackingStoreSupplier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;

import static java.util.Collections.synchronizedList;
import static org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest.createZipArchiveEntryRequest;

/**
 * Creates a zip in parallel by using multiple threadlocal {@link ScatterZipOutputStream} instances.
 * <p>
 * Note that this class generally makes no guarantees about the order of things written to
 * the output file. Things that need to come in a specific order (manifests, directories)
 * must be handled by the client of this class, usually by writing these things to the
 * {@link ZipArchiveOutputStream} <em>before</em> calling {@link #writeTo writeTo} on this class.</p>
 * <p>
 * The client can supply an {@link java.util.concurrent.ExecutorService}, but for reasons of
 * memory model consistency, this will be shut down by this class prior to completion.
 * </p>
 * @since 1.10
 */
public class ParallelScatterZipCreator {
    private final List<ScatterZipOutputStream> streams = synchronizedList(new ArrayList<ScatterZipOutputStream>());
    private final ExecutorService es;
    private final ScatterGatherBackingStoreSupplier backingStoreSupplier;
    private final List<Future<Object>> futures = new ArrayList<>();

    private final long startedAt = System.currentTimeMillis();
    private long compressionDoneAt = 0;
    private long scatterDoneAt;

    private static class DefaultBackingStoreSupplier implements ScatterGatherBackingStoreSupplier {
        final AtomicInteger storeNum = new AtomicInteger(0);

        @Override
        public ScatterGatherBackingStore get() throws IOException {
            final File tempFile = File.createTempFile("parallelscatter", "n" + storeNum.incrementAndGet());
            return new FileBasedScatterGatherBackingStore(tempFile);
        }
    }

    private ScatterZipOutputStream createDeferred(final ScatterGatherBackingStoreSupplier scatterGatherBackingStoreSupplier)
            throws IOException {
        final ScatterGatherBackingStore bs = scatterGatherBackingStoreSupplier.get();
        // lifecycle is bound to the ScatterZipOutputStream returned
        final StreamCompressor sc = StreamCompressor.create(Deflater.DEFAULT_COMPRESSION, bs); //NOSONAR
        return new ScatterZipOutputStream(bs, sc);
    }

    private final ThreadLocal<ScatterZipOutputStream> tlScatterStreams = new ThreadLocal<ScatterZipOutputStream>() {
        @Override
        protected ScatterZipOutputStream initialValue() {
            try {
                final ScatterZipOutputStream scatterStream = createDeferred(backingStoreSupplier);
                streams.add(scatterStream);
                return scatterStream;
            } catch (final IOException e) {
                throw new RuntimeException(e); //NOSONAR
            }
        }
    };

    /**
     * Create a ParallelScatterZipCreator with default threads, which is set to the number of available
     * processors, as defined by {@link java.lang.Runtime#availableProcessors}
     */
    public ParallelScatterZipCreator() {
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    /**
     * Create a ParallelScatterZipCreator
     *
     * @param executorService The executorService to use for parallel scheduling. For technical reasons,
     *                        this will be shut down by this class.
     */
    public ParallelScatterZipCreator(final ExecutorService executorService) {
        this(executorService, new DefaultBackingStoreSupplier());
    }

    /**
     * Create a ParallelScatterZipCreator
     *
     * @param executorService The executorService to use. For technical reasons, this will be shut down
     *                        by this class.
     * @param backingStoreSupplier The supplier of backing store which shall be used
     */
    public ParallelScatterZipCreator(final ExecutorService executorService,
                                     final ScatterGatherBackingStoreSupplier backingStoreSupplier) {
        this.backingStoreSupplier = backingStoreSupplier;
        es = executorService;
    }

    /**
     * Adds an archive entry to this archive.
     * <p>
     * This method is expected to be called from a single client thread
     * </p>
     *
     * @param zipArchiveEntry The entry to add.
     * @param source          The source input stream supplier
     */

    public void addArchiveEntry(final ZipArchiveEntry zipArchiveEntry, final InputStreamSupplier source) {
        submit(createCallable(zipArchiveEntry, source));
    }

    /**
     * Adds an archive entry to this archive.
     * <p>
     * This method is expected to be called from a single client thread
     * </p>
     *
     * @param zipArchiveEntryRequestSupplier Should supply the entry to be added.
     * @since 1.13
     */
    public void addArchiveEntry(final ZipArchiveEntryRequestSupplier zipArchiveEntryRequestSupplier) {
        submit(createCallable(zipArchiveEntryRequestSupplier));
    }

    /**
     * Submit a callable for compression.
     *
     * @see ParallelScatterZipCreator#createCallable for details of if/when to use this.
     *
     * @param callable The callable to run, created by {@link #createCallable createCallable}, possibly wrapped by caller.
     */
    public final void submit(final Callable<Object> callable) {
        futures.add(es.submit(callable));
    }

    /**
     * Create a callable that will compress the given archive entry.
     *
     * <p>This method is expected to be called from a single client thread.</p>
     *
     * Consider using {@link #addArchiveEntry addArchiveEntry}, which wraps this method and {@link #submit submit}.
     * The most common use case for using {@link #createCallable createCallable} and {@link #submit submit} from a
     * client is if you want to wrap the callable in something that can be prioritized by the supplied
     * {@link ExecutorService}, for instance to process large or slow files first.
     * Since the creation of the {@link ExecutorService} is handled by the client, all of this is up to the client.
     *
     * @param zipArchiveEntry The entry to add.
     * @param source          The source input stream supplier
     * @return A callable that should subsequently passed to #submit, possibly in a wrapped/adapted from. The
     * value of this callable is not used, but any exceptions happening inside the compression
     * will be propagated through the callable.
     */

    public final Callable<Object> createCallable(final ZipArchiveEntry zipArchiveEntry, final InputStreamSupplier source) {
        final int method = zipArchiveEntry.getMethod();
        if (method == ZipMethod.UNKNOWN_CODE) {
            throw new IllegalArgumentException("Method must be set on zipArchiveEntry: " + zipArchiveEntry);
        }
        final ZipArchiveEntryRequest zipArchiveEntryRequest = createZipArchiveEntryRequest(zipArchiveEntry, source);
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                tlScatterStreams.get().addArchiveEntry(zipArchiveEntryRequest);
                return null;
            }
        };
    }

    /**
     * Create a callable that will compress archive entry supplied by {@link ZipArchiveEntryRequestSupplier}.
     *
     * <p>This method is expected to be called from a single client thread.</p>
     *
     * The same as {@link #createCallable(ZipArchiveEntry, InputStreamSupplier)}, but the archive entry
     * to be added is supplied by a {@link ZipArchiveEntryRequestSupplier}.
     *
     * @see #createCallable(ZipArchiveEntry, InputStreamSupplier)
     *
     * @param zipArchiveEntryRequestSupplier Should supply the entry to be added.
     * @return A callable that should subsequently passed to #submit, possibly in a wrapped/adapted from. The
     * value of this callable is not used, but any exceptions happening inside the compression
     * will be propagated through the callable.
     * @since 1.13
     */
    public final Callable<Object> createCallable(final ZipArchiveEntryRequestSupplier zipArchiveEntryRequestSupplier) {
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                tlScatterStreams.get().addArchiveEntry(zipArchiveEntryRequestSupplier.get());
                return null;
            }
        };
    }

    /**
     * Write the contents this to the target {@link ZipArchiveOutputStream}.
     * <p>
     * It may be beneficial to write things like directories and manifest files to the targetStream
     * before calling this method.
     * </p>
     *
     * <p>Calling this method will shut down the {@link ExecutorService} used by this class. If any of the {@link
     * Callable}s {@link #submit}ted to this instance throws an exception, the archive can not be created properly and
     * this method will throw an exception.</p>
     *
     * @param targetStream The {@link ZipArchiveOutputStream} to receive the contents of the scatter streams
     * @throws IOException          If writing fails
     * @throws InterruptedException If we get interrupted
     * @throws ExecutionException   If something happens in the parallel execution
     */
    public void writeTo(final ZipArchiveOutputStream targetStream)
            throws IOException, InterruptedException, ExecutionException {

        // Make sure we catch any exceptions from parallel phase
        try {
            for (final Future<?> future : futures) {
                future.get();
            }
        } finally {
            es.shutdown();
        }

        es.awaitTermination(1000 * 60L, TimeUnit.SECONDS);  // == Infinity. We really *must* wait for this to complete

        // It is important that all threads terminate before we go on, ensure happens-before relationship
        compressionDoneAt = System.currentTimeMillis();

        synchronized (streams) {
            for (final ScatterZipOutputStream scatterStream : streams) {
                scatterStream.writeTo(targetStream);
                scatterStream.close();
            }
        }

        scatterDoneAt = System.currentTimeMillis();
    }

    /**
     * Returns a message describing the overall statistics of the compression run
     *
     * @return A string
     */
    public ScatterStatistics getStatisticsMessage() {
        return new ScatterStatistics(compressionDoneAt - startedAt, scatterDoneAt - compressionDoneAt);
    }
}

