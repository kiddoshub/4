package com.gaspoweredcake.client33.utils.render;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Compact cached coordinates. No live entities, chunk references or inferred inventories. */
public final class CachedStorageIndex {
    public record Entry(int x, int y, int z, String type) {}
    private record Chunk(int x, int z) {}
    private record Position(int x, int y, int z) {}
    private record Region(int x, int y, int z) {}
    /** Immutable render pages; every matching record remains available to the compass. */
    public record Page(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, List<Entry> entries) {
        public Page { entries = List.copyOf(entries); }
    }
    private final Map<Chunk, List<Entry>> chunks = new HashMap<>();

    public void replace(int x, int z, Collection<Entry> entries) {
        var unique = new LinkedHashMap<Position, Entry>();
        for (Entry entry : entries) {
            if (entry.type == null || entry.type.isBlank() || entry.x >> 4 != x || entry.z >> 4 != z) continue;
            unique.put(new Position(entry.x, entry.y, entry.z), entry);
        }
        Chunk key = new Chunk(x, z);
        if (unique.isEmpty()) chunks.remove(key);
        else chunks.put(key, List.copyOf(unique.values()));
    }

    public List<Entry> matching(Predicate<String> selected, BiPredicate<Integer, Integer> live) {
        var result = new ArrayList<Entry>();
        for (var chunk : chunks.entrySet()) {
            if (live.test(chunk.getKey().x, chunk.getKey().z)) continue;
            for (Entry entry : chunk.getValue()) if (selected.test(entry.type)) result.add(entry);
        }
        return List.copyOf(result);
    }

    /** 128-block cells bound render work spatially, without a distance or record quota. */
    public static List<Page> pages(Collection<Entry> entries) {
        var regions = new LinkedHashMap<Region, List<Entry>>();
        for (Entry entry : entries) {
            var region = new Region(Math.floorDiv(entry.x, 128), Math.floorDiv(entry.y, 128), Math.floorDiv(entry.z, 128));
            regions.computeIfAbsent(region, _ -> new ArrayList<>()).add(entry);
        }
        var result = new ArrayList<Page>(regions.size());
        for (var records : regions.values()) {
            int minX = Integer.MAX_VALUE, minY = minX, minZ = minX;
            int maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
            for (Entry entry : records) {
                minX = Math.min(minX, entry.x); minY = Math.min(minY, entry.y); minZ = Math.min(minZ, entry.z);
                maxX = Math.max(maxX, entry.x + 1); maxY = Math.max(maxY, entry.y + 1); maxZ = Math.max(maxZ, entry.z + 1);
            }
            result.add(new Page(minX, minY, minZ, maxX, maxY, maxZ, records));
        }
        return List.copyOf(result);
    }

    public void remove(int x, int z) { chunks.remove(new Chunk(x, z)); }
    public void clear() { chunks.clear(); }
}
