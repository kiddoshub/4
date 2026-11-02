package com.gaspoweredcake.client33.utils.player;

import java.util.ArrayList;
import java.util.List;

/** Stable priority queue; removed requests remain owned by the caller until completed. */
public final class RotationQueue<T> {
    private record Entry<T>(T value, int priority) {}
    private final List<Entry<T>> entries = new ArrayList<>();

    public void add(T value, int priority) {
        int index = 0;
        while (index < entries.size() && priority <= entries.get(index).priority()) index++;
        entries.add(index, new Entry<>(value, priority));
    }

    public boolean isEmpty() { return entries.isEmpty(); }
    public T poll() { return entries.removeFirst().value(); }
}
