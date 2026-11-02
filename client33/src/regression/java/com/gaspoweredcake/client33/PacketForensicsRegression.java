package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.intel.PacketForensics;

public final class PacketForensicsRegression {
    public static void run() {
        var ring = new PacketForensics(4);
        byte[] bytes = {1, 2};
        ring.add(new PacketForensics.Packet(1,"play",2,2,100,bytes));
        bytes[0] = 7;
        ring.add(new PacketForensics.Packet(1,"play",2,2,110,new byte[]{3}));
        ring.add(new PacketForensics.Packet(1,"play",2,2,130,new byte[]{4}));
        ring.add(new PacketForensics.Packet(1,"config",2,2,135,new byte[]{5}));
        var stats = ring.timing(1,"play",2);
        check(stats.intervals() == 2 && stats.meanNanos() == 15 && stats.varianceNanosSquared() == 25, "Rolling intervals and variance");
        check(ring.packets().getFirst().prefix()[0] == 1, "Input bytes are copied");
        byte[] output = ring.packets().getFirst().prefix(); output[0] = 0;
        check(ring.packets().getFirst().prefix()[0] == 1, "Output bytes are copied");
        ring.add(new PacketForensics.Packet(2,"play",2,0,140,new byte[0]));
        check(ring.packets().size() == 4 && ring.timing(1,"play",2).intervals() == 1, "Eviction updates rolling statistics");
        check(ring.timing(2,"play",2).intervals() == 0, "Connections remain separate");
        ring.clear(); check(ring.packets().isEmpty(), "Disconnect clears observations");
        System.out.println("Packet forensics regressions passed.");
    }
    private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
