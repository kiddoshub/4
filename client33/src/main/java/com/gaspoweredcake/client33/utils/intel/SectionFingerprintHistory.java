package com.gaspoweredcake.client33.utils.intel;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;

/** Bounded history of observed section changes, retained across chunk unloads for this world session. */
public final class SectionFingerprintHistory {
    public record Key(int chunkX, int sectionY, int chunkZ) {}
    public record Change(Key key, long previousHash, long currentHash, long observedNanos) {}
    private final int maxSections, maxChanges;
    private final LinkedHashMap<Key, Long> hashes = new LinkedHashMap<>();
    private final ArrayDeque<Change> changes = new ArrayDeque<>();
    public SectionFingerprintHistory(int maxSections, int maxChanges) {
        if (maxSections < 1 || maxChanges < 1) throw new IllegalArgumentException("Invalid bounds");
        this.maxSections = maxSections; this.maxChanges = maxChanges;
    }
    public void observe(Key key, long hash, long time) {
        Long previous = hashes.remove(key); hashes.put(key, hash);
        if (previous != null && previous.longValue() != hash) {
            if (changes.size() >= maxChanges) changes.removeFirst();
            changes.addLast(new Change(key, previous, hash, time));
        }
        while (hashes.size() > maxSections) hashes.remove(hashes.keySet().iterator().next());
    }
    public List<Change> changes() { return List.copyOf(changes); }
    public void clear() { hashes.clear(); changes.clear(); }
}
