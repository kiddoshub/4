package com.gaspoweredcake.client33.utils.intel;

import java.util.ArrayDeque;
import java.util.List;

/** Bounded rolling packet observations and inter-arrival statistics, grouped by connection, phase and ID. */
public final class PacketForensics {
    public record Packet(long connection, String phase, int id, int length, long receivedNanos, byte[] prefix) {
        public Packet { prefix = prefix.clone(); }
        @Override public byte[] prefix() { return prefix.clone(); }
    }
    public record Timing(int intervals, double meanNanos, double varianceNanosSquared) {}
    private final int limit;
    private final ArrayDeque<Packet> packets = new ArrayDeque<>();
    public PacketForensics(int limit) { if (limit < 1) throw new IllegalArgumentException("Invalid limit"); this.limit = limit; }
    public synchronized void add(Packet packet) { if (packets.size() == limit) packets.removeFirst(); packets.addLast(packet); }
    public synchronized void clear() { packets.clear(); }
    public synchronized List<Packet> packets() { return List.copyOf(packets); }
    public synchronized Timing timing(long connection, String phase, int id) {
        int count = 0; double mean = 0, m2 = 0; long previous = 0; boolean hasPrevious = false;
        for (Packet packet : packets) {
            if (packet.connection != connection || packet.id != id || !packet.phase.equals(phase)) continue;
            if (hasPrevious && packet.receivedNanos >= previous) {
                double delta = (double) (packet.receivedNanos - previous);
                count++; double difference = delta - mean; mean += difference / count; m2 += difference * (delta - mean);
            }
            previous = packet.receivedNanos; hasPrevious = true;
        }
        return new Timing(count, mean, count == 0 ? 0 : m2 / count);
    }
}
