package com.gaspoweredcake.client33.utils.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/** Streaming top-k selection with an optional retained primary. */
public final class TargetSelection<T> {
    private final int limit;
    private final T retained;
    private final Comparator<T> order;
    private final PriorityQueue<T> best;
    private final Set<T> seen = new HashSet<>();
    private boolean retainFound;

    public TargetSelection(int limit, T retained, Comparator<T> order) {
        if (limit < 1) throw new IllegalArgumentException("Target limit must be positive");
        this.limit = limit;
        this.retained = retained;
        this.order = order;
        best = new PriorityQueue<>(Math.min(limit, 64), order.reversed());
    }

    public void add(T candidate) {
        if (seen.contains(candidate)) return;
        if (candidate.equals(retained)) { retainFound = true; seen.add(candidate); return; }
        if (best.size() < limit) { best.add(candidate); seen.add(candidate); }
        else if (order.compare(candidate, best.peek()) < 0) {
            seen.remove(best.poll());
            best.add(candidate);
            seen.add(candidate);
        }
    }

    public List<T> result() {
        List<T> result = new ArrayList<>(best);
        result.sort(order);
        if (retainFound) result.addFirst(retained);
        if (result.size() > limit) result.subList(limit, result.size()).clear();
        return result;
    }
}
