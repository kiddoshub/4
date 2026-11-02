package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.intel.LightFusion;

public final class LightFusionRegression {
    public static void run() {
        int height = 384, bits = 9, per = 64 / bits;
        long[] surface = new long[(256 + per - 1) / per];
        for (int n = 0; n < 256; n++) surface[n / per] |= 128L << ((n % per) * bits);
        byte[] block = new byte[2048], sky = new byte[2048];
        int at = (4 << 8) | (2 << 4) | 1;
        block[at >> 1] |= (byte) (12 << ((at & 1) * 4));
        var result = LightFusion.analyze(-1, -64, height, surface, block, sky);
        check(result.available() && result.brightUndergroundSamples() == 1 && result.brightestBlockLight() == 12, "Underground low-sky light is recorded");
        sky[at >> 1] |= (byte) (15 << ((at & 1) * 4));
        check(LightFusion.analyze(-1, -64, height, surface, block, sky).brightUndergroundSamples() == 0, "Sky light excludes the observation");
        check(LightFusion.analyze(-1, -64, height, null, block, sky).available() == false, "Missing heightmaps stay unknown");
        check(!LightFusion.analyze(-1, -64, height, surface, block, null).available(), "Missing sky data stays unknown");
        check(LightFusion.analyze(4, -64, height, surface, block, new byte[2048]).brightUndergroundSamples() == 0, "Surface and above do not count");
        System.out.println("Light fusion regressions passed.");
    }
    private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
