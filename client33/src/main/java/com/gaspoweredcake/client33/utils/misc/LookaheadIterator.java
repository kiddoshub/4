package com.gaspoweredcake.client33.utils.misc;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Lazy iterator for non-null values. Exhaustion is stable and next() follows the Iterator contract. */
public abstract class LookaheadIterator<T> implements Iterator<T> {
    private T next;
    private boolean prepared, exhausted;
    protected abstract T findNext();

    @Override public final boolean hasNext() {
        if (exhausted) return false;
        if (!prepared) {
            next = findNext();
            prepared = true;
            exhausted = next == null;
        }
        return !exhausted;
    }
    @Override public final T next() {
        if (!hasNext()) throw new NoSuchElementException();
        T result = next;
        next = null;
        prepared = false;
        return result;
    }
}
