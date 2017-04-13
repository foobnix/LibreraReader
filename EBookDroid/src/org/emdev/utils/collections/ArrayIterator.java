package org.emdev.utils.collections;

import java.util.Iterator;

public class ArrayIterator<E> implements Iterator<E>, Iterable<E> {

    private final E[] values;
    private int index = -1;

    public ArrayIterator(final E[] values) {
        this.values = values;
    }

    @Override
    public Iterator<E> iterator() {
        index = -1;
        return this;
    }

    @Override
    public boolean hasNext() {
        index++;
        while (index < values.length) {
            if (values[index] != null) {
                break;
            }
            index++;
        }
        return index < values.length;
    }

    @Override
    public E next() {
        return 0 <= index && index < values.length ? values[index] : null;
    }

    @Override
    public void remove() {
        if (0 <= index && index < values.length) {
            values[index] = null;
        }
    }
}
