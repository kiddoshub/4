package com.gaspoweredcake.client33.utils.intel;

/** Conservative count of bright, low-sky-light samples beneath the received surface heightmap. */
public final class LightFusion {
    public record Result(boolean available, int brightUndergroundSamples, int brightestBlockLight) {}
    private LightFusion() {}

    public static Result analyze(int sectionY, int minY, int height, long[] surface, byte[] block, byte[] sky) {
        if (height < 1 || surface == null || block == null || sky == null || block.length != 2048 || sky.length != 2048)
            return new Result(false, 0, 0);
        int bits = 32 - Integer.numberOfLeadingZeros(height);
        int perWord = 64 / bits;
        if (surface.length != (256 + perWord - 1) / perWord) return new Result(false, 0, 0);
        long mask = (1L << bits) - 1;
        int count = 0, brightest = 0;
        for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int index = (y << 8) | (z << 4) | x;
            int blockLevel = ((block[index >> 1] & 255) >>> ((index & 1) * 4)) & 15;
            if (blockLevel < 8) continue;
            int skyLevel = ((sky[index >> 1] & 255) >>> ((index & 1) * 4)) & 15;
            if (skyLevel > 3) continue;
            int column = z * 16 + x;
            long relativeSurface = (surface[column / perWord] >>> ((column % perWord) * bits)) & mask;
            if ((long) sectionY * 16 + y >= (long) minY + relativeSurface - 4) continue;
            count++; brightest = Math.max(brightest, blockLevel);
        }
        return new Result(true, count, brightest);
    }
}
