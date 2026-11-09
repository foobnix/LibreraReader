package com.foobnix.sys;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Set when the reader is left while a book loads, and on memory trims - every time the app goes
 * to the background: the converters then give up on the book half made. A copy of a book opened
 * for something else, such as reading it aloud in the background, is opened through
 * {@link #ignoredWhile} and does not see it - on its own thread, and on the threads that opening
 * starts, such as the one reading the footnotes.
 */
public class LoadingCancel {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final InheritableThreadLocal<Boolean> ignored = new InheritableThreadLocal<>();

    private boolean isIgnored() {
        return Boolean.TRUE.equals(ignored.get());
    }

    public boolean get() {
        return !isIgnored() && cancelled.get();
    }

    public void set(boolean value) {
        cancelled.set(value);
    }

    public boolean getAndSet(boolean value) {
        return cancelled.getAndSet(value) && !isIgnored();
    }

    public <T> T ignoredWhile(Callable<T> open) throws Exception {
        final Boolean before = ignored.get();
        ignored.set(true);
        try {
            return open.call();
        } finally {
            ignored.set(before);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(get());
    }
}
