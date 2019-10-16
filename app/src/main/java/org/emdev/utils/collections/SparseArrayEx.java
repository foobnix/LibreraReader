package org.emdev.utils.collections;

import android.util.SparseArray;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SparseArrayEx<T> extends SparseArray<T> implements Iterable<T> {

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new SparseArrayIterator();
    }

    class SparseArrayIterator implements Iterator<T> {

        /** Number of elements remaining in this iteration */
        private int remaining = size();

        /** Index of element that remove() would remove, or -1 if no such elt */
        private int removalIndex = -1;

        public boolean hasNext() {
            return remaining > 0;
        }

        public T next() {
            if (remaining <= 0) {
                throw new NoSuchElementException();
            }
            removalIndex = size() - remaining;
            remaining--;
            return valueAt(removalIndex);
        }

        public void remove() {
            if (removalIndex < 0) {
                throw new IllegalStateException();
            }
            SparseArrayEx.this.remove(keyAt(removalIndex));
            removalIndex = -1;
        }
    }
}
