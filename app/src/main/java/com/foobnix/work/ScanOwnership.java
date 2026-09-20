package com.foobnix.work;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

/** Serializes ownership changes with writes; UI requests are dispatched off the main thread. */
final class ScanOwnership {
    static final String GENERATION = "scan_generation";
    private static final ReentrantLock WRITES = new ReentrantLock();
    private static final ExecutorService REQUESTS = Executors.newSingleThreadExecutor();
    private static volatile long generation = new java.security.SecureRandom().nextLong() & Long.MAX_VALUE;
    private static boolean adopted;

    private ScanOwnership() {}

    static void request(Runnable operation) { REQUESTS.execute(operation); }

    static long claim() {
        WRITES.lock();
        try {
            adopted = true;
            return ++generation;
        } finally { WRITES.unlock(); }
    }

    /** A persisted request can adopt ownership only before this process accepts a newer request. */
    static long adopt(long requested) {
        WRITES.lock();
        try {
            if (requested <= 0) return claim();
            if (!adopted) {
                generation = requested;
                adopted = true;
            }
            return requested;
        } finally { WRITES.unlock(); }
    }

    static boolean isCurrent(long owner, BooleanSupplier stopped) {
        return owner == generation && !stopped.getAsBoolean();
    }

    static boolean write(long owner, BooleanSupplier stopped, Runnable operation) {
        WRITES.lock();
        try {
            if (!isCurrent(owner, stopped)) return false;
            operation.run();
            return true;
        } finally { WRITES.unlock(); }
    }

    /** Progress is disposable: never wait behind a database transaction on the main looper. */
    static void tryProgress(long owner, BooleanSupplier stopped, Runnable operation) {
        if (!WRITES.tryLock()) return;
        try {
            if (isCurrent(owner, stopped)) operation.run();
        } finally { WRITES.unlock(); }
    }
}
