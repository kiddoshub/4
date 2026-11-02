package com.gaspoweredcake.client33.utils.storage;

import java.util.*;

/** Immutable, bounded summary of server-observed slots; never infers unopened inventory. */
public record ContainerSnapshot(long observedAt, int slots, int occupied, long items, int enchanted,
                                int lowestDurability, boolean serverObserved, List<ItemTotal> contents) {
    public record Stack(String id, String name, int count, int maxCount, boolean enchanted, int durability) {}
    public record ItemTotal(String id, String name, long count) {}

    public ContainerSnapshot { contents = List.copyOf(contents); }

    public static ContainerSnapshot summarize(long now, List<Stack> stacks) {
        return summarize(now, stacks, true);
    }
    public static ContainerSnapshot summarize(long now, List<Stack> stacks, boolean serverObserved) {
        if (stacks.size() > 108) throw new IllegalArgumentException("Container exceeds observation budget");
        Map<String, ItemTotal> totals = new HashMap<>();
        int occupied = 0, enchanted = 0, durability = -1;
        long items = 0;
        for (Stack stack : stacks) {
            if (stack == null || stack.count <= 0) continue;
            int count = stack.count; // Preserve server/plugin overstack counts; totals use long.
            occupied++; items += count;
            if (stack.enchanted) enchanted++;
            if (stack.durability >= 0) durability = durability < 0 ? Math.min(100, stack.durability) : Math.min(durability, stack.durability);
            String id = clean(stack.id, 128), name = clean(stack.name, 48);
            // Keep separately named variants separate; item-component payloads are deliberately not retained.
            String key = id + "\u0000" + name;
            ItemTotal old = totals.get(key);
            totals.put(key, new ItemTotal(id, name, count + (old == null ? 0 : old.count)));
        }
        List<ItemTotal> ordered = totals.values().stream()
            .sorted(Comparator.comparingLong(ItemTotal::count).reversed().thenComparing(ItemTotal::id).thenComparing(ItemTotal::name)).toList();
        return new ContainerSnapshot(now, stacks.size(), occupied, items, enchanted, durability, serverObserved, ordered);
    }

    public boolean expired(long now, long ttlMillis) { return now < observedAt || now - observedAt >= ttlMillis; }
    public double occupancy() { return slots == 0 ? 0 : (double) occupied / slots; }
    public static String clean(String value, int limit) {
        if (value == null) return "";
        String clean = value.replaceAll("§.", "").replaceAll("[\\p{Cntrl}]", " ");
        return clean.length() <= limit ? clean : clean.substring(0, limit - 1) + "…";
    }
}
