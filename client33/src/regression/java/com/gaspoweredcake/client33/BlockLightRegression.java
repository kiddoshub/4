package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.recon.BlockLightSample;

public final class BlockLightRegression {
    public static void run() {
        byte[] data = new byte[2048];
        set(data, 1, 2, 3, 10);
        set(data, 0, 2, 3, 8);
        set(data, 7, 12, 9, 15);
        var result = BlockLightSample.brightest(data, -2, -21, 8);
        check(result != null && result.x() == 1 && result.y() == -30 && result.z() == 3 && result.level() == 10, "Y threshold excludes the brighter sample, including negative sections");
        result = BlockLightSample.brightest(data, -2, -1, 12);
        check(result != null && result.x() == 7 && result.y() == -20 && result.z() == 9 && result.level() == 15, "Brightest sample and nibble ordering are correct");
        check(BlockLightSample.brightest(data, -2, -40, 1) == null, "Entire sections below the threshold are excluded");
        check(BlockLightSample.brightest(new byte[2], 0, 10, 1) == null, "Malformed sections are ignored");
        check(BlockLightSample.brightest(new byte[2048], 0, 10, 1) == null, "Dark sections produce no marker");
        var regions = BlockLightSample.regions(data, -2, -1, 8);
        check(regions.size() == 2 && regions.stream().anyMatch(r -> r.x() == 1 && r.y() == -30 && r.level() == 10)
            && regions.stream().anyMatch(r -> r.x() == 7 && r.y() == -20 && r.level() == 15), "Separate lit regions keep their strongest samples");
        check(BlockLightSample.regions(data, -2, -21, 8).size() == 1, "Regional samples obey the Y cutoff");
        check(BlockLightSample.regions(new byte[2], 0, 20, 1).isEmpty(), "Malformed regional data is ignored");
        System.out.println("Block light packet regressions passed.");
    }
    private static void set(byte[] data, int x, int y, int z, int value) {
        int i = (y << 8) | (z << 4) | x;
        data[i >> 1] |= (byte) (value << ((i & 1) * 4));
    }
    private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
