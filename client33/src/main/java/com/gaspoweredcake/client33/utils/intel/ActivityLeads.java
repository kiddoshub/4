package com.gaspoweredcake.client33.utils.intel;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/** Evidence from received entities; a lead is not proof of a stash. */
public final class ActivityLeads<K> {
    public enum Kind { ITEM_FRAME, BOAT, ENDER_PEARL, NAMED_MOB, MOB_CLUSTER }
    private final Map<K, EnumSet<Kind>> reasons = new HashMap<>();
    private final Map<K, Integer> livingCounts = new HashMap<>();

    public void add(K chunk, Kind kind) {
        reasons.computeIfAbsent(chunk, ignored -> EnumSet.noneOf(Kind.class)).add(kind);
    }

    public void addLiving(K chunk) { livingCounts.merge(chunk, 1, Integer::sum); }

    public Map<K, EnumSet<Kind>> finish(int minimum) {
        Map<K, EnumSet<Kind>> result = new HashMap<>();
        reasons.forEach((chunk, kinds) -> result.put(chunk, kinds.clone()));
        if (minimum > 0) livingCounts.forEach((chunk, count) -> {
            if (count >= minimum) result.computeIfAbsent(chunk, ignored -> EnumSet.noneOf(Kind.class)).add(Kind.MOB_CLUSTER);
        });
        return result;
    }
}
