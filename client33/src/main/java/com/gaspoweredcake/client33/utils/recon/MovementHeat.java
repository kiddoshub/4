package com.gaspoweredcake.client33.utils.recon;

import java.util.*;

/** Session-only aggregated movement. Stationary players and teleports do not accumulate heat. */
public final class MovementHeat {
    public record Cell(int x, int z) {}
    public record Heat(Cell cell, double y, double value, int visits, long seen) {}
    private record Sample(double x, double y, double z, long seen) {}
    private final LinkedHashMap<Cell, Heat> cells = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Sample> last = new LinkedHashMap<>();
    private final int capacity;
    public MovementHeat(int capacity) { this.capacity = Math.max(1, capacity); }
    public void sample(UUID player, double x, double y, double z, long now, int size, double halfLifeMillis) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || size < 1) return;
        Sample previous = last.put(player, new Sample(x, y, z, now));
        while (last.size() > 256) last.remove(last.keySet().iterator().next());
        if (previous == null || now - previous.seen > 5000) return;
        double distance = Math.pow(x - previous.x, 2) + Math.pow(y - previous.y, 2) + Math.pow(z - previous.z, 2);
        if (distance < 0.25 || distance > 4096) return;
        Cell cell = new Cell((int) Math.floor(x / size), (int) Math.floor(z / size));
        Heat old = cells.remove(cell);
        double energy = old == null ? 0 : decayed(old, now, halfLifeMillis);
        cells.put(cell, new Heat(cell, y, Math.min(1000, energy + 1), old == null ? 1 : Math.min(1_000_000, old.visits + 1), now));
        while (cells.size() > capacity) cells.remove(cells.keySet().iterator().next());
    }
    public static double decayed(Heat heat, long now, double halfLifeMillis) {
        return heat.value * Math.pow(0.5, Math.max(0, now - heat.seen) / Math.max(1, halfLifeMillis));
    }
    public void expire(long now, long ttl) {
        cells.values().removeIf(heat -> now - heat.seen > ttl);
        last.values().removeIf(sample -> now - sample.seen > 5000);
    }
    public Collection<Heat> cells() { return Collections.unmodifiableCollection(cells.values()); }
    public void clear() { cells.clear(); last.clear(); }
}
