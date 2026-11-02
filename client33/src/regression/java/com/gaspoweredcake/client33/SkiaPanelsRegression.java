package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.gui.renderer.SkiaPanels;

/** Native Skia smoke check; run with the platform native jar and Strata API on the classpath. */
public final class SkiaPanelsRegression {
    public static void main(String[] args) {
        for (int[] size : new int[][] {{300, 88}, {600, 176}, {1, 1}, {1024, 512}}) {
            var image = SkiaPanels.rasterize(size[0], size[1]);
            if (image.width() != size[0] || image.height() != size[1]) throw new AssertionError("Raster dimensions changed");
            if (size[0] > 1) {
                if ((image.get(size[0] / 2, size[1] / 2) >>> 24) < 200)
                    throw new AssertionError("Text needs a stable dark surface");
                if ((image.get(0, 0) >>> 24) > 100) throw new AssertionError("Rounded corner must remain transparent");
            }
        }
        var first = SkiaPanels.rasterize(300, 88);
        var second = SkiaPanels.rasterize(300, 88);
        if (!java.util.Arrays.equals(first.array(), second.array())) throw new AssertionError("Cached artwork must be deterministic");
        try { SkiaPanels.rasterize(4096, 4096); throw new AssertionError("Oversized raster accepted"); }
        catch (IllegalArgumentException expected) {}
        System.out.println("Skia panel native regression passed");
    }
}
