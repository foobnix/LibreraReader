package com.foobnix.work;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/** Schedules only as much work as can run, publishing results in completion order. */
final class BoundedTasks {
    private BoundedTasks() {}

    static <T, R> boolean run(List<T> items, int parallelism, BooleanSupplier stopped,
                              Function<T, R> task, Consumer<R> completed)
            throws InterruptedException, ExecutionException {
        int threads = Math.max(1, parallelism);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        ExecutorCompletionService<R> results = new ExecutorCompletionService<>(pool);
        Iterator<T> remaining = items.iterator();
        int active = 0;
        try {
            if (stopped.getAsBoolean()) return false;
            while (remaining.hasNext() || active > 0) {
                if (stopped.getAsBoolean()) return false;
                while (active < threads && remaining.hasNext()) {
                    if (stopped.getAsBoolean()) return false;
                    T item = remaining.next();
                    results.submit(() -> task.apply(item));
                    active++;
                }
                Future<R> ready = results.poll(250, TimeUnit.MILLISECONDS);
                if (ready == null) continue;
                active--;
                R result = ready.get();
                if (stopped.getAsBoolean()) return false;
                completed.accept(result);
            }
            return true;
        } finally {
            pool.shutdownNow();
        }
    }
}
