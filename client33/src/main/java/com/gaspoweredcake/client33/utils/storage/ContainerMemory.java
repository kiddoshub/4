package com.gaspoweredcake.client33.utils.storage;

import java.util.LinkedHashMap;

/** Session-local bounded observation cache with object identity protection against unloaded/replaced containers. */
public final class ContainerMemory<K> {
    private record Observation(Object identity, ContainerSnapshot snapshot) {}
    private final LinkedHashMap<K, Observation> observations = new LinkedHashMap<>();
    private final int capacity;
    public ContainerMemory(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }
    public void put(K key, Object identity, ContainerSnapshot snapshot) {
        observations.remove(key);
        observations.put(key, new Observation(identity, snapshot));
        while (observations.size() > capacity) observations.remove(observations.keySet().iterator().next());
    }
    public ContainerSnapshot get(K key, Object identity, long now, long ttlMillis) {
        Observation value = observations.get(key);
        if (value == null) return null;
        if (value.identity != identity || value.snapshot.expired(now, ttlMillis)) { observations.remove(key); return null; }
        return value.snapshot;
    }
    public void expire(long now, long ttlMillis) { observations.values().removeIf(v -> v.snapshot.expired(now, ttlMillis)); }
    public void clear() { observations.clear(); }
    public int size() { return observations.size(); }
}
