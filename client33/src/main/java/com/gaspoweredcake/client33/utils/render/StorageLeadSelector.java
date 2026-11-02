package com.gaspoweredcake.client33.utils.render;

import java.util.*;

/** Chooses display rows only; the underlying storage index is never truncated. */
public final class StorageLeadSelector {
    public enum Source { CURRENT, REMEMBERED, CACHED, HEARD }
    public enum Order { Largest, Nearest }
    public record Candidate(Source source, StorageClusters.Point point, int containers, long time, String label) {}

    private StorageLeadSelector() {}

    public static List<Candidate> select(Iterable<Candidate> candidates, int limit, Order order, double x, double y, double z) {
        if (limit <= 0) return List.of();
        Comparator<Candidate> distance = Comparator.comparingDouble(c -> c.point().squaredDistance(x, y, z));
        Comparator<Candidate> count = Comparator.comparingInt(Candidate::containers).reversed();
        Comparator<Candidate> rank = (order == Order.Nearest ? distance.thenComparing(count) : count.thenComparing(distance))
            .thenComparingDouble(c -> c.point().x()).thenComparingDouble(c -> c.point().y())
            .thenComparingDouble(c -> c.point().z()).thenComparingLong(Candidate::time)
            .thenComparing(c -> c.label() == null ? "" : c.label());
        Map<Source, PriorityQueue<Candidate>> groups = new EnumMap<>(Source.class);
        for (Candidate candidate : candidates) {
            var heap = groups.computeIfAbsent(candidate.source(), _ -> new PriorityQueue<>(rank.reversed()));
            if (heap.size() < limit) heap.add(candidate);
            else if (rank.compare(candidate, heap.peek()) < 0) { heap.poll(); heap.add(candidate); }
        }
        Map<Source, List<Candidate>> ordered = new EnumMap<>(Source.class);
        groups.forEach((source, heap) -> { var list = new ArrayList<>(heap); list.sort(rank); ordered.put(source, list); });
        var result = new ArrayList<Candidate>(limit);
        for (int round = 0; result.size() < limit; round++) {
            boolean added = false;
            for (Source source : Source.values()) {
                var list = ordered.get(source);
                if (list != null && round < list.size()) {
                    result.add(list.get(round)); added = true;
                    if (result.size() == limit) break;
                }
            }
            if (!added) break;
        }
        return List.copyOf(result);
    }
}
