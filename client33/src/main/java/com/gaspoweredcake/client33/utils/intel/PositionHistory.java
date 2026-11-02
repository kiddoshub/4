package com.gaspoweredcake.client33.utils.intel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Bounded positions received for known entities. Absence says nothing about hidden players. */
public final class PositionHistory {
    public record Position(int id, UUID uuid, String type, double x, double y, double z, String source, long nanos) {}
    private final int maxEntities, maxSamples;
    private final LinkedHashMap<Integer, ArrayDeque<Position>> tracks = new LinkedHashMap<>();
    public PositionHistory(int maxEntities, int maxSamples) {
        if (maxEntities < 1 || maxSamples < 1) throw new IllegalArgumentException("Invalid limits");
        this.maxEntities = maxEntities; this.maxSamples = maxSamples;
    }
    public synchronized void add(Position value) {
        if (!Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z)) return;
        var track = tracks.remove(value.id);
        if (track == null || !track.isEmpty() && !track.getLast().uuid.equals(value.uuid)) track = new ArrayDeque<>();
        while (track.size() >= maxSamples) track.removeFirst();
        track.addLast(value); tracks.put(value.id, track);
        while (tracks.size() > maxEntities) tracks.remove(tracks.keySet().iterator().next());
    }
    public synchronized void remove(int id, UUID uuid) {
        var track = tracks.get(id);
        if (track != null && !track.isEmpty() && track.getLast().uuid.equals(uuid)) tracks.remove(id);
    }
    public synchronized void removeId(int id) { tracks.remove(id); }
    public synchronized List<Position> get(int id) {
        var track = tracks.get(id); return track == null ? List.of() : List.copyOf(track);
    }
    public synchronized List<Position> nearby(double x, double y, double z, double range) {
        if (!Double.isFinite(range) || range < 0) return List.of();
        double r2 = range * range; var result = new ArrayList<Position>();
        for (var track : tracks.values()) {
            Position p = track.getLast(); double dx=p.x-x, dy=p.y-y, dz=p.z-z;
            if (dx*dx + dy*dy + dz*dz <= r2) result.add(p);
        }
        return List.copyOf(result);
    }
    public synchronized void clear() { tracks.clear(); }
}
