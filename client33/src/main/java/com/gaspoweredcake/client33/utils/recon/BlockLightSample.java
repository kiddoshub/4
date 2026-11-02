package com.gaspoweredcake.client33.utils.recon;

import java.util.ArrayList;
import java.util.List;

/** Finds the brightest received block-light sample in one section, at or below the chosen Y. */
public final class BlockLightSample {
    public record Result(int x, int y, int z, int level) {}
    private BlockLightSample() {}

    public static Result brightest(byte[] data, int sectionY, int maxY, int minimum) {
        if (data == null || data.length != 2048 || minimum < 1 || minimum > 15) return null;
        int top = Math.min(15, maxY - sectionY * 16);
        if (top < 0) return null;
        int best = minimum - 1, bx = 0, by = 0, bz = 0;
        for (int y = 0; y <= top; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int index = (y << 8) | (z << 4) | x;
            int level = ((data[index >> 1] & 255) >> ((index & 1) * 4)) & 15;
            if (level > best) { best = level; bx = x; by = y; bz = z; }
        }
        return best < minimum ? null : new Result(bx, sectionY * 16 + by, bz, best);
    }
    /** Finds one maximum per 8-block cube, preserving separate areas within a section. */
    public static List<Result> regions(byte[] data, int sectionY, int maxY, int minimum) {
        if (data == null || data.length != 2048 || minimum < 1 || minimum > 15) return List.of();
        long base = (long) sectionY * 16;
        int top = (int) Math.min(15L, (long) maxY - base);
        if (top < 0) return List.of();
        Result[] best = new Result[8];
        for (int y = 0; y <= top; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int index = (y << 8) | (z << 4) | x;
            int level = ((data[index >> 1] & 255) >> ((index & 1) * 4)) & 15;
            int region = ((y >> 3) << 2) | ((z >> 3) << 1) | (x >> 3);
            if (level >= minimum && (best[region] == null || level > best[region].level()))
                best[region] = new Result(x, (int) (base + y), z, level);
        }
        List<Result> results = new ArrayList<>(8);
        for (Result result : best) if (result != null) results.add(result);
        return results;
    }

}
