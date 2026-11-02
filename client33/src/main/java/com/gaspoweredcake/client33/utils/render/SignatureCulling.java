package com.gaspoweredcake.client33.utils.render;

/** Conservative side-plane test for interpolated hologram and constellation geometry. */
public final class SignatureCulling {
    private SignatureCulling() {}

    public static boolean include(StorageView view, double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ, double width, boolean tether) {
        // A tether can cross the screen even when its endpoint is outside it.
        if (tether) return true;
        double padding = Math.max(0, width) + 1;
        return view.intersects(minX - padding, minY - 1, minZ - padding,
            maxX + padding, maxY + 1, maxZ + padding);
    }
}
